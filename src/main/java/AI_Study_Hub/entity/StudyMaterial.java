package AI_Study_Hub.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List; // Thêm import List

@Entity
@Table(name = "study_materials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id")
    private Long materialId;

    // Các khóa ngoại (Foreign Keys)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    // 👇 Chặn Jackson không đào vào roles/devices và bỏ qua biến ảo của LAZY
    @JsonIgnoreProperties({"roles", "devices", "passwordHash", "hibernateLazyInitializer", "handler"})
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    // 👇 Chặn Jackson không đào vào chuyên ngành
    @JsonIgnoreProperties({"specialization", "hibernateLazyInitializer", "handler"})
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    // 👇 Bỏ qua biến ảo của LAZY
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Semester semester;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "VARCHAR(MAX)")
    private String description;

    @Column(name = "file_url", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type", length = 255)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "visibility", length = 20)
    private String visibility = "PRIVATE";

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Thay vì dùng List, chúng ta dùng @OneToOne để khớp với MaterialContext
    @OneToOne(mappedBy = "studyMaterial", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private MaterialContext materialContext;

    // Tự động gán thời gian khi insert/update
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}