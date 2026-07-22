package AI_Study_Hub.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportDetailResponse {
    Long reportId;
    String description;
    String status;
    String internalNotes;
    LocalDateTime createdAt;

    ReporterInfo reporter;
    ReportedUserInfo reportedUser;
    MaterialInfo material;

    @Data
    @Builder
    public static class ReporterInfo {
        Long accountId;
        String userName;
        String fullName;
        String email;
        String avatarUrl;
    }

    @Data
    @Builder
    public static class ReportedUserInfo {
        Long accountId;
        String userName;
        String fullName;
        String email;
        String avatarUrl;
        String accountStatus;
    }

    @Data
    @Builder
    public static class MaterialInfo {
        Long materialId;
        String title;
    }
}