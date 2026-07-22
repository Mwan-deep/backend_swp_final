package AI_Study_Hub.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class StudyMaterialResponseDTO {

    private Long materialId;
    private String title;
    private String description;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileUrl;
    private String visibility;
    private Integer viewCount;
    private Integer downloadCount;
    private LocalDateTime createdAt; // Nếu Entity của bạn dùng java.util.Date thì bạn đổi lại kiểu Date ở đây nhé

    // Các "vỏ hộp" con được thiết kế khớp 100% với cấu trúc Frontend đang gọi
    private AccountInfo account;
    private SubjectInfo subject;
    private SemesterInfo semester;

    @Data
    @Builder
    public static class AccountInfo {
        private String userName;
        private String fullName;
        private String avatarUrl;
    }

    @Data
    @Builder
    public static class SubjectInfo {
        private String subjectName;
        private String majorName;
    }

    @Data
    @Builder
    public static class SemesterInfo {
        private String semesterName;
        private String year;
        private String displayName;
    }
}