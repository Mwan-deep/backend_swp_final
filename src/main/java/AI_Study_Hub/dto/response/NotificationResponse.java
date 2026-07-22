package AI_Study_Hub.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private Boolean isRead;
    private String type; // Để Front-end render icon tương ứng
    private LocalDateTime createdAt;
}