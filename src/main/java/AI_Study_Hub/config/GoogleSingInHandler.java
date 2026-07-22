package AI_Study_Hub.config;

import AI_Study_Hub.entity.Account;
import AI_Study_Hub.entity.Role;
import AI_Study_Hub.exception.AppException;
import AI_Study_Hub.exception.ErrorCode;
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.repository.RoleRespository;
import AI_Study_Hub.service.AuthenticateService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE,  makeFinal = true)
public class GoogleSingInHandler implements AuthenticationSuccessHandler {
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    AccountRepository accountRespository;
    RoleRespository roleRespository;

    @Autowired
    AuthenticateService authenticateService;

    public GoogleSingInHandler(AccountRepository accountRespository, RoleRespository roleRespository, AuthenticateService authenticateService) {
        this.accountRespository = accountRespository;
        this.roleRespository = roleRespository;
        this.authenticateService = authenticateService;
    }

    // ... (Các phần import và Constructor ở trên giữ nguyên) ...

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        if(oAuth2User == null) return;

        String email = oAuth2User.getAttribute("email");
        if(email == null) return;

        String name = oAuth2User.getAttribute("name");

        // 1. KIỂM TRA XEM TÀI KHOẢN ĐÃ TỒN TẠI CHƯA
        Optional<Account> existingAccount = accountRespository.findByEmail(email);
        Account account;

        if (existingAccount.isPresent()) {
            // Đã tồn tại -> Lấy tài khoản cũ ra dùng
            account = existingAccount.get();

            // =====================================================================
            // ĐÃ THÊM: CHỐT CHẶN BẢO MẬT DÀNH CHO GOOGLE LOGIN
            // =====================================================================
            if (account.getAccountStatus() != null &&
                    ("INACTIVE".equalsIgnoreCase(account.getAccountStatus()) || "SUSPENDED".equalsIgnoreCase(account.getAccountStatus()))) {

                // ĐÃ SỬA: Đá về trang login Vercel kèm mã lỗi trên URL
                response.sendRedirect("https://frontend-swp-final.vercel.app/login?error=account_locked");
                return; // Ngắt luồng thực thi ngay lập tức!
            }

        } else {
            // Chưa tồn tại -> Tạo mới hoàn toàn
            HashSet<Role> roles = new HashSet<>();
            Role userRole = roleRespository.findById("USER").orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

            roles.add(userRole);
            account = Account.builder()
                    .userName(email)
                    .fullName(name)
                    .gender(null)
                    .dob(null)
                    .email(email)
                    .avatarUrl(null)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(null)
                    .accountStatus("ACTIVE")
                    .roles(roles)
                    .bio(null)
                    .build();

            account = accountRespository.save(account);
        }

        // 2. TẠO TOKEN TỪ ACCOUNT (Chỉ chạy đến đây nếu tài khoản ACTIVE)
        String token = authenticateService.generateToken(account);

        // 3. ĐÃ SỬA: CHUYỂN HƯỚNG VỀ FRONTEND VERCEL VÀ GẮN TOKEN LÊN URL
        response.sendRedirect("https://frontend-swp-final.vercel.app/login?token=" + token);
    }

    // ... (Phần code bên dưới giữ nguyên) ...
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        AuthenticationSuccessHandler.super.onAuthenticationSuccess(request, response, chain, authentication);
    }
}