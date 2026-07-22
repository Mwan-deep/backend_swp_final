package AI_Study_Hub.repository;

import AI_Study_Hub.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Tìm tất cả thông báo của 1 user, sắp xếp mới nhất lên đầu
    List<Notification> findByAccount_AccountIdOrderByCreatedAtDesc(Long accountId);
}