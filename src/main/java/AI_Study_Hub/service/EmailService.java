package AI_Study_Hub.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    @Value("${brevo.api.key:#{null}}")
    private String brevoApiKey;

    // Lấy email người gửi đã đăng ký trên Brevo (hoặc dùng chung email của bạn)
    @Value("${spring.mail.username}")
    private String senderEmail;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    public void sendGmail(String toEmail, String subject, String content) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // 1. Chuẩn bị Header
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            // 2. Chuẩn bị Body theo đúng chuẩn JSON của Brevo API
            Map<String, Object> body = new HashMap<>();

            Map<String, String> sender = new HashMap<>();
            sender.put("name", "AI Study Hub");
            sender.put("email", senderEmail); // Email đã xác thực trên Brevo
            body.put("sender", sender);

            Map<String, String> recipient = new HashMap<>();
            recipient.put("email", toEmail);
            body.put("to", List.of(recipient));

            body.put("subject", subject);
            body.put("textContent", content); // Hoặc dùng 'htmlContent' nếu muốn gửi mã HTML

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 3. Gửi Request POST qua cổng HTTPS (443) - Không bao giờ bị chặn trên Render
            ResponseEntity<String> response = restTemplate.exchange(
                    BREVO_API_URL,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Gửi email OTP thành công qua Brevo API tới: {}", toEmail);
            } else {
                log.error("Brevo API trả về lỗi: {}", response.getBody());
                throw new RuntimeException("Không thể gửi email qua API.");
            }

        } catch (Exception e) {
            log.error("LỖI KHI GỬI EMAIL QUA BREVO API: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi gửi email: " + e.getMessage());
        }
    }
}