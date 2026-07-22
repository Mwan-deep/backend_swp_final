package AI_Study_Hub.repository;

import AI_Study_Hub.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // 1. Phục vụ Component <ActivityList />: Lấy 10 hoạt động mới nhất của user
    List<ActivityLog> findTop10ByAccount_AccountIdOrderByCreatedAtDesc(Long accountId);

    // 2. Phục vụ Component Weekly Chart: Lấy các hoạt động trong một khoảng thời gian (7 ngày qua)
    List<ActivityLog> findByAccount_AccountIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long accountId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // 3. Phục vụ Stats Grid: Tính tổng số phút đã học của user
    @Query("SELECT SUM(a.durationMinutes) FROM ActivityLog a WHERE a.account.accountId = :accountId")
    Integer getTotalStudyMinutesByAccountId(@Param("accountId") Long accountId);
}