package AI_Study_Hub.repository;

import AI_Study_Hub.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserName(String userName);
    Optional<Account> findByEmail(String email);
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);

    // ===================================================================
    // HỆ THỐNG TÍNH ĐIỂM (GAMIFICATION) - NATIVE SQL CHO SQL SERVER
    // ===================================================================
    @Query(value = """
        SELECT 
            a.account_id AS accountId, 
            a.user_name AS userName, 
            a.full_name AS fullName, 
            a.avatar_url AS avatarUrl,
            COALESCE((SELECT SUM(view_count) FROM study_materials WHERE account_id = a.account_id), 0) AS totalViews,
            COALESCE((SELECT SUM(download_count) FROM study_materials WHERE account_id = a.account_id), 0) AS totalDownloads,
            COALESCE((SELECT COUNT(qa.attempt_id) 
                      FROM quiz_attempts qa 
                      JOIN quizzes q ON qa.quiz_id = q.quiz_id 
                      WHERE q.account_id = a.account_id 
                        AND qa.account_id != a.account_id), 0) AS totalQuizAttempts
        FROM account a
        """, nativeQuery = true)
    List<Object[]> getRawLeaderboardData();

    @Query("SELECT new AI_Study_Hub.dto.response.CommunityStatDTO(a.accountId, a.userName, a.fullName, a.avatarUrl, " +
            "COALESCE(SUM(m.viewCount), 0L), COALESCE(SUM(m.downloadCount), 0L), " +
            // ĐÃ ĐỔI qa.accountId THÀNH qa.account.accountId ĐỂ KHỚP VỚI ENTITY QUY CHUẨN
            "COALESCE((SELECT COUNT(qa.attemptId) FROM QuizAttempt qa WHERE qa.account.accountId = a.accountId), 0L)) " +
            "FROM Account a LEFT JOIN StudyMaterial m ON a.accountId = m.account.accountId " +
            "GROUP BY a.accountId, a.userName, a.fullName, a.avatarUrl")
    List<AI_Study_Hub.dto.response.CommunityStatDTO> getCommunityStats();
}