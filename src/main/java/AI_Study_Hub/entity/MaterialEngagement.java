package AI_Study_Hub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "material_engagements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialEngagement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với tài liệu được xem/tải
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private StudyMaterial studyMaterial;

    // Loại hành động: "VIEW" hoặc "DOWNLOAD"
    @Column(name = "action_type", length = 20, nullable = false)
    private String actionType;

    // Thời điểm người dùng thực hiện hành động
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}