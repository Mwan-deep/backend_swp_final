package AI_Study_Hub.service;

import AI_Study_Hub.dto.response.QuizResponseDTO;
import AI_Study_Hub.entity.Question;
import AI_Study_Hub.entity.QuestionOption;
import AI_Study_Hub.entity.Quiz;
import AI_Study_Hub.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import AI_Study_Hub.dto.response.QuizAttemptHistoryDTO;
import AI_Study_Hub.entity.QuizAttempt;
import AI_Study_Hub.repository.QuizAttemptRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizQueryService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    // HÀM HELPER: Trích xuất tên môn học an toàn từ Quiz -> StudyMaterial -> Subject
    private String extractSubjectName(Quiz quiz) {
        try {
            // Nếu Quiz có liên kết trực tiếp với StudyMaterial trong Database
            if (quiz.getStudyMaterial() != null && quiz.getStudyMaterial().getSubject() != null) {
                return quiz.getStudyMaterial().getSubject().getSubjectName();
            }
            // Mở rộng: Nếu Entity Quiz của bạn có lưu thẳng chuỗi subject (phòng hờ)
            // if (quiz.getSubject() != null) return quiz.getSubject();
        } catch (Exception e) {
            System.err.println("Không thể lấy môn học cho Quiz ID: " + quiz.getQuizId());
        }
        return "General"; // Trả về mặc định nếu tài liệu không có môn học
    }

    @Transactional
    public QuizResponseDTO getQuizForTaking(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thi"));

        List<QuizResponseDTO.QuestionDTO> questionDTOs = new ArrayList<>();

        for (Question question : quiz.getQuestions()) {

            List<QuizResponseDTO.OptionDTO> optionDTOs = new ArrayList<>();
            for (QuestionOption option : question.getQuestionOptions()) {
                optionDTOs.add(QuizResponseDTO.OptionDTO.builder()
                        .optionId(option.getOptionId())
                        .optionText(option.getOptionText())
                        .build());
            }

            questionDTOs.add(QuizResponseDTO.QuestionDTO.builder()
                    .questionId(question.getQuestionId())
                    .questionText(question.getQuestionText())
                    .options(optionDTOs)
                    .build());
        }

        return QuizResponseDTO.builder()
                .quizId(quiz.getQuizId())
                .title(quiz.getTitle())

                // GỌI HÀM LẤY MÔN HỌC Ở ĐÂY
                .subject(extractSubjectName(quiz))

                .quantity(quiz.getQuantity())
                .duration(quiz.getDuration())
                .questions(questionDTOs)
                .build();
    }

    public List<QuizResponseDTO> getUserQuizzes(Long accountId) {
        List<Quiz> quizzes = quizRepository.findByAccount_AccountIdOrderByCreatedAtDesc(accountId);
        return quizzes.stream().map(this::mapToSimpleDTO).collect(Collectors.toList());
    }

    public List<QuizResponseDTO> getPublicSystemQuizzes() {
        List<Quiz> quizzes = quizRepository.findByVisibilityOrderByCreatedAtDesc("PUBLIC");
        return quizzes.stream().map(this::mapToSimpleDTO).collect(Collectors.toList());
    }

    public List<QuizAttemptHistoryDTO> getUserQuizHistory(Long accountId) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByAccount_AccountIdOrderByCreatedAtDesc(accountId);

        List<QuizAttemptHistoryDTO> historyList = new ArrayList<>();

        for (QuizAttempt attempt : attempts) {
            historyList.add(QuizAttemptHistoryDTO.builder()
                    .attemptId(attempt.getAttemptId())
                    .quizId(attempt.getQuiz().getQuizId())
                    .quizTitle(attempt.getQuiz().getTitle())
                    .score(attempt.getScore())
                    .totalWrong(attempt.getTotalQuestionFalse())
                    .attemptedAt(attempt.getCompletedAt())
                    .timeTaken("N/A")
                    // THÊM 2 DÒNG NÀY VÀO:
                    .status(attempt.getStatus() != null ? attempt.getStatus() : "COMPLETED")
                    .savedAnswers(attempt.getSavedAnswers())
                    .build());
        }

        return historyList;
    }

    // Helper map Quiz sang dạng DTO rút gọn cho danh sách hiển thị
    private QuizResponseDTO mapToSimpleDTO(Quiz entity) {
        return QuizResponseDTO.builder()
                .quizId(entity.getQuizId())
                .title(entity.getTitle())

                // GỌI HÀM LẤY MÔN HỌC CHO CÁC THẺ CARD TRÊN GIAO DIỆN
                .subject(extractSubjectName(entity))

                .quantity(entity.getQuantity())
                .visibility(entity.getVisibility())
                .createdAt(entity.getCreatedAt())
                .account(entity.getAccount() != null ?
                        QuizResponseDTO.AccountInfo.builder()
                                .userName(entity.getAccount().getUserName())
                                .build() : null)
                .build();
    }

    public AI_Study_Hub.dto.response.QuizAnalyticsDTO getQuizAnalytics(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new RuntimeException("Không tìm thấy bài thi"));

        // Lấy tất cả các bài đã NỘP (COMPLETED), sắp xếp theo điểm giảm dần để xếp Hạng (Rank)
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuiz_QuizId(quizId).stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .collect(Collectors.toList());

        int totalAttempts = attempts.size();
        double sumScore = 0;
        int sumWrong = 0;
        int totalQuestions = quiz.getQuantity();

        List<AI_Study_Hub.dto.response.QuizAnalyticsDTO.AttemptRecord> records = new ArrayList<>();
        int[] distCounts = new int[5]; // Mảng đếm phổ điểm: [0]=Giỏi, [1]=Khá, [2]=TB, [3]=Qua, [4]=Rớt

        for(int i = 0; i < attempts.size(); i++) {
            QuizAttempt a = attempts.get(i);
            sumScore += a.getScore();
            sumWrong += a.getTotalQuestionFalse();
            int correct = totalQuestions - a.getTotalQuestionFalse();

            records.add(AI_Study_Hub.dto.response.QuizAnalyticsDTO.AttemptRecord.builder()
                    .rank(i + 1)
                    .name(a.getAccount().getUserName())
                    .accountId(a.getAccount().getAccountId())
                    .score(a.getScore())
                    .correct(correct)
                    .wrong(a.getTotalQuestionFalse())
                    .time(quiz.getDuration() + "m") // Mặc định thời gian làm bài
                    .build());

            // Phân loại phổ điểm
            if(a.getScore() >= 9) distCounts[0]++;
            else if(a.getScore() >= 7) distCounts[1]++;
            else if(a.getScore() >= 5) distCounts[2]++;
            else if(a.getScore() >= 4) distCounts[3]++;
            else distCounts[4]++;
        }

        double avgScore = totalAttempts > 0 ? Math.round((sumScore / totalAttempts) * 10.0) / 10.0 : 0.0;
        double avgCorrect = totalAttempts > 0 ? Math.round(((double)(totalQuestions * totalAttempts - sumWrong) / totalAttempts) * 10.0) / 10.0 : 0.0;

        List<AI_Study_Hub.dto.response.QuizAnalyticsDTO.ScoreDistribution> dist = new ArrayList<>();
        String[] grades = {"Excellent (9-10)", "Good (7-8.9)", "Average (5-6.9)", "Pass (4-4.9)", "Fail (<4)"};
        String[] colors = {"#22c55e", "#3b82f6", "#eab308", "#f97316", "#ef4444"};
        for(int i=0; i<5; i++) {
            double pct = totalAttempts > 0 ? Math.round(((double)distCounts[i] / totalAttempts * 100) * 10.0) / 10.0 : 0.0;
            dist.add(AI_Study_Hub.dto.response.QuizAnalyticsDTO.ScoreDistribution.builder()
                    .grade(grades[i]).count(distCounts[i]).percent(pct).color(colors[i]).build());
        }

        // Mock dữ liệu Câu hỏi khó (Do Database chưa lưu lịch sử đáp án từng câu)
        List<AI_Study_Hub.dto.response.QuizAnalyticsDTO.HardestQuestion> hardest = new ArrayList<>();
        hardest.add(AI_Study_Hub.dto.response.QuizAnalyticsDTO.HardestQuestion.builder().id("Q03").title("Xác định mốc lịch sử...").wrongRate("68%").build());
        hardest.add(AI_Study_Hub.dto.response.QuizAnalyticsDTO.HardestQuestion.builder().id("Q12").title("Định nghĩa OOP...").wrongRate("55%").build());

        return AI_Study_Hub.dto.response.QuizAnalyticsDTO.builder()
                .quizId(quizId)
                .title(quiz.getTitle())
                .createdAt(quiz.getCreatedAt())
                .totalAttempts(totalAttempts)
                .averageScore(avgScore)
                .averageCorrect(avgCorrect)
                .totalQuestions(totalQuestions)
                .resultsTable(records)
                .scoreDistribution(dist)
                .hardestQuestions(hardest)
                .build();
    }
}