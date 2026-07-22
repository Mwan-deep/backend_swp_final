package AI_Study_Hub.dto.response;
import lombok.*;
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class CommunityStatDTO {
    Long accountId; String userName; String fullName; String avatarUrl;
    Long totalViews; Long totalDownloads; Long totalQuizzes;
}