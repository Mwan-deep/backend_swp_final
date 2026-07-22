package AI_Study_Hub.service;

import AI_Study_Hub.dto.request.QuizSubmitRequest;
import AI_Study_Hub.entity.Account;
import AI_Study_Hub.entity.ActivityLog;
import AI_Study_Hub.entity.QuestionOption;
import AI_Study_Hub.entity.Quiz;
import AI_Study_Hub.entity.QuizAttempt;
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.repository.ActivityLogRepository;
import AI_Study_Hub.repository.QuestionOptionRepository;
import AI_Study_Hub.repository.QuizAttemptRepository;
import AI_Study_Hub.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuizGradingService {

    private final QuizRepository quizRepository;
    private final AccountRepository accountRepository;
    private final QuestionOptionRepository optionRepository;
    private final QuizAttemptRepository attemptRepository;

    // ĐÃ THÊM: Repository để lưu log hoạt động
    private final ActivityLogRepository activityLogRepository;

    @Transactional
    public QuizAttempt gradeQuiz(QuizSubmitRequest request) {

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();

        Account account = accountRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + username));

        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thi"));

        int totalQuestions = quiz.getQuestions().size();
        if (totalQuestions == 0) {
            throw new RuntimeException("Bài thi này chưa có câu hỏi nào!");
        }

        int correctCount = 0;

        for (Map.Entry<Long, Long> entry : request.getAnswers().entrySet()) {
            Long selectedOptionId = entry.getValue();
            if (selectedOptionId == null) {
                continue;
            }

            QuestionOption selectedOption = optionRepository.findById(selectedOptionId).orElse(null);

            if (selectedOption != null && selectedOption.getIsCorrect()) {
                correctCount++;
            }
        }

        int wrongCount = totalQuestions - correctCount;
        double score = (double) correctCount / totalQuestions * 10;
        score = Math.round(score * 100.0) / 100.0;

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .account(account)
                .score(score)
                .totalQuestionFalse(wrongCount)
                .completedAt(LocalDateTime.now())
                .status("COMPLETED")
                .build();

        QuizAttempt savedAttempt = attemptRepository.save(attempt);

        // ĐÃ THÊM: Ghi nhận hoạt động vào Recent Activity
        ActivityLog log = new ActivityLog();
        log.setAccount(account);
        log.setActionType("SUBMIT_QUIZ");
        log.setDescription("Đã hoàn thành bài thi: " + quiz.getTitle() + " (Điểm: " + score + ")");
        log.setCreatedAt(LocalDateTime.now());
        activityLogRepository.save(log);

        return savedAttempt;
    }
}