package AI_Study_Hub.repository;

import AI_Study_Hub.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    // Cũ (Vẫn giữ lại nếu có chỗ nào khác dùng)
    List<ChatSession> findByAccount_AccountIdAndStudyMaterial_MaterialId(Long accountId, Long materialId);

    // MỚI THÊM: Lấy danh sách tất cả các phiên chat của User (Sắp xếp mới nhất lên đầu)
    List<ChatSession> findByAccount_AccountIdOrderByCreatedAtDesc(Long accountId);
}