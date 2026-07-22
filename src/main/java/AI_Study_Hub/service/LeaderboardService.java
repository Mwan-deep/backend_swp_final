package AI_Study_Hub.service;

import AI_Study_Hub.dto.response.LeaderboardDTO;
import AI_Study_Hub.entity.AccountGoal; // Import thêm
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.repository.AccountGoalRepository; // Import thêm
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final AccountRepository accountRepository;
    private final AccountGoalRepository accountGoalRepository; // Thêm vào

    @Transactional(readOnly = true)
    public List<LeaderboardDTO> getLeaderboard() {
        List<Object[]> rawData = accountRepository.getRawLeaderboardData();
        List<LeaderboardDTO> result = new ArrayList<>();
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Lấy danh hiệu do Manager cấp
        List<AccountGoal> allAwards = accountGoalRepository.findAll();
        Map<Long, List<String>> userBadgesMap = new HashMap<>();
        for (AccountGoal award : allAwards) {
            Long accId = award.getAccount().getAccountId();
            userBadgesMap.putIfAbsent(accId, new ArrayList<>());
            if (!userBadgesMap.get(accId).contains(award.getGoal().getGoalName())) {
                userBadgesMap.get(accId).add(award.getGoal().getGoalName());
            }
        }

        for (Object[] row : rawData) {
            Long accountId = ((Number) row[0]).longValue();
            String userName = (String) row[1];
            String fullName = (String) row[2];
            String avatarUrl = (String) row[3];
            Long totalViews = ((Number) row[4]).longValue();
            Long totalDownloads = ((Number) row[5]).longValue();
            Long totalQuizAttempts = ((Number) row[6]).longValue();

            Long engagementScore = (totalViews * 1) + (totalDownloads * 2) + (totalQuizAttempts * 3);

            // 2. Lấy danh hiệu thật từ Map
            List<String> badges = userBadgesMap.getOrDefault(accountId, new ArrayList<>());

            result.add(LeaderboardDTO.builder()
                    .accountId(accountId)
                    .name((fullName != null && !fullName.trim().isEmpty()) ? fullName : userName)
                    .major("Khối ngành CNTT")
                    .avatar(avatarUrl != null ? avatarUrl : "https://ui-avatars.com/api/?name=" + userName)
                    .totalViews(totalViews)
                    .totalDownloads(totalDownloads)
                    .totalQuizAttempts(totalQuizAttempts)
                    .engagementScore(engagementScore)
                    .currentUser(userName.equals(currentUsername))
                    .badges(badges) // Gửi mảng danh hiệu thật
                    .build());
        }

        result.sort(Comparator.comparing(LeaderboardDTO::getEngagementScore).reversed());
        for (int i = 0; i < result.size(); i++) result.get(i).setRank(i + 1);

        return result;
    }
}