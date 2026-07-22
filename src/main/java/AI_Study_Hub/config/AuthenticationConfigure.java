package AI_Study_Hub.config;

import AI_Study_Hub.exception.AppException;
import AI_Study_Hub.exception.ErrorCode;
import AI_Study_Hub.entity.Account;
import AI_Study_Hub.entity.Role;
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.repository.RoleRespository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;

@Configuration
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationConfigure {
    RoleRespository roleRespository;
    AccountRepository accountRespository;


    @Bean
    ApplicationRunner applicationRunner (){
        return args -> {
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

            // Tìm xem tài khoản Admin đã có trong DB chưa
            var adminOpt = accountRespository.findByUserName("Admin");

            if(adminOpt.isEmpty()){
                // NẾU CHƯA CÓ -> TẠO MỚI
                HashSet<Role> roles = new HashSet<>();
                Role adminRole = roleRespository.findRoleByRoleId("ADMIN");
                if(adminRole == null){
                    throw new AppException(ErrorCode.NOT_FOUND);
                }
                roles.add(adminRole);

                Account account = Account.builder()
                        .userName("Admin")
                        .passwordHash(passwordEncoder.encode("12345")) // Mật khẩu mới
                        .fullName("Tran Ngoc Duc")
                        .dob(LocalDate.of(2004, 12, 10))
                        .gender("MALE")
                        .email("thanhbinh.11005@gmail.com")
                        .avatarUrl(null)
                        .bio("AI STUDY HUB DEVELOPERS")
                        .createdAt(LocalDateTime.now())
                        .accountStatus("ACTIVE")
                        .roles(roles)
                        .build();

                accountRespository.save(account);
                log.warn("Tài khoản Admin đã được tạo mới với mật khẩu: 12345");

            } else {
                // NẾU ĐÃ CÓ RỒI -> ÉP CẬP NHẬT LẠI MẬT KHẨU
                Account existingAdmin = adminOpt.get();
                existingAdmin.setPasswordHash(passwordEncoder.encode("12345"));
                accountRespository.save(existingAdmin);
                log.warn("Tài khoản Admin đã tồn tại. Đã ÉP cập nhật lại mật khẩu thành: 12345");
            }
        };
    }

}
