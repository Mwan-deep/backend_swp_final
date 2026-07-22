package AI_Study_Hub.service;

import AI_Study_Hub.dto.request.AccountCreateRequest;
import AI_Study_Hub.dto.request.AccountUpdateRequest;
import AI_Study_Hub.dto.request.ChangePasswordRequest;
import AI_Study_Hub.dto.response.AccountResponse;
import AI_Study_Hub.dto.response.ChangePasswordResponse;
import AI_Study_Hub.entity.Device;
import AI_Study_Hub.entity.Notification;
import AI_Study_Hub.exception.AppException;
import AI_Study_Hub.exception.ErrorCode;
import AI_Study_Hub.Mapper.AccountMapper;
import AI_Study_Hub.entity.Account;
import AI_Study_Hub.entity.Role;
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.repository.DeviceRepository;
import AI_Study_Hub.repository.NotificationRepository;
import AI_Study_Hub.repository.RoleRespository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AccountService {

    AccountRepository accountRepository;
    RoleRespository roleRespository;
    DeviceRepository deviceRepository;
    AccountMapper accountMapper;
    NotificationRepository notificationRepository;

    @Autowired
    GoogleDriveService googleDriveService;

    // ĐÃ THÊM: Sử dụng EntityManager để can thiệp xóa cứng Database
    @PersistenceContext
    EntityManager entityManager;

    private AccountResponse buildAccountResponse(Account account) {
        AccountResponse response = accountMapper.toAccountResponse(account);
        response.setAvatarUrl(account.getAvatarUrl());
        return response;
    }

    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request){
        try{
            if(accountRepository.existsByEmail(request.getEmail()))
                throw new AppException((ErrorCode.EMAIL_EXITED));

            Account account = accountMapper.toAccount(request);
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
            account.setPasswordHash(passwordEncoder.encode(request.getPasswordHash()));

            HashSet<Role> roles = new HashSet<>();
            Role userRole = roleRespository.findById("USER")
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

            roles.add(userRole);
            account.setRoles(roles);
            account.setCreatedAt(LocalDateTime.now());
            account.setAccountStatus("ACTIVE");
            account.setUserName("user_" + UUID.randomUUID().toString());

            account = accountRepository.save(account);

            if(request.getDeviceId() != null && !request.getDeviceId().isEmpty()){
                Device device = new Device();
                device.setId(UUID.randomUUID().toString());
                device.setDeviceId(request.getDeviceId());
                device.setTrusted(true);
                device.setLastLogin(LocalDateTime.now().toString());
                device.setAccountId(account.getAccountId());
                deviceRepository.save(device);
            }
            return buildAccountResponse(account);
        }catch (DataIntegrityViolationException exception){
            throw new AppException(ErrorCode.USERNAME_EXITED);
        }
    }

    public AccountResponse updateAccount(AccountUpdateRequest request , Long id){
        Account account = accountRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_EXITS));
        String oldAvatar = account.getAvatarUrl();
        accountMapper.toUpdateAccount(request, account);
        if (account.getAvatarUrl() == null || account.getAvatarUrl().isEmpty()) {
            account.setAvatarUrl(oldAvatar);
        }
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);
        return buildAccountResponse(account);
    }

    // =========================================================================================
    // NÂNG CẤP API DELETE: XÓA CỨNG (HARD DELETE) TOÀN BỘ DỮ LIỆU LIÊN QUAN TRƯỚC KHI XÓA USER
    // =========================================================================================
    @Transactional
    public void deleteAccount(Long id){
        Account account = accountRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_EXITS));

        // 1. KHIÊN BẢO VỆ: Chặn xóa Admin
        boolean isAdmin = account.getRoles().stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getRoleId()));
        if(isAdmin) {
            throw new RuntimeException("Lỗi bảo mật: Bạn KHÔNG THỂ xóa tài khoản của Quản trị viên (ADMIN)!");
        }

        Long accountId = account.getAccountId();

        // 2. TIẾN HÀNH "XÓA CUỐN CHIẾU" (XÓA TỪ NGỌN ĐẾN GỐC) ĐỂ KHÔNG BỊ LỖI FOREIGN KEY

        // Xóa thông báo & Thiết bị
        entityManager.createNativeQuery("DELETE FROM notifications WHERE account_id = :accountId").setParameter("accountId", accountId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM device WHERE account_id = :accountId").setParameter("accountId", accountId).executeUpdate();

        // Xóa Log hoạt động
        entityManager.createNativeQuery("DELETE FROM activity_logs WHERE account_id = :accountId").setParameter("accountId", accountId).executeUpdate();

        // Xóa Lịch sử Chat (Messages -> Sessions)
        entityManager.createNativeQuery("DELETE FROM chat_messages WHERE session_id IN (SELECT session_id FROM chat_sessions WHERE account_id = :accountId)").setParameter("accountId", accountId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM chat_sessions WHERE account_id = :accountId").setParameter("accountId", accountId).executeUpdate();

        // [MỚI BỔ SUNG] - Xóa dữ liệu Quiz để tránh lỗi khóa ngoại FK__quizzes__account
        entityManager.createNativeQuery("DELETE FROM quizzes WHERE account_id = :accountId").setParameter("accountId", accountId).executeUpdate();

        // Xóa Quiz & Context của Tài liệu (Questions -> Contexts -> Materials)
        entityManager.createNativeQuery("DELETE FROM questions WHERE context_id IN (SELECT context_id FROM material_contexts WHERE material_id IN (SELECT material_id FROM study_materials WHERE account_id = :accountId))").setParameter("accountId", accountId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM material_contexts WHERE material_id IN (SELECT material_id FROM study_materials WHERE account_id = :accountId)").setParameter("accountId", accountId).executeUpdate();

        // Cuối cùng: Xóa Tài liệu
        entityManager.createNativeQuery("DELETE FROM study_materials WHERE account_id = :accountId").setParameter("accountId", accountId).executeUpdate();

        // Xóa bảng trung gian Role
        entityManager.createNativeQuery("DELETE FROM account_role WHERE account_id = :accountId").setParameter("accountId", accountId).executeUpdate();

        // 3. Kết liễu: Xóa Account
        accountRepository.delete(account);
    }

    public List<AccountResponse> getAllAccount(){
        return accountRepository.findAll().stream().map(this::buildAccountResponse).toList();
    }

    public ChangePasswordResponse changePassword(ChangePasswordRequest request){
        var userName = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByUserName(userName).orElseThrow(() -> new AppException(ErrorCode.USERNAME_NOT_EXITED));
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        if(!passwordEncoder.matches(request.getOldPassword(), account.getPasswordHash())){
            throw new AppException(ErrorCode.PASSWORD_INCORRECTLY);
        }
        if(!request.getNewPassword().equals(request.getConfirmNewPassword())){
            throw new AppException(ErrorCode.NEW_PASSWORD_INCORRECTLY);
        }
        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
        return ChangePasswordResponse.builder().changed(true).build();
    }

    public AccountResponse createAccountByAdmin(AccountCreateRequest request){
        var account = accountMapper.toAccount(request);
        if(accountRepository.existsByEmail(request.getEmail())){
            throw new AppException(ErrorCode.EMAIL_EXITED);
        }
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        account.setPasswordHash(passwordEncoder.encode(request.getPasswordHash()));

        HashSet<Role> roles = new HashSet<>();
        String roleName = (request.getRole() != null) ? request.getRole() : "USER";
        Role role = roleRespository.findById(roleName).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        roles.add(role);

        account.setRoles(roles);
        if (request.getEmail() != null && request.getEmail().contains("@")) {
            account.setUserName(request.getEmail().split("@")[0]);
        } else {
            account.setUserName(request.getUserName());
        }

        return buildAccountResponse(accountRepository.save(account));
    }

    public AccountResponse GetAccountById(Long id){
        var account = accountRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_EXITS));
        return buildAccountResponse(account);
    }

    @Transactional
    public AccountResponse uploadAvatar(Long id, MultipartFile file) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        try {
            String fileId = googleDriveService.uploadFileToDrive(file);
            String directUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
            account.setAvatarUrl(directUrl);
            accountRepository.save(account);
            return buildAccountResponse(account);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi upload ảnh đại diện: " + e.getMessage());
        }
    }

    // =========================================================================
    // HỆ THỐNG API BẢO MẬT DÀNH RIÊNG CHO NGƯỜI DÙNG HIỆN TẠI (MY-PROFILE)
    // =========================================================================

    public AccountResponse getMyInfo() {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByUserName(userName)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_EXITS));
        return buildAccountResponse(account);
    }

    public AccountResponse updateMyProfile(AccountUpdateRequest request) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByUserName(userName)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_EXITS));

        String currentAvatar = account.getAvatarUrl();
        accountMapper.toUpdateAccount(request, account);
        if (account.getAvatarUrl() == null || account.getAvatarUrl().isEmpty()) {
            account.setAvatarUrl(currentAvatar);
        }

        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);
        return buildAccountResponse(account);
    }

    @Transactional
    public AccountResponse uploadMyAvatar(MultipartFile file) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        try {
            String fileId = googleDriveService.uploadFileToDrive(file);
            String directUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
            account.setAvatarUrl(directUrl);
            accountRepository.save(account);
            return buildAccountResponse(account);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi upload ảnh đại diện: " + e.getMessage());
        }
    }

    public List<Device> getMyDevices() {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByUserName(userName)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_EXITS));
        return deviceRepository.findByAccountId(account.getAccountId());
    }

    public void removeMyDevice(String deviceId) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByUserName(userName)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_EXITS));

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị"));

        if (!device.getAccountId().equals(account.getAccountId())) {
            throw new RuntimeException("Không có quyền truy cập thiết bị này");
        }
        deviceRepository.delete(device);
    }

    @Transactional
    public AccountResponse toggleAccountStatus(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_EXITS));

        // KHIÊN BẢO VỆ: Chặn khóa Admin
        boolean isAdmin = account.getRoles().stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getRoleId()));
        if(isAdmin) {
            throw new RuntimeException("Lỗi bảo mật: Bạn KHÔNG THỂ khóa tài khoản của Quản trị viên (ADMIN)!");
        }

        String newStatus = "ACTIVE".equalsIgnoreCase(account.getAccountStatus()) ? "INACTIVE" : "ACTIVE";
        account.setAccountStatus(newStatus);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        String message = newStatus.equals("INACTIVE")
                ? "Tài khoản của bạn đã bị đình chỉ hoạt động bởi Admin."
                : "Tài khoản của bạn đã được mở khóa trở lại.";

        notificationRepository.save(Notification.builder()
                .account(account)
                .title("Cập nhật trạng thái tài khoản")
                .message(message)
                .notificationType("system")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());

        return buildAccountResponse(account);
    }
}