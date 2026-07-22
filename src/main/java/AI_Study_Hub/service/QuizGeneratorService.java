package AI_Study_Hub.service;

import AI_Study_Hub.dto.request.QuizCreateRequest;
import AI_Study_Hub.dto.response.QuizResponseDTO;
import AI_Study_Hub.entity.*;
import AI_Study_Hub.repository.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizGeneratorService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final MaterialContextRepository contextRepository;
    private final AccountRepository accountRepository;
    private final StudyMaterialRepository materialRepository;
    private final GeminiService geminiService;
    private final ActivityLogRepository activityLogRepository;


    // HÀM 1: AI SINH CÂU HỎI VÀ TRẢ VỀ DTO (ĐÃ FIX LỖI 501)
    @Transactional
    public List<QuizResponseDTO.QuestionDTO> generateQuestionsOnly(Long materialId, int quantity) {

        MaterialContext context = contextRepository.findByStudyMaterial_MaterialId(materialId)
                .orElseThrow(() -> new RuntimeException("Tài liệu chưa được phân tích nội dung văn bản để sinh trắc nghiệm"));

        String prompt = "Dựa trên nội dung tài liệu học tập được cung cấp dưới đây, hãy tạo ra đúng " + quantity + " câu hỏi trắc nghiệm ôn tập. " +
                "Mỗi câu hỏi phải có từ 4 phương án lựa chọn, và chỉ có duy nhất một phương án đúng.\n\n" +
                "YÊU CẦU BẮT BUỘC: Bạn chỉ được phép trả về chuỗi định dạng JSON thuần túy theo cấu trúc mẫu sau, không được chứa bất kỳ từ giải thích nào ngoài khối JSON:\n" +
                "[\n" +
                "  {\n" +
                "    \"questionText\": \"Nội dung câu hỏi trắc nghiệm?\",\n" +
                "    \"options\": [\n" +
                "      {\"text\": \"Phương án A\", \"isCorrect\": true},\n" +
                "      {\"text\": \"Phương án B\", \"isCorrect\": false},\n" +
                "      {\"text\": \"Phương án C\", \"isCorrect\": false},\n" +
                "      {\"text\": \"Phương án D\", \"isCorrect\": false}\n" +
                "    ]\n" +
                "  }\n" +
                "]\n\n" +
                "--- NỘI DUNG TÀI LIỆU ---\n" + context.getExtractedText();

        String aiResponse = geminiService.chatWithGemini(prompt).trim();

        // 1. KIỂM TRA API CÓ BỊ QUÁ TẢI/LỖI KHÔNG (Kiểm tra bao quát hơn)
        if (aiResponse == null
                || aiResponse.contains("503")
                || aiResponse.contains("Service Unavailable")
                || aiResponse.contains("\"code\": 503")) {
            throw new RuntimeException("AI của Google hiện đang quá tải. Vui lòng đợi vài giây và thử lại!");
        }

        // 2. LÀM SẠCH PHẢN HỒI (Cắt bỏ markdown nếu có)
        if (aiResponse.startsWith("```json")) {
            aiResponse = aiResponse.substring(7);
        } else if (aiResponse.startsWith("```")) {
            aiResponse = aiResponse.substring(3);
        }

        if (aiResponse.endsWith("```")) {
            aiResponse = aiResponse.substring(0, aiResponse.length() - 3);
        }
        aiResponse = aiResponse.trim();

        // 3. CHẶN ĐỨNG NGAY NẾU KHÔNG PHẢI LÀ MẢNG JSON (Bảo vệ Gson không bị crash)
        if (!aiResponse.startsWith("[")) {
            System.out.println("LỖI DỮ LIỆU TỪ AI: " + aiResponse); // Log ra console để dễ debug
            throw new RuntimeException("AI không trả về đúng định dạng mong muốn. Vui lòng tạo lại!");
        }
        try {
            JsonArray questionsArray = JsonParser.parseString(aiResponse).getAsJsonArray();
            List<QuizResponseDTO.QuestionDTO> dtoList = new ArrayList<>();

            for (JsonElement qElem : questionsArray) {
                JsonObject qObj = qElem.getAsJsonObject();
                String qText = qObj.get("questionText").getAsString();

                Question question = Question.builder()
                        .materialContext(context)
                        .questionText(qText)
                        .build();
                question = questionRepository.save(question);

                JsonArray optionsArray = qObj.getAsJsonArray("options");
                String correctText = "";
                List<QuizResponseDTO.OptionDTO> optionDTOs = new ArrayList<>();

                for (JsonElement oElem : optionsArray) {
                    JsonObject oObj = oElem.getAsJsonObject();
                    String oText = oObj.get("text").getAsString();
                    boolean isCorrect = oObj.get("isCorrect").getAsBoolean();

                    QuestionOption option = QuestionOption.builder()
                            .question(question)
                            .optionText(oText)
                            .isCorrect(isCorrect)
                            .build();
                    option = optionRepository.save(option);

                    // Map Option sang DTO
                    optionDTOs.add(QuizResponseDTO.OptionDTO.builder()
                            .optionId(option.getOptionId()) // Đảm bảo Entity QuestionOption dùng getId()
                            .optionText(option.getOptionText())
                            .build());

                    if (isCorrect) {
                        correctText = oText;
                    }
                }

                question.setCorrectAnswer(correctText);
                questionRepository.save(question);

                // Map Question sang DTO
                dtoList.add(QuizResponseDTO.QuestionDTO.builder()
                        .questionId(question.getQuestionId())
                        .questionText(question.getQuestionText())
                        .correctAnswer(question.getCorrectAnswer())
                        .options(optionDTOs)
                        .build());
            }

            return dtoList;

        } catch (Exception e) {
            throw new RuntimeException("Lỗi phân tích cú pháp dữ liệu câu hỏi từ AI: " + e.getMessage());
        }
    }

    @Transactional
    public QuizResponseDTO createCustomQuiz(Long accountId, QuizCreateRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        List<Question> selectedQuestions = questionRepository.findAllById(request.getQuestionIds());

        if (selectedQuestions.isEmpty()) {
            throw new RuntimeException("Bạn phải chọn ít nhất 1 câu hỏi để tạo bài thi!");
        }

        int hours = request.getDurationMinutes() / 60;
        int minutes = request.getDurationMinutes() % 60;
        LocalTime duration = LocalTime.of(hours, minutes);

        String visibilityStatus = (request.getVisibility() != null && request.getVisibility().equalsIgnoreCase("PUBLIC"))
                ? "PUBLIC" : "PRIVATE";

        Quiz quiz = Quiz.builder()
                .account(account)
                .title(request.getTitle())
                .quantity(selectedQuestions.size())
                .duration(duration)
                .passScore(request.getPassScore())
                .visibility(visibilityStatus)
                .questions(selectedQuestions)
                .build();

        Quiz savedQuiz = quizRepository.save(quiz);

        // ĐÃ THÊM: Ghi nhận hoạt động tạo Quiz
        ActivityLog log = new ActivityLog();
        log.setAccount(account);
        log.setActionType("CREATE_QUIZ");
        log.setDescription("Đã tạo đề trắc nghiệm bằng AI: " + request.getTitle());
        log.setCreatedAt(java.time.LocalDateTime.now());
        activityLogRepository.save(log);

        return mapToDTO(savedQuiz);
    }

    // HÀM 3: CẬP NHẬT TRẠNG THÁI PUBLIC/PRIVATE SAU KHI ĐÃ TẠO
    @Transactional
    public QuizResponseDTO updateQuizVisibility(Long quizId, Long accountId, String newVisibility) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thi với ID: " + quizId));

        if (!quiz.getAccount().getAccountId().equals(accountId)) {
            throw new RuntimeException("Truy cập bị từ chối! Bạn không có quyền chỉnh sửa bài thi của người khác.");
        }

        String visibilityStatus = (newVisibility != null && newVisibility.equalsIgnoreCase("PUBLIC"))
                ? "PUBLIC" : "PRIVATE";

        quiz.setVisibility(visibilityStatus);
        Quiz savedQuiz = quizRepository.save(quiz);

        return mapToDTO(savedQuiz);
    }

    // Hàm Helper chuyển đổi Quiz sang DTO
    private QuizResponseDTO mapToDTO(Quiz entity) {
        return QuizResponseDTO.builder()
                .quizId(entity.getQuizId())
                .title(entity.getTitle())
                .quantity(entity.getQuantity())
                .visibility(entity.getVisibility())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Transactional
    public List<QuizResponseDTO.QuestionDTO> generateQuestionsFromPrompt(String userPrompt, int quantity) {
        String prompt = "Dựa trên yêu cầu sau: '" + userPrompt + "', hãy tạo ra đúng " + quantity + " câu hỏi trắc nghiệm. " +
                "Mỗi câu hỏi phải có từ 4 phương án lựa chọn, và chỉ có duy nhất một phương án đúng.\n\n" +
                "YÊU CẦU BẮT BUỘC: Bạn chỉ được phép trả về chuỗi định dạng JSON thuần túy theo cấu trúc mẫu sau, không được chứa bất kỳ từ giải thích nào ngoài khối JSON:\n" +
                "[\n  {\n    \"questionText\": \"Nội dung câu hỏi?\",\n    \"options\": [\n      {\"text\": \"Đáp án A\", \"isCorrect\": true},\n      {\"text\": \"Đáp án B\", \"isCorrect\": false}\n    ]\n  }\n]";

        String aiResponse = geminiService.chatWithGemini(prompt).trim();
        if (aiResponse.startsWith("```json")) aiResponse = aiResponse.substring(7);
        if (aiResponse.endsWith("```")) aiResponse = aiResponse.substring(0, aiResponse.length() - 3);

        try {
            JsonArray questionsArray = JsonParser.parseString(aiResponse.trim()).getAsJsonArray();
            List<QuizResponseDTO.QuestionDTO> dtoList = new ArrayList<>();

            for (JsonElement qElem : questionsArray) {
                JsonObject qObj = qElem.getAsJsonObject();

                // Vì không có materialContext, ta lưu câu hỏi trơn
                Question question = Question.builder()
                        .questionText(qObj.get("questionText").getAsString())
                        .build();
                question = questionRepository.save(question);

                List<QuizResponseDTO.OptionDTO> optionDTOs = new ArrayList<>();
                String correctText = "";

                for (JsonElement oElem : qObj.getAsJsonArray("options")) {
                    JsonObject oObj = oElem.getAsJsonObject();
                    boolean isCorrect = oObj.get("isCorrect").getAsBoolean();

                    QuestionOption option = QuestionOption.builder()
                            .question(question)
                            .optionText(oObj.get("text").getAsString())
                            .isCorrect(isCorrect)
                            .build();
                    option = optionRepository.save(option);

                    optionDTOs.add(QuizResponseDTO.OptionDTO.builder()
                            .optionId(option.getOptionId()).optionText(option.getOptionText()).build());
                    if (isCorrect) correctText = option.getOptionText();
                }

                question.setCorrectAnswer(correctText);
                questionRepository.save(question);

                dtoList.add(QuizResponseDTO.QuestionDTO.builder()
                        .questionId(question.getQuestionId())
                        .questionText(question.getQuestionText())
                        .options(optionDTOs).build());
            }
            return dtoList;
        } catch (Exception e) {
            throw new RuntimeException("AI trả về sai định dạng. Vui lòng thử lại!");
        }
    }
}