package AI_Study_Hub.service;

import AI_Study_Hub.dto.request.AuthenticateRequest;
import AI_Study_Hub.dto.request.IntroSpecRequest;
import AI_Study_Hub.dto.request.LogoutRequest;
import AI_Study_Hub.dto.request.VerifyOtpRequest;
import AI_Study_Hub.dto.response.AuthenticateResponse;
import AI_Study_Hub.dto.response.IntroSpecResponse;
import AI_Study_Hub.entity.Device;
import AI_Study_Hub.entity.OtpVerification;
import AI_Study_Hub.exception.AppException;
import AI_Study_Hub.exception.ErrorCode;
import AI_Study_Hub.entity.Account;
import AI_Study_Hub.entity.InvalidatedtokenSession;
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.repository.DeviceRepository;
import AI_Study_Hub.repository.InvalidationRespository;
import AI_Study_Hub.repository.OtpVerificationRespository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticateService {

    @Value("${jwt.signerToken}")
    @NonFinal
    protected String SIGNER_TOKEN;

    AccountRepository accountRespository;
    InvalidationRespository invalidationRespository;
    OtpVerificationRespository  otpVerificationRespository;
    DeviceRepository  deviceRepository;

    @Autowired
    EmailService emailService;

    @Autowired
    OtpService otpService;

    // =========================================================================
    // HÀM 1: XỬ LÝ ĐĂNG NHẬP CHÍNH
    // =========================================================================
    @Transactional
    public AuthenticateResponse authenticate (AuthenticateRequest request){
        var account = accountRespository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXITS));

        // KIỂM TRA TRẠNG THÁI TÀI KHOẢN TRƯỚC KHI CHO ĐĂNG NHẬP
        if (account.getAccountStatus() != null &&
                ("INACTIVE".equalsIgnoreCase(account.getAccountStatus()) || "SUSPENDED".equalsIgnoreCase(account.getAccountStatus()))) {
            // Ném lỗi AppException để GlobalExceptionHandler bắt và trả về cho React
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(request.getPasswordHash(), account.getPasswordHash());
        if(!authenticated){
            throw new AppException(ErrorCode.PASSWORD_INCORRECTLY);
        }

        // Kiểm tra thiết bị tin cậy (Verify Device)
        Optional<Device> deviceOptional = deviceRepository.findByDeviceId(request.getDeviceId());
        if(deviceOptional.isPresent() && deviceOptional.get().getTrusted()){
            var token = generateToken(account);
            return AuthenticateResponse.builder()
                    .token(token)
                    .authenticated(authenticated)
                    .build();
        } else {
            // 1. Dọn dẹp mã OTP cũ của email này trước
            otpVerificationRespository.deleteAllByGmail(request.getEmail());

            // 2. Tạo mã mới
            String optVf = otpService.generarteOtp();
            OtpVerification otpVerification = new OtpVerification();
            otpVerification.setGmail(request.getEmail());
            otpVerification.setOtp(optVf);

            // 3. Set thời gian sống 5 phút
            otpVerification.setExpireTime(LocalDateTime.now().plus(5, ChronoUnit.MINUTES));
            otpVerificationRespository.save(otpVerification);

            // 4. Gửi Mail
            try {
                emailService.sendGmail(
                        account.getEmail(),
                        " AI STUDY HUB - OTP Verification",
                        "=====================================\n" +
                                "        AI STUDY HUB SECURITY\n" +
                                "=====================================\n\n" +
                                "Hello,\n\n" +
                                "You are trying to sign in to AI STUDY HUB.\n\n" +
                                "Your One-Time Password (OTP) is:\n\n" +
                                "        " + optVf + "\n\n" +
                                "This code will expire in 5 minutes.\n\n" +
                                "If you did NOT request this login, please ignore this email.\n\n" +
                                "-------------------------------------\n" +
                                "AI STUDY HUB Team\n" +
                                "Secure Learning Platform\n" +
                                "-------------------------------------");
            } catch (Exception e) {
                // Bắt tận tay lỗi Gửi Mail và in ra Log Render
                log.error("LỖI NGHIÊM TRỌNG KHI GỬI EMAIL OTP: {}", e.getMessage(), e);
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
            }


            return AuthenticateResponse.builder()
                    .authenticated(false)
                    .build();
        }
    }

    // =========================================================================
    // HÀM 2: XÁC THỰC OTP KHI ĐĂNG NHẬP TRÊN THIẾT BỊ MỚI
    // =========================================================================
    @Transactional
    public AuthenticateResponse verifyOtp(VerifyOtpRequest request) {

        // --- 1. GIĂNG LƯỚI BẮT LỖI TỪ FRONTEND ---
        log.info("=== KIỂM TRA ĐẦU VÀO TỪ FRONTEND ===");
        log.info("Email nhận được: {}", request.getEmail());
        log.info("OTP nhận được: {}", request.getOtp());

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            log.error("LỖI CHÍ MẠNG: Frontend không gửi email về Backend!");
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // --- 2. TÌM BẢN GHI TRONG DATABASE ---
        // Đã thêm lại (OtpVerification) để ép kiểu cứng
        OtpVerification otpRecord = (OtpVerification) otpVerificationRespository.findByGmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("LỖI: Không tìm thấy OTP trong Database cho email: {}", request.getEmail());
                    return new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
                });

        // --- 3. KIỂM TRA TÍNH HỢP LỆ ---
        if (!otpRecord.getOtp().equals(request.getOtp())) {
            log.error("LỖI: OTP nhập sai. Trong DB là: {}, Frontend gửi là: {}", otpRecord.getOtp(), request.getOtp());
            throw new AppException(ErrorCode.OTP_INCORRECT);
        }

        if (otpRecord.getExpireTime().isBefore(LocalDateTime.now())) {
            log.error("LỖI: OTP đã hết hạn. Giờ hết hạn: {}, Giờ hiện tại: {}", otpRecord.getExpireTime(), LocalDateTime.now());
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        log.info("=> OTP HOÀN TOÀN HỢP LỆ!");

        // 4. OTP Hợp lệ -> Tìm tài khoản
        Account account = accountRespository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXITS));

        // KIỂM TRA TRẠNG THÁI LẠI MỘT LẦN NỮA (Phòng khi đang nhập OTP thì bị Admin khóa)
        if (account.getAccountStatus() != null &&
                ("INACTIVE".equalsIgnoreCase(account.getAccountStatus()) || "SUSPENDED".equalsIgnoreCase(account.getAccountStatus()))) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 5. Lưu thiết bị này thành thiết bị tin cậy (Trusted Device)
        Device device = deviceRepository.findByDeviceId(request.getDeviceId())
                .orElse(new Device());
        device.setDeviceId(request.getDeviceId());
        device.setTrusted(true);
        device.setAccount(account);
        deviceRepository.save(device);

        // 6. Xóa mã OTP để tránh tái sử dụng
        otpVerificationRespository.delete(otpRecord);

        // 7. Cấp phát Token mới cho người dùng
        String token = generateToken(account);
        return AuthenticateResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    // =========================================================================
    // CÁC HÀM XỬ LÝ TOKEN (JWT) VÀ LOGOUT
    // =========================================================================
    public String generateToken(Account account){
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS256);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(account.getUserName())
                .issuer("devteria")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope" , buildScope(account))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_TOKEN.getBytes()));
            return jwsObject.serialize();
        }catch (JOSEException e){
            log.info("CAN NOT GET TOKEN");
            throw  new RuntimeException(e);
        }
    }

    public String buildScope(Account account){
        StringJoiner stringJoiner = new StringJoiner(" ");

        if(!CollectionUtils.isEmpty(account.getRoles())){
            account.getRoles().forEach(role -> stringJoiner.add("ROLE_"+role.getRoleId()));
        }
        return stringJoiner.toString();
    }

    public IntroSpecResponse introSpec (IntroSpecRequest request)
            throws JOSEException, ParseException{
        boolean isValid = true;

        try {
            verifyToken(request.getToken());
        }catch (AppException e) {
            isValid = false;
        }
        return IntroSpecResponse.builder()
                .authenticated(isValid)
                .build();
    }

    public SignedJWT verifyToken(String token)
            throws JOSEException, ParseException {

        JWSVerifier verifier = new MACVerifier(SIGNER_TOKEN.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expireTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        boolean verified = signedJWT.verify(verifier);
        String jit = signedJWT.getJWTClaimsSet().getJWTID();
        boolean invalidatedToken = invalidationRespository.existsById(jit);

        if(!(verified && expireTime.after(new Date())))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if(invalidatedToken){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return signedJWT;
    }

    public void logout(LogoutRequest request)
            throws ParseException, JOSEException {
        try{
            var signedJWT = verifyToken(request.getToken());

            LocalDateTime expireTime =  signedJWT.getJWTClaimsSet()
                    .getExpirationTime()
                    .toInstant().atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            String jit = signedJWT.getJWTClaimsSet().getJWTID();

            InvalidatedtokenSession invalidatedtokenSession = InvalidatedtokenSession.builder()
                    .invalidId(jit)
                    .expireTime(expireTime)
                    .build();

            invalidationRespository.save(invalidatedtokenSession);
        }catch (AppException e){
            log.info("Token already expired");
        }
    }
}