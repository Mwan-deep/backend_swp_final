package AI_Study_Hub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class QuizResponseDTO {

    private Long quizId;
    private String title;

    // Đã thêm trường subject và loại bỏ difficulty
    private String subject;

    private LocalDateTime startedAt;
    private LocalDateTime endAt;
    private LocalTime duration;
    private Double passScore;
    private Integer quantity;
    private LocalDateTime createdAt;
    private String visibility;

    private AccountInfo account;
    private List<QuestionDTO> questions;

    @Data
    @Builder
    public static class AccountInfo {
        private String userName;
    }

    @Data
    @Builder
    public static class QuestionDTO {
        private Long questionId;
        private String questionText;
        private String correctAnswer;
        private List<OptionDTO> options;
    }

    @Data
    @Builder
    public static class OptionDTO {
        private Long optionId;
        private String optionText;
    }
}