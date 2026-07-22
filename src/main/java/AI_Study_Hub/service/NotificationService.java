package AI_Study_Hub.service;

import AI_Study_Hub.dto.response.NotificationResponse;
import AI_Study_Hub.entity.Account;
import AI_Study_Hub.entity.Notification;
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;

    private Account getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));
    }

    public List<NotificationResponse> getMyNotifications() {
        Account account = getCurrentUser();
        return notificationRepository.findByAccount_AccountIdOrderByCreatedAtDesc(account.getAccountId())
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notif = notificationRepository.findById(notificationId).orElseThrow();
        if (notif.getAccount().getAccountId().equals(getCurrentUser().getAccountId())) {
            notif.setIsRead(true);
            notificationRepository.save(notif);
        }
    }

    @Transactional
    public void markAllAsRead() {
        Account account = getCurrentUser();
        List<Notification> notifs = notificationRepository.findByAccount_AccountIdOrderByCreatedAtDesc(account.getAccountId());
        for (Notification n : notifs) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(notifs);
    }

    // ĐÃ THÊM: Xử lý xóa thông báo
    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        // Kiểm tra bảo mật: Chỉ cho phép xóa nếu thông báo thuộc về người dùng hiện tại
        if (notif.getAccount().getAccountId().equals(getCurrentUser().getAccountId())) {
            notificationRepository.delete(notif);
        } else {
            throw new RuntimeException("Bạn không có quyền xóa thông báo này");
        }
    }

    private NotificationResponse mapToDto(Notification entity) {
        return NotificationResponse.builder()
                .id(entity.getNotificationId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .isRead(entity.getIsRead() != null ? entity.getIsRead() : false)
                .createdAt(entity.getCreatedAt())
                // Lấy type từ DB, nếu null thì mặc định là system
                .type(entity.getNotificationType() != null ? entity.getNotificationType() : "system")
                .build();
    }
}