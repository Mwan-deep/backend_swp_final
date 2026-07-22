package AI_Study_Hub.repository;

import AI_Study_Hub.entity.MaterialEngagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MaterialEngagementRepository extends JpaRepository<MaterialEngagement, Long> {
    // Lấy tất cả lịch sử tương tác diễn ra SAU một mốc thời gian cụ thể (Dùng cho lọc Tuần/Tháng)
    List<MaterialEngagement> findByCreatedAtAfter(LocalDateTime startDate);
    // THÊM ĐOẠN NÀY ĐỂ DỌN DẸP LƯỢT VIEW/DOWNLOAD

    @Modifying
    @Transactional
    @Query("DELETE FROM MaterialEngagement m WHERE m.studyMaterial.materialId = :materialId")
    void deleteByStudyMaterialId(@Param("materialId") Long materialId);
}