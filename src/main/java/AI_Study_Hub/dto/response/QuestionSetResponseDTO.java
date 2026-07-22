package AI_Study_Hub.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class QuestionSetResponseDTO {
    private Long id;
    private String title;
    private String subject;
    private int totalQuestions;
    private int downloads;
    private String status;

    // THÊM ĐOẠN NÀY: Tận dụng file có sẵn để tạo DTO cho Câu hỏi
    @Getter
    @Setter
    @Builder
    public static class QuestionDetailDTO {
        private Long questionId;
        private String questionText;
        private String documentTitle;
        private Long documentId;
        private String difficulty;
    }
}