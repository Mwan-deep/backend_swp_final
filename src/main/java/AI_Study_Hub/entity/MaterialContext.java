package AI_Study_Hub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List; // Thêm import List

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

    // --- THÊM ĐOẠN NÀY ĐỂ HẾT BÁO ĐỎ ---
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