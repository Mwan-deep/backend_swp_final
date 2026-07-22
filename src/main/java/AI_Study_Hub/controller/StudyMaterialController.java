package AI_Study_Hub.controller;

import AI_Study_Hub.dto.response.StudyMaterialResponseDTO;
import AI_Study_Hub.entity.Account;
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.service.GoogleDriveService;
import AI_Study_Hub.service.StudyMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class StudyMaterialController {

    private final StudyMaterialService materialService;
    private final GoogleDriveService googleDriveService;
    private final AccountRepository accountRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @RequestParam("specializationId") Long specializationId,
            @RequestParam("semesterId") Long semesterId,
            @RequestParam("subjectName") String subjectName,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("file") MultipartFile file) {

        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Account currentUser = accountRepository.findByUserName(username)
                    .orElseThrow(() -> new RuntimeException("Không xác định được danh tính người dùng"));

            StudyMaterialResponseDTO savedMaterial = materialService.uploadDocument(
                    currentUser.getAccountId(), specializationId, semesterId, subjectName, title, description, file
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(savedMaterial);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi khi upload: " + e.getMessage());
        }
    }

    @GetMapping("/my-contributions")
    public ResponseEntity<?> getMyContributions() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Account currentUser = accountRepository.findByUserName(username)
                    .orElseThrow(() -> new RuntimeException("Không xác định được danh tính người dùng"));

            List<StudyMaterialResponseDTO> myDocs = materialService.getMaterialsByAccountId(currentUser.getAccountId());

            return ResponseEntity.ok(myDocs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi truy xuất tài liệu của bạn: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getDocuments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "majorId", required = false) Long majorId,
            @RequestParam(value = "specializationId", required = false) Long specializationId) {

        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Account currentUser = accountRepository.findByUserName(username)
                    .orElseThrow(() -> new RuntimeException("Lỗi định danh"));

            List<StudyMaterialResponseDTO> results = materialService.getFilteredMaterials(
                    currentUser.getAccountId(), semesterId, majorId, specializationId, keyword
            );

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi truy xuất tài liệu: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudyMaterialResponseDTO> getDocumentDetail(@PathVariable("id") Long id) {
        try {
            StudyMaterialResponseDTO material = materialService.getMaterialDetailWithViewCount(id);
            return ResponseEntity.ok(material);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadDocument(@PathVariable("id") Long id) {
        try {
            StudyMaterialResponseDTO material = materialService.getMaterialForDownloadWithDownloadCount(id);
            String fileIdOrUrl = material.getFileUrl();

            if (fileIdOrUrl == null || fileIdOrUrl.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tài liệu không có file đính kèm!");
            }

            if (fileIdOrUrl.startsWith("http://") || fileIdOrUrl.startsWith("https://")) {
                org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(fileIdOrUrl);
                String contentType = material.getFileType() != null ? material.getFileType() : "application/octet-stream";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.getFileName() + "\"")
                        .body(resource);
            }

            com.google.api.services.drive.Drive driveService = googleDriveService.getDriveService();

            java.io.InputStream inputStream = driveService.files().get(fileIdOrUrl).executeMediaAsInputStream();
            org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(inputStream);

            String contentType = material.getFileType() != null ? material.getFileType() : "application/octet-stream";
            String downloadFileName = material.getFileName() != null ? material.getFileName() : "downloaded_document.pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống khi tải file từ Google Drive: " + e.getMessage());
        }
    }

    // ĐÃ CẬP NHẬT: Nhận thêm tham số "reason" (lý do) để gửi thông báo
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocument(
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", required = false) String reason) {
        try {
            materialService.deleteMaterial(id, reason);
            return ResponseEntity.ok("Đã xóa tài liệu và file vật lý thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi khi xóa: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllDocumentsForManager() {
        try {
            List<StudyMaterialResponseDTO> results = materialService.getAllMaterialsForManager();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi truy xuất hệ thống tài liệu: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/visibility")
    public org.springframework.http.ResponseEntity<?> updateVisibility(
            @PathVariable("id") Long id,
            @RequestBody java.util.Map<String, String> request) {
        try {
            String visibility = request.get("visibility");
            if (visibility == null || visibility.trim().isEmpty()) {
                return org.springframework.http.ResponseEntity.badRequest().body("Thiếu trạng thái visibility");
            }

            var result = materialService.updateVisibility(id, visibility);
            return org.springframework.http.ResponseEntity.ok(result);

        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }
}