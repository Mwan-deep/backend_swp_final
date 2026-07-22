package AI_Study_Hub.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class LeaderboardDTO {
    private Integer rank;
    private Long accountId;
    private String name;
    private String major;
    private String avatar;

    // CÁC CHỈ SỐ TƯƠNG TÁC (ĐÃ THÊM LƯỢT LÀM QUIZ)
    private Long totalViews;
    private Long totalDownloads;
    private Long totalQuizAttempts;

    // ĐIỂM SỐ & DANH HIỆU
    private Long engagementScore;
    private Boolean currentUser;
    private List<String> badges;
}