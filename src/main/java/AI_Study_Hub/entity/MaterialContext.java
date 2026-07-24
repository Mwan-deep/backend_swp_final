package AI_Study_Hub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
// Bổ sung 2 thư viện này
import lombok.EqualsAndHashCode; 
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "material_contexts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "context_id")
    private Long contextId;

    // 👇 ĐÃ THÊM LÁ CHẮN CHỐNG LẶP VÔ TẬN VỚI STUDY MATERIAL
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false, unique = true)
    private StudyMaterial studyMaterial;

    @Column(name = "extracted_text", columnDefinition = "NVARCHAR(MAX)")
    private String extractedText;

    @Column(name = "summary", columnDefinition = "NVARCHAR(MAX)")
    private String summary;

    @Column(name = "extracted_keywords", columnDefinition = "NVARCHAR(MAX)")
    private String extractedKeywords;

    @Column(name = "embedding_status")
    private String embeddingStatus;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 👇 THÊM VÀO ĐÂY ĐỂ AN TOÀN TUYỆT ĐỐI VỚI DANH SÁCH CÂU HỎI
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "materialContext", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Question> questions;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.embeddingStatus == null) {
            this.embeddingStatus = "pending";
        }
    }
}
