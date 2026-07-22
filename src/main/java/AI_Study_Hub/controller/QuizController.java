package AI_Study_Hub.controller;

import AI_Study_Hub.dto.response.QuizAttemptHistoryDTO;
import AI_Study_Hub.dto.response.QuizResponseDTO;
import AI_Study_Hub.dto.request.QuizSubmitRequest;
import AI_Study_Hub.dto.request.QuizCreateRequest;
import AI_Study_Hub.entity.Account;
import AI_Study_Hub.entity.QuizAttempt;
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.service.QuizGeneratorService;
import AI_Study_Hub.service.QuizGradingService;
import AI_Study_Hub.service.QuizQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

// IMPORT THƯ VIỆN JACKSON
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizGeneratorService quizGeneratorService;
    private final QuizGradingService quizGradingService;
    private final QuizQueryService quizQueryService;
    private final AccountRepository accountRepository;

    // CÁCH SỬA LỖI TẠI ĐÂY: Khởi tạo trực tiếp bằng từ khóa new
    // Bằng cách này, Lombok và Spring Boot sẽ không đòi tiêm Bean nữa!
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AI_Study_Hub.repository.QuizAttemptRepository quizAttemptRepository;
    @Autowired
    private AI_Study_Hub.repository.QuizRepository quizRepository;


    // 1. API Nhờ AI sinh câu hỏi
    @PostMapping("/generate-questions")
    public ResponseEntity<?> generateQuestions(
            @RequestParam("materialId") Long materialId,
            @RequestParam(value = "quantity", defaultValue = "5") int quantity) {
        try {
            List<QuizResponseDTO.QuestionDTO> generatedQuestions = quizGeneratorService.generateQuestionsOnly(materialId, quantity);
            return ResponseEntity.ok(generatedQuestions);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi sinh câu hỏi: " + e.getMessage());
        }
    }

    // 2. API Người dùng chốt cấu hình và tạo đề thi
    @PostMapping("/create")
    public ResponseEntity<?> createQuiz(@RequestBody QuizCreateRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Account currentUser = accountRepository.findByUserName(username)
                    .orElseThrow(() -> new RuntimeException("Lỗi định danh"));

            QuizResponseDTO quiz = quizGeneratorService.createCustomQuiz(currentUser.getAccountId(), request);

            Map<String, Object> response = new HashMap<>();
            response.put("quizId", quiz.getQuizId());
            response.put("title", quiz.getTitle());
            response.put("totalQuestions", quiz.getQuantity());
            response.put("visibility", quiz.getVisibility());
            response.put("message", "Đã tạo đề thi thành công!");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi tạo đề thi: " + e.getMessage());
        }
    }

    // 3. API Nộp bài và Chấm điểm
    @PostMapping("/submit")
    public ResponseEntity<?> submitAndGradeQuiz(@RequestBody QuizSubmitRequest request) {
        try {
            QuizAttempt attempt = quizGradingService.gradeQuiz(request);

            Map<String, Object> response = new HashMap<>();
            response.put("attemptId", attempt.getAttemptId());
            response.put("score", attempt.getScore());
            response.put("totalWrong", attempt.getTotalQuestionFalse());
            response.put("message", "Nộp bài và chấm điểm thành công!");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi chấm điểm: " + e.getMessage());
        }
    }

    // 4. API Frontend gọi để lấy đề thi về làm
    @GetMapping("/{quizId}/take")
    public ResponseEntity<?> getQuizForTaking(@PathVariable("quizId") Long quizId) {
        try {
            QuizResponseDTO quizData = quizQueryService.getQuizForTaking(quizId);
            return ResponseEntity.ok(quizData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi truy xuất bài thi: " + e.getMessage());
        }
    }

    // 5. API Lấy danh sách bài thi của cá nhân
    @GetMapping("/my-quizzes")
    public ResponseEntity<?> getMyQuizzes() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Account currentUser = accountRepository.findByUserName(username)
                    .orElseThrow(() -> new RuntimeException("Lỗi định danh"));

            List<QuizResponseDTO> quizzes = quizQueryService.getUserQuizzes(currentUser.getAccountId());
            return ResponseEntity.ok(quizzes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi lấy danh sách bài thi cá nhân: " + e.getMessage());
        }
    }

    // 6. API Lấy toàn bộ bài thi PUBLIC trên hệ thống
    @GetMapping("/all")
    public ResponseEntity<?> getPublicQuizzes() {
        try {
            List<QuizResponseDTO> quizzes = quizQueryService.getPublicSystemQuizzes();
            return ResponseEntity.ok(quizzes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi lấy danh sách bài thi hệ thống: " + e.getMessage());
        }
    }

    // 7. API Lấy lịch sử làm bài thi
    @GetMapping("/history")
    public ResponseEntity<?> getMyQuizHistory() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Account currentUser = accountRepository.findByUserName(username)
                    .orElseThrow(() -> new RuntimeException("Lỗi định danh"));

            List<QuizAttemptHistoryDTO> history = quizQueryService.getUserQuizHistory(currentUser.getAccountId());
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi lấy lịch sử làm bài: " + e.getMessage());
        }
    }

    // 8. API Cập nhật trạng thái Công khai/Riêng tư của bài thi
    @PatchMapping("/{quizId}/visibility")
    public ResponseEntity<?> updateVisibility(
            @PathVariable("quizId") Long quizId,
            @RequestParam("status") String status) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Account currentUser = accountRepository.findByUserName(username)
                    .orElseThrow(() -> new RuntimeException("Lỗi định danh"));

            QuizResponseDTO updatedQuiz = quizGeneratorService.updateQuizVisibility(quizId, currentUser.getAccountId(), status);

            Map<String, Object> response = new HashMap<>();
            response.put("quizId", updatedQuiz.getQuizId());
            response.put("title", updatedQuiz.getTitle());
            response.put("newVisibility", updatedQuiz.getVisibility());
            response.put("message", "Đã cập nhật trạng thái bài thi thành công!");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi cập nhật trạng thái: " + e.getMessage());
        }
    }

    // 9. API Sinh câu hỏi từ Prompt tự do
    @PostMapping("/generate-from-prompt")
    public ResponseEntity<?> generateFromPrompt(@RequestBody Map<String, Object> requestBody) {
        try {
            String prompt = (String) requestBody.get("prompt");
            int quantity = (int) requestBody.get("quantity");

            List<QuizResponseDTO.QuestionDTO> generated = quizGeneratorService.generateQuestionsFromPrompt(prompt, quantity);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "AI sinh câu hỏi thành công");
            response.put("result", generated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 10. API: Lưu nháp tự động (Auto-save)
    @PostMapping("/auto-save")
    public ResponseEntity<?> autoSaveQuiz(@RequestBody Map<String, Object> payload) {
        try {
            Long quizId = Long.valueOf(payload.get("quizId").toString());
            Object answersObj = payload.get("answers");

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Account currentUser = accountRepository.findByUserName(username)
                    .orElseThrow(() -> new RuntimeException("Lỗi định danh"));

            List<AI_Study_Hub.entity.QuizAttempt> existing = quizAttemptRepository
                    .findByAccount_AccountIdOrderByCreatedAtDesc(currentUser.getAccountId());

            AI_Study_Hub.entity.QuizAttempt draftAttempt = existing.stream()
                    .filter(a -> a.getQuiz().getQuizId().equals(quizId) && "IN_PROGRESS".equals(a.getStatus()))
                    .findFirst()
                    .orElse(null);

            // SỬ DỤNG OBJECT MAPPER ĐÃ ĐƯỢC KHỞI TẠO Ở TRÊN
            String answersJson = objectMapper.writeValueAsString(answersObj);

            if (draftAttempt != null) {
                draftAttempt.setSavedAnswers(answersJson);
                quizAttemptRepository.save(draftAttempt);
            } else {
                AI_Study_Hub.entity.Quiz quiz = quizRepository.findById(quizId).orElseThrow();
                AI_Study_Hub.entity.QuizAttempt newDraft = new AI_Study_Hub.entity.QuizAttempt();
                newDraft.setAccount(currentUser);
                newDraft.setQuiz(quiz);
                newDraft.setStatus("IN_PROGRESS");
                newDraft.setSavedAnswers(answersJson);
                newDraft.setScore(0.0);
                newDraft.setTotalQuestionFalse(0);
                newDraft.setCompletedAt(java.time.LocalDateTime.now());

                quizAttemptRepository.save(newDraft);
            }
            return ResponseEntity.ok("Auto-saved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi Auto-save: " + e.getMessage());
        }
    }

    // Thêm API này vào cùng với các API GET khác
    @GetMapping("/{quizId}/analytics")
    public ResponseEntity<?> getQuizAnalytics(@PathVariable("quizId") Long quizId) {
        try {
            AI_Study_Hub.dto.response.QuizAnalyticsDTO analytics = quizQueryService.getQuizAnalytics(quizId);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi tải thống kê: " + e.getMessage());
        }
    }
}