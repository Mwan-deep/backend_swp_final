package AI_Study_Hub.repository;

import AI_Study_Hub.entity.Report;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // Spring Boot JPA sẽ tự động tạo câu lệnh SQL cho các hàm này:

    // Tìm tất cả các báo cáo theo trạng thái (Ví dụ: tìm tất cả report đang PENDING)
    List<Report> findByStatus(String status);

    // Tìm tất cả báo cáo do một user cụ thể gửi
    List<Report> findByAccountId(Long accountId);

    // Tìm tất cả báo cáo nhắm vào một tài liệu cụ thể
    List<Report> findByMaterialId(Long materialId);

    // ĐÃ FIX: Trỏ thẳng vào biến r.materialId trong class Report
    @Modifying
    @Transactional
    @Query("DELETE FROM Report r WHERE r.materialId = :materialId")
    void deleteAllReportsByMaterialId(Long materialId);
}