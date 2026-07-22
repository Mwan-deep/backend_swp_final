package AI_Study_Hub.service;

import AI_Study_Hub.dto.request.AwardRequestDTO;
import AI_Study_Hub.dto.response.CommunityStatDTO;
import AI_Study_Hub.dto.response.HallOfFameDTO;
import AI_Study_Hub.dto.response.LeaderboardDTO;
import AI_Study_Hub.entity.*;
import AI_Study_Hub.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityService {

    // Các Repository cũ
    private final MaterialEngagementRepository engagementRepository;
    private final AccountRepository accountRepository;

    // Các Repository mới cho chức năng Biểu dương
    private final GoalRepository goalRepository;
    private final AccountGoalRepository accountGoalRepository;
    private final NotificationRepository notificationRepository;
    private final EntityManager entityManager;

    // =========================================================================
    // 1. LOGIC BẢNG XẾP HẠNG (TÍNH VIEW + DOWNLOAD + QUIZ)
    // =========================================================================
    public List<LeaderboardDTO> getLeaderboard(String filter) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account currentUser = accountRepository.findByUserName(username).orElse(null);
        Long currentUserId = currentUser != null ? currentUser.getAccountId() : -1L;

        LocalDateTime startDate;
        switch (filter.toLowerCase()) {
            case "weekly": startDate = LocalDateTime.now().minusWeeks(1); break;
            case "monthly": startDate = LocalDateTime.now().minusMonths(1); break;
            default: startDate = LocalDateTime.of(2000, 1, 1, 0, 0); break;
        }

        List<MaterialEngagement> engagements = engagementRepository.findByCreatedAtAfter(startDate);
        Map<Account, Long[]> userStats = new HashMap<>();

        if (currentUser != null) {
            userStats.put(currentUser, new Long[]{0L, 0L});
        }

        for (MaterialEngagement eng : engagements) {
            Account uploader = eng.getStudyMaterial().getAccount();
            if (uploader == null) continue;

            userStats.putIfAbsent(uploader, new Long[]{0L, 0L});
            if ("VIEW".equals(eng.getActionType())) {
                userStats.get(uploader)[0]++;
            } else if ("DOWNLOAD".equals(eng.getActionType())) {
                userStats.get(uploader)[1]++;
            }
        }

        // Kéo dữ liệu Quiz
        String quizQueryStr = "SELECT qa.account.accountId, COUNT(qa) FROM QuizAttempt qa WHERE qa.createdAt >= :startDate GROUP BY qa.account.accountId";
        List<Object[]> quizCounts = entityManager.createQuery(quizQueryStr, Object[].class)
                .setParameter("startDate", startDate)
                .getResultList();

        Map<Long, Long> userQuizMap = new HashMap<>();
        for (Object[] row : quizCounts) {
            userQuizMap.put((Long) row[0], (Long) row[1]);
        }

        // Kéo Danh Vọng (Badges) từ bảng account_goal
        List<AccountGoal> allAwards = accountGoalRepository.findAll();
        Map<Long, List<String>> userBadgesMap = new HashMap<>();
        for (AccountGoal award : allAwards) {
            Long accId = award.getAccount().getAccountId();
            userBadgesMap.putIfAbsent(accId, new ArrayList<>());
            if (!userBadgesMap.get(accId).contains(award.getGoal().getGoalName())) {
                userBadgesMap.get(accId).add(award.getGoal().getGoalName());
            }
        }

        List<LeaderboardDTO> dtoList = new ArrayList<>();
        Set<Account> allAccounts = new HashSet<>(userStats.keySet());
        for (Long accId : userQuizMap.keySet()) {
            accountRepository.findById(accId).ifPresent(allAccounts::add);
        }
        for (Long accId : userBadgesMap.keySet()) {
            accountRepository.findById(accId).ifPresent(allAccounts::add);
        }

        for (Account acc : allAccounts) {
            Long[] vd = userStats.getOrDefault(acc, new Long[]{0L, 0L});
            Long quizzes = userQuizMap.getOrDefault(acc.getAccountId(), 0L);

            // Điểm: View (1) + Download (2) + Quiz (3)
            Long totalEngagementScore = vd[0] + (vd[1] * 2) + (quizzes * 3);

            dtoList.add(LeaderboardDTO.builder()
                    .accountId(acc.getAccountId())
                    .name(acc.getUserName())
                    .major("FPT Student")
                    .avatar("https://ui-avatars.com/api/?name=" + acc.getUserName() + "&background=random")
                    .totalViews(vd[0])
                    .totalDownloads(vd[1])
                    .totalQuizAttempts(quizzes)
                    .engagementScore(totalEngagementScore)
                    .currentUser(acc.getAccountId().equals(currentUserId))
                    .badges(userBadgesMap.getOrDefault(acc.getAccountId(), new ArrayList<>()))
                    .build());
        }

        dtoList.sort((a, b) -> b.getEngagementScore().compareTo(a.getEngagementScore()));
        for (int i = 0; i < dtoList.size(); i++) dtoList.get(i).setRank(i + 1);

        return dtoList;
    }

    // =========================================================================
    // 2. LOGIC QUẢN LÝ CỘNG ĐỒNG (Thống kê & Hall of Fame)
    // =========================================================================
    public List<CommunityStatDTO> getCommunityStats() {
        return accountRepository.getCommunityStats();
    }

    public List<HallOfFameDTO> getHallOfFame() {
        return accountGoalRepository.findAllByOrderByCreatedAtDesc().stream().map(ag ->
                HallOfFameDTO.builder()
                        .id(ag.getId())
                        .month(ag.getAwardedMonth())
                        .category(ag.getCategory())
                        .badgeName(ag.getGoal().getGoalName())
                        .account(HallOfFameDTO.AccountInfo.builder()
                                .accountId(ag.getAccount().getAccountId())
                                .userName(ag.getAccount().getUserName())
                                .fullName(ag.getAccount().getFullName())
                                .avatarUrl(ag.getAccount().getAvatarUrl())
                                .build())
                        .build()
        ).collect(Collectors.toList());
    }

    @Transactional
    public void awardBadgeToUser(AwardRequestDTO request) {
        Account user = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));

        Goal goal = goalRepository.findByGoalName(request.getBadgeName())
                .orElseGet(() -> {
                    Goal newGoal = Goal.builder()
                            .goalName(request.getBadgeName())
                            .description("Huy hiệu được tạo tự động từ hệ thống")
                            .goalAt(LocalDateTime.now())
                            .build();
                    return goalRepository.save(newGoal);
                });

        AccountGoal accountGoal = AccountGoal.builder()
                .account(user)
                .goal(goal)
                .awardedMonth(request.getMonth())
                .category(request.getCategory())
                .createdAt(LocalDateTime.now())
                .build();
        accountGoalRepository.save(accountGoal);

        notificationRepository.save(Notification.builder()
                .account(user)
                .title("🎉 BẠN VỪA ĐƯỢC VINH DANH TRÊN BẢNG VÀNG!")
                .message("Chúc mừng! Bạn đã được Quản trị viên trao tặng huy hiệu '" + goal.getGoalName() +
                        "' cho hạng mục: " + request.getCategory() + " (" + request.getMonth() + ").")
                .notificationType("system")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void revokeAward(Long accountGoalId) {
        accountGoalRepository.deleteById(accountGoalId);
    }
}