package AI_Study_Hub.controller;

import AI_Study_Hub.dto.response.NotificationResponse;
import AI_Study_Hub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications() {
        return ResponseEntity.ok(notificationService.getMyNotifications());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<String> markAsRead(@PathVariable("id") Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok("Đã đánh dấu đọc");
    }

    @PutMapping("/read-all")
    public ResponseEntity<String> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok("Đã đánh dấu đọc tất cả");
    }

    // ĐÃ THÊM: API Xóa thông báo
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNotification(@PathVariable("id") Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok("Đã xóa thông báo");
    }
}