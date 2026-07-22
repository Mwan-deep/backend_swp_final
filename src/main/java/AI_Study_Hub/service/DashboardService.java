package AI_Study_Hub.service;

import AI_Study_Hub.dto.response.DashboardSummaryResponse;
import AI_Study_Hub.entity.Account;
import AI_Study_Hub.entity.ActivityLog;
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.repository.ActivityLogRepository;
import AI_Study_Hub.repository.StudyMaterialRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StudyMaterialRepository studyMaterialRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AccountRepository accountRepository;
    private final GeminiService geminiService;

    @PersistenceContext
    private EntityManager entityManager;

    public DashboardSummaryResponse getDashboardSummary(String userName) {
        // Lấy thông tin user từ Token
        Account account = accountRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        Long accountId = account.getAccountId();
        LocalDateTime now = LocalDateTime.now();

        // 1. Thống kê cơ bản
        long totalDocs = studyMaterialRepository.countByAccount_AccountId(accountId);
        Integer totalMins = activityLogRepository.getTotalStudyMinutesByAccountId(accountId);
        if (totalMins == null) totalMins = 0;

        // 2. Tính % Tăng trưởng tháng (Monthly Growth)
        LocalDateTime startOfThisMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);
        LocalDateTime endOfLastMonth = startOfThisMonth.minusSeconds(1);

        long uploadsThisMonth = studyMaterialRepository.countByAccount_AccountIdAndCreatedAtBetween(accountId, startOfThisMonth, now);
        long uploadsLastMonth = studyMaterialRepository.countByAccount_AccountIdAndCreatedAtBetween(accountId, startOfLastMonth, endOfLastMonth);

        int growth = (uploadsLastMonth == 0) ? ((uploadsThisMonth > 0) ? 100 : 0)
                : (int) (((double) (uploadsThisMonth - uploadsLastMonth) / uploadsLastMonth) * 100);

        // 3. Xử lý Biểu đồ tuần
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).withHour(0).withMinute(0);
        List<ActivityLog> weeklyLogs = activityLogRepository.findByAccount_AccountIdAndCreatedAtBetweenOrderByCreatedAtAsc(accountId, startOfWeek, now);

        Map<String, Integer> weeklyActivity = new LinkedHashMap<>();
        Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach(day -> weeklyActivity.put(day, 0));

        for (ActivityLog log : weeklyLogs) {
            String dayName = log.getCreatedAt().getDayOfWeek().name().substring(0, 3);
            String formattedDay = dayName.charAt(0) + dayName.substring(1).toLowerCase();
            if (log.getDurationMinutes() != null) {
                weeklyActivity.put(formattedDay, weeklyActivity.get(formattedDay) + log.getDurationMinutes());
            }
        }

        // 4. Lấy danh sách hoạt động gần nhất (ĐÃ LỌC RÁC HEARTBEAT)
        List<ActivityLog> allRecentActivities = activityLogRepository.findTop10ByAccount_AccountIdOrderByCreatedAtDesc(accountId);

        List<String> activityDescriptions = allRecentActivities.stream()
                .filter(log -> !"SYSTEM_HEARTBEAT".equals(log.getDescription())) // Lọc bỏ log hệ thống
                .map(ActivityLog::getDescription)
                .limit(5) // Chỉ lấy 5 hoạt động có ý nghĩa nhất
                .collect(Collectors.toList());

        // Nếu mảng vẫn trống (user mới)
        if (activityDescriptions.isEmpty()) {
            activityDescriptions.add("Chào mừng bạn gia nhập AI Study Hub!");
            activityDescriptions.add("Hãy tải lên tài liệu để hệ thống ghi nhận hoạt động.");
        }

        // 5. Gợi ý AI (Dùng hàm chatWithGemini thay vì viết thêm hàm mới)
        List<String> aiSuggestions = new ArrayList<>();
        try {
            String prompt = "Bạn là trợ lý AI Study Hub. Học viên vừa mở Dashboard. " +
                    "Hệ thống ghi nhận họ có " + totalDocs + " tài liệu và đã học " + totalMins + " phút. " +
                    "Các hoạt động gần nhất: " + activityDescriptions.toString() + ". " +
                    "Dựa vào thông tin trên, hãy sinh ra ĐÚNG 3 LỜI KHUYÊN học tập cá nhân hóa (mỗi lời khuyên trên 1 dòng, KHÔNG gạch đầu dòng, siêu ngắn gọn dưới 15 chữ, bằng tiếng Việt).";

            String geminiResponse = geminiService.chatWithGemini(prompt);

            // Xử lý chuỗi trả về từ AI thành danh sách
            String[] tipsArray = geminiResponse.split("\n");
            for (String tip : tipsArray) {
                if (!tip.trim().isEmpty()) {
                    aiSuggestions.add(tip.replace("-", "").replace("*", "").trim());
                }
            }
            if (aiSuggestions.size() > 3) aiSuggestions = aiSuggestions.subList(0, 3);

        } catch (Exception e) {
            System.err.println("Lỗi sinh AI Suggestion: " + e.getMessage());
            aiSuggestions.add("Sử dụng Pomodoro 25 phút để tập trung.");
            aiSuggestions.add("AI có thể giúp bạn tạo Quiz ôn tập.");
            aiSuggestions.add("Xem lại tài liệu cũ để nhớ lâu hơn.");
        }

        // 6. Xử lý mảng cột tháng
        List<Long> monthlyUploadsPerWeek = Arrays.asList(
                uploadsThisMonth / 4,
                uploadsThisMonth / 3,
                uploadsThisMonth / 2,
                uploadsThisMonth - (uploadsThisMonth / 4 + uploadsThisMonth / 3 + uploadsThisMonth / 2)
        );

        return DashboardSummaryResponse.builder()
                .totalDocuments(totalDocs)
                .totalStudyMinutes(totalMins)
                .weeklyActivity(weeklyActivity)
                .monthlyGrowthPercentage(growth)
                .monthlyUploadsPerWeek(monthlyUploadsPerWeek)
                .recentActivities(activityDescriptions)
                .aiSuggestions(aiSuggestions)
                .build();
    }

    @Transactional
    public void recordActiveMinute(String userName) {
        Account account = accountRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        Long accountId = account.getAccountId();
        LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();

        String updateQ = "UPDATE activity_logs " +
                "SET duration_minutes = duration_minutes + 1, created_at = GETDATE() " +
                "WHERE account_id = :accountId AND description = 'SYSTEM_HEARTBEAT' AND created_at >= :startOfDay";

        int updatedCount = entityManager.createNativeQuery(updateQ)
                .setParameter("accountId", accountId)
                .setParameter("startOfDay", startOfDay)
                .executeUpdate();

        if (updatedCount == 0) {
            String insertQ = "INSERT INTO activity_logs (account_id, action_type, description, duration_minutes, created_at) " +
                    "VALUES (:accountId, 'STUDY_SESSION', 'SYSTEM_HEARTBEAT', 1, GETDATE())";
            entityManager.createNativeQuery(insertQ)
                    .setParameter("accountId", accountId)
                    .executeUpdate();
        }
    }
}