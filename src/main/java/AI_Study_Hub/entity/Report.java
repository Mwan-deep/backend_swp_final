package AI_Study_Hub.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    Long reportId;

    @Column(name = "material_id")
    Long materialId;

    @Column(name = "account_id")
    Long accountId;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    String description;

    @Column(name = "report_token")
    String reportToken;

    @Column(name = "expired_at")
    LocalDateTime expiredAt;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    // HAI CỘT MỚI CHÚNG TA VỪA QUY ƯỚC THÊM VÀO SQL
    @Column(name = "status")
    String status = "PENDING"; // Trạng thái mặc định khi user vừa báo cáo xong

    @Column(name = "internal_notes", columnDefinition = "NVARCHAR(MAX)")
    String internalNotes;
}