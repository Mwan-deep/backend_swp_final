package AI_Study_Hub.dto.response;

import AI_Study_Hub.entity.StudyMaterial;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentResponse {
    private Long materialId;
    private String title;
    private String fileName;
    private String fileUrl;
    private Integer viewCount;
    private Integer downloadCount;
    private LocalDateTime createdAt;

    // Đóng gói 2 đối tượng con giống hệt cấu trúc JSON React đang mong đợi
    private AccountDto account;
    private SemesterDto semester;

    @Data @Builder
    public static class AccountDto {
        private String userName;
    }

    @Data @Builder
    public static class SemesterDto {
        private String displayName;
    }

    // Hàm tiện ích tự động map từ Entity sang DTO
    public static DocumentResponse fromEntity(StudyMaterial mat) {
        return DocumentResponse.builder()
                .materialId(mat.getMaterialId())
                .title(mat.getTitle())
                .fileName(mat.getFileName())
                .fileUrl(mat.getFileUrl())
                .viewCount(mat.getViewCount())
                .downloadCount(mat.getDownloadCount())
                .createdAt(mat.getCreatedAt())
                .account(mat.getAccount() != null ?
                        AccountDto.builder().userName(mat.getAccount().getUserName()).build() : null)
                .semester(mat.getSemester() != null ?
                        SemesterDto.builder().displayName(mat.getSemester().getDisplayName()).build() : null)
                .build();
    }
}