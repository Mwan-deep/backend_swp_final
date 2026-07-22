package AI_Study_Hub.controller;

import AI_Study_Hub.dto.request.ReportCreateRequest;
import AI_Study_Hub.dto.request.ReportUpdateRequest;
import AI_Study_Hub.dto.response.ApiResponse;
import AI_Study_Hub.dto.response.ReportDetailResponse;
import AI_Study_Hub.service.ReportService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ReportController {

    ReportService reportService;

    // --- API DÀNH CHO NGƯỜI DÙNG TẠO BÁO CÁO ---
    @PostMapping
    public ApiResponse<Void> createReport(@RequestBody @Valid ReportCreateRequest request) {
        reportService.createReport(request);
        return ApiResponse.<Void>builder()
                .message("Đã gửi báo cáo thành công! Chúng tôi sẽ xem xét sớm nhất.")
                .build();
    }

    // --- API DÀNH CHO ADMIN & MANAGER ---
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<ReportDetailResponse> getReportDetail(@PathVariable Long id) {
        var result = reportService.getReportDetail(id);
        return ApiResponse.<ReportDetailResponse>builder()
                .result(result)
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<ReportDetailResponse> updateReport(
            @PathVariable Long id,
            @RequestBody ReportUpdateRequest request) {
        var result = reportService.updateReport(id, request);
        return ApiResponse.<ReportDetailResponse>builder()
                .message("Cập nhật trạng thái báo cáo thành công!")
                .result(result)
                .build();
    }
    // API Lấy danh sách tất cả báo cáo
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<List<ReportDetailResponse>> getAllReports() {
        var result = reportService.getAllReports();
        return ApiResponse.<List<ReportDetailResponse>>builder()
                .result(result)
                .build();
    }
}