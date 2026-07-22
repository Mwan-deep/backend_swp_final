package AI_Study_Hub.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    public void sendGmail(String toEmail, String subject, String content) {
        // TẠM THỜI BYPASS KHÔNG GỌI API, IN THẲNG OTP RA LOG RENDER ĐỂ LẤY NHẬP WEB
        log.info("======================================");
        log.info("📧 MÃ OTP GỬI TỚI [{}] LÀ:", toEmail);
        log.info("👉 XIN VUI LÒNG LẤY MÃ NÀY NHẬP VÀO WEB: {}", extractOtp(content));
        log.info("======================================");
    }

    private String extractOtp(String content) {
        try {
            // Tách nhanh chuỗi để lấy mã OTP in ra log cho dễ nhìn
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.trim().matches("\\d{6}")) {
                    return line.trim();
                }
            }
        } catch (Exception ignored) {}
        return "Check content";
    }
}