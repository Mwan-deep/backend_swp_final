package AI_Study_Hub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@Table(name = "activity_logs")
public class ActivityLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long activityId;

    // Loại hoạt động (VD: "UPLOAD_DOCUMENT", "COMPLETE_QUIZ", "CREATE_FLASHCARD")
    @Column(name = "action_type", nullable = false)
    private String actionType;

    // Mô tả chi tiết (VD: "Đã tải lên tài liệu Giải tích 1")
    @Column(name = "description", nullable = false, length = 500)
    private String description;

    // Dùng để vẽ biểu đồ "Weekly Study Activity" (Trục Y: số giờ/phút học)
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Quan hệ N-1 với bảng Account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
}