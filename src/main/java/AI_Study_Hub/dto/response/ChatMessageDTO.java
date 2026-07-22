package AI_Study_Hub.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageDTO {
    private Long id;
    private String sender; // Sẽ trả về "user" hoặc "ai"
    private String text;
    private LocalDateTime createdAt;
}