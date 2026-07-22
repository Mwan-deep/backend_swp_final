package AI_Study_Hub.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuizAnalyticsDTO {
    private Long quizId;
    private String title;
    private LocalDateTime createdAt;
    private Integer totalAttempts;
    private Double averageScore;
    private Double averageCorrect;
    private Integer totalQuestions;

    private List<AttemptRecord> resultsTable;
    private List<ScoreDistribution> scoreDistribution;
    private List<HardestQuestion> hardestQuestions;

    @Data
    @Builder
    public static class AttemptRecord {
        private Integer rank;
        private String name;
        private Long accountId;
        private Double score;
        private Integer correct;
        private Integer wrong;
        private String time;
    }

    @Data
    @Builder
    public static class ScoreDistribution {
        private String grade;
        private Integer count;
        private Double percent;
        private String color;
    }

    @Data
    @Builder
    public static class HardestQuestion {
        private String id;
        private String title;
        private String wrongRate;
    }
}