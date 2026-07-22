package AI_Study_Hub.service;

import AI_Study_Hub.dto.response.StudyMaterialResponseDTO;
import AI_Study_Hub.entity.*;
import AI_Study_Hub.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyMaterialService {

    private final StudyMaterialRepository materialRepository;
    private final AccountRepository accountRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final SpecializationRepository specializationRepository;
    private final GoogleDriveService googleDriveService;
    private final DocumentAnalyzerService documentAnalyzerService;
    private final MaterialEngagementRepository engagementRepository;
    private final NotificationRepository notificationRepository;
    private final ReportRepository reportRepository;
    private final EntityManager entityManager;

    // ĐÃ THÊM: Repository để lưu log hoạt động
    private final ActivityLogRepository activityLogRepository;

    private final String UPLOAD_DIR = "uploads/";

    @Transactional
    public StudyMaterialResponseDTO uploadDocument(Long userId, Long specializationId, Long semesterId,
                                                   String subjectName, String title, String description, MultipartFile file) throws IOException {

        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Semester"));

        Subject subject = subjectRepository.findBySubjectNameAndSpecialization_SpecializationId(subjectName, specializationId)
                .orElseGet(() -> {
                    Specialization spec = specializationRepository.findById(specializationId)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy Chuyên ngành hẹp"));

                    Subject newSubject = new Subject();
                    newSubject.setSubjectName(subjectName);
                    newSubject.setSpecialization(spec);
                    return subjectRepository.save(newSubject);
                });

        String googleDriveFileId = googleDriveService.uploadFileToDrive(file);

        StudyMaterial material = StudyMaterial.builder()
                .account(account)
                .subject(subject)
                .semester(semester)
                .title(title)
                .description(description)
                .fileUrl(googleDriveFileId)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .visibility("PUBLIC")
                .viewCount(0)
                .downloadCount(0)
                .build();

        StudyMaterial savedMaterial = materialRepository.save(material);
        documentAnalyzerService.processAndSaveContext(savedMaterial, file);

        notificationRepository.save(Notification.builder()
                .account(account)
                .title("Upload tài liệu thành công")
                .message("Tài liệu '" + title + "' của bạn đã được đăng lên hệ thống.")
                .notificationType("documents")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());

        // ĐÃ THÊM: Ghi nhận hoạt động vào Recent Activity
        ActivityLog log = new ActivityLog();
        log.setAccount(account);
        log.setActionType("UPLOAD_DOCUMENT");
        log.setDescription("Đã tải lên tài liệu: " + title);
        log.setCreatedAt(LocalDateTime.now());
        activityLogRepository.save(log);

        return mapToDTO(savedMaterial);
    }

    // ... (Giữ nguyên các hàm còn lại của bạn) ...
    public List<StudyMaterialResponseDTO> getFilteredMaterials(Long accountId, Long semesterId, Long majorId, Long specializationId, String keyword) {
        List<StudyMaterial> materials = materialRepository.filterAndSearchVisibleMaterials(accountId, semesterId, majorId, specializationId, keyword);
        return materials.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public StudyMaterial getMaterialById(Long materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu với ID: " + materialId));
    }

    @Transactional
    public StudyMaterialResponseDTO getMaterialDetailWithViewCount(Long materialId) {
        StudyMaterial material = getMaterialById(materialId);
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isOwner = false;

        if (currentUsername != null && !currentUsername.equals("anonymousUser")) {
            Account currentUser = accountRepository.findByUserName(currentUsername).orElse(null);
            if (currentUser != null && material.getAccount() != null &&
                    currentUser.getAccountId().equals(material.getAccount().getAccountId())) {
                isOwner = true;
            }
        }

        if (!isOwner) {
            material.setViewCount(material.getViewCount() + 1);
            StudyMaterial savedMaterial = materialRepository.save(material);

            engagementRepository.save(MaterialEngagement.builder()
                    .studyMaterial(material)
                    .actionType("VIEW")
                    .createdAt(LocalDateTime.now())
                    .build());
            return mapToDTO(savedMaterial);
        }
        return mapToDTO(material);
    }

    @Transactional
    public StudyMaterialResponseDTO getMaterialForDownloadWithDownloadCount(Long materialId) {
        StudyMaterial material = getMaterialById(materialId);
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isOwner = false;

        if (currentUsername != null && !currentUsername.equals("anonymousUser")) {
            Account currentUser = accountRepository.findByUserName(currentUsername).orElse(null);
            if (currentUser != null && material.getAccount() != null &&
                    currentUser.getAccountId().equals(material.getAccount().getAccountId())) {
                isOwner = true;
            }
        }

        if (!isOwner) {
            material.setDownloadCount(material.getDownloadCount() + 1);
            StudyMaterial savedMaterial = materialRepository.save(material);

            engagementRepository.save(MaterialEngagement.builder()
                    .studyMaterial(material)
                    .actionType("DOWNLOAD")
                    .createdAt(LocalDateTime.now())
                    .build());
            return mapToDTO(savedMaterial);
        }
        return mapToDTO(material);
    }

    @Transactional
    public void deleteMaterial(Long materialId, String reason) throws IOException {
        StudyMaterial material = getMaterialById(materialId);
        String fileUrlOrId = material.getFileUrl();

        if (fileUrlOrId != null && !fileUrlOrId.isEmpty() && !fileUrlOrId.startsWith("http")) {
            try {
                googleDriveService.deleteFileFromDrive(fileUrlOrId);
            } catch (Exception e) {
                System.out.println("Bỏ qua lỗi Drive (File có thể đã bị xóa): " + e.getMessage());
            }
        }

        Account owner = material.getAccount();
        String title = material.getTitle();

        engagementRepository.deleteByStudyMaterialId(materialId);
        reportRepository.deleteAllReportsByMaterialId(materialId);

        try {
            entityManager.createNativeQuery(
                    "DELETE FROM quiz_questions WHERE question_id IN (" +
                            "SELECT q.question_id FROM questions q " +
                            "JOIN material_contexts mc ON q.context_id = mc.context_id " +
                            "WHERE mc.material_id = :materialId)"
            ).setParameter("materialId", materialId).executeUpdate();
        } catch (Exception e) {
            System.out.println("Bỏ qua dọn dẹp quiz_questions: " + e.getMessage());
        }

        materialRepository.delete(material);

        if (owner != null) {
            String actualReason = (reason != null && !reason.trim().isEmpty()) ? reason : "Vi phạm tiêu chuẩn cộng đồng hoặc chứa nội dung không phù hợp.";

            notificationRepository.save(Notification.builder()
                    .account(owner)
                    .title("Tài liệu vi phạm đã bị gỡ bỏ")
                    .message("Tài liệu '" + title + "' của bạn đã bị Quản trị viên xóa khỏi hệ thống. Lý do: " + actualReason)
                    .notificationType("system")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    public List<StudyMaterialResponseDTO> getMaterialsByAccountId(Long accountId) {
        List<StudyMaterial> materials = materialRepository.findByAccount_AccountIdOrderByCreatedAtDesc(accountId);
        return materials.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public StudyMaterialResponseDTO mapToDTO(StudyMaterial entity) {
        return StudyMaterialResponseDTO.builder()
                .materialId(entity.getMaterialId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .fileName(entity.getFileName())
                .fileType(entity.getFileType())
                .fileSize(entity.getFileSize())
                .fileUrl(entity.getFileUrl())
                .visibility(entity.getVisibility())
                .viewCount(entity.getViewCount())
                .downloadCount(entity.getDownloadCount())
                .createdAt(entity.getCreatedAt())
                .account(entity.getAccount() != null ?
                        StudyMaterialResponseDTO.AccountInfo.builder()
                                .userName(entity.getAccount().getUserName())
                                .fullName(entity.getAccount().getFullName())
                                .avatarUrl(entity.getAccount().getAvatarUrl())
                                .build() : null)
                .subject(entity.getSubject() != null ?
                        StudyMaterialResponseDTO.SubjectInfo.builder()
                                .subjectName(entity.getSubject().getSubjectName())

                                // ĐÃ BỔ SUNG: Truy xuất qua các bảng khóa ngoại để lấy tên Chuyên ngành lớn
                                .majorName(entity.getSubject().getSpecialization() != null &&
                                        entity.getSubject().getSpecialization().getMajor() != null
                                           ? entity.getSubject().getSpecialization().getMajor().getMajorName()
                                           : "Chung (General)")

                                .build() : null)
                .semester(entity.getSemester() != null ?
                        StudyMaterialResponseDTO.SemesterInfo.builder()
                                .semesterName(entity.getSemester().getSemesterName())
                                .year(entity.getSemester().getAcademicYear())
                                .displayName(entity.getSemester().getDisplayName())
                                .build() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StudyMaterialResponseDTO> getAllMaterialsForManager() {
        return materialRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public StudyMaterialResponseDTO updateVisibility(Long materialId, String newVisibility) {
        StudyMaterial material = getMaterialById(materialId);
        String currentUserName = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Account currentUser = accountRepository.findByUserName(currentUserName)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hiện tại"));

        boolean isOwner = material.getAccount() != null && material.getAccount().getAccountId().equals(currentUser.getAccountId());
        boolean isAdminOrManager = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN") || a.getAuthority().contains("MANAGER"));

        if (!isOwner && !isAdminOrManager) {
            throw new RuntimeException("Bạn không có quyền thay đổi trạng thái của tài liệu này!");
        }

        material.setVisibility(newVisibility);
        StudyMaterial savedMaterial = materialRepository.save(material);

        if (isAdminOrManager && "PUBLIC".equalsIgnoreCase(newVisibility)) {
            List<Report> reports = reportRepository.findByMaterialId(materialId);
            if (reports != null && !reports.isEmpty()) {
                reportRepository.deleteAllReportsByMaterialId(materialId);
                Account owner = material.getAccount();
                if (owner != null) {
                    notificationRepository.save(Notification.builder()
                            .account(owner)
                            .title("Tài liệu đã được xác nhận an toàn")
                            .message("Tài liệu '" + material.getTitle() + "' của bạn đã được Quản trị viên kiểm duyệt và đánh dấu là Hợp lệ (An toàn). Mọi báo cáo trước đó đã được gỡ bỏ.")
                            .notificationType("system")
                            .isRead(false)
                            .createdAt(LocalDateTime.now())
                            .build());
                }
            }
        }
        return mapToDTO(savedMaterial);
    }
}