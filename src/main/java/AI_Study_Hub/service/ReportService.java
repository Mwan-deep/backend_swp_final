package AI_Study_Hub.service;

import AI_Study_Hub.dto.request.ReportCreateRequest;
import AI_Study_Hub.dto.request.ReportUpdateRequest;
import AI_Study_Hub.dto.response.ReportDetailResponse;
import AI_Study_Hub.entity.Account;
import AI_Study_Hub.entity.Notification;
import AI_Study_Hub.entity.Report;
import AI_Study_Hub.entity.StudyMaterial;
import AI_Study_Hub.exception.AppException;
import AI_Study_Hub.exception.ErrorCode;
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.repository.NotificationRepository;
import AI_Study_Hub.repository.ReportRepository;
import AI_Study_Hub.repository.StudyMaterialRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ReportService {

    ReportRepository reportRepository;
    AccountRepository accountRepository;
    NotificationRepository notificationRepository;
    StudyMaterialRepository materialRepository;

    // 1. DÀNH CHO USER: Gửi báo cáo
    public void createReport(ReportCreateRequest request) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        Account reporter = accountRepository.findByUserName(userName)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_EXITS));

        StudyMaterial material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu với ID: " + request.getMaterialId()));

        Report report = Report.builder()
                .accountId(reporter.getAccountId())
                .materialId(request.getMaterialId())
                .description(request.getDescription())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        reportRepository.save(report);

        Account owner = material.getAccount();
        if (owner != null) {
            notificationRepository.save(Notification.builder()
                    .account(owner)
                    .title("Cảnh báo: Tài liệu bị tố cáo")
                    .message("Tài liệu '" + material.getTitle() + "' của bạn vừa bị người dùng báo cáo vi phạm.")
                    .notificationType("community") // Phân loại thông báo: Community
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    // 2. DÀNH CHO ADMIN & MANAGER: Xem chi tiết báo cáo
    public ReportDetailResponse getReportDetail(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo này!"));

        Account reporter = accountRepository.findById(report.getAccountId()).orElse(new Account());

        StudyMaterial material = materialRepository.findById(report.getMaterialId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu bị báo cáo"));

        Account reportedUser = material.getAccount() != null ? material.getAccount() : new Account();

        return ReportDetailResponse.builder()
                .reportId(report.getReportId())
                .description(report.getDescription())
                .status(report.getStatus())
                .internalNotes(report.getInternalNotes())
                .createdAt(report.getCreatedAt())
                .reporter(ReportDetailResponse.ReporterInfo.builder()
                        .accountId(reporter.getAccountId())
                        .userName(reporter.getUserName())
                        .fullName(reporter.getFullName())
                        .email(reporter.getEmail())
                        .avatarUrl(reporter.getAvatarUrl())
                        .build())
                .reportedUser(ReportDetailResponse.ReportedUserInfo.builder()
                        .accountId(reportedUser.getAccountId())
                        .userName(reportedUser.getUserName())
                        .fullName(reportedUser.getFullName())
                        .email(reportedUser.getEmail())
                        .avatarUrl(reportedUser.getAvatarUrl())
                        .accountStatus(reportedUser.getAccountStatus())
                        .build())
                .material(ReportDetailResponse.MaterialInfo.builder()
                        .materialId(material.getMaterialId())
                        .title(material.getTitle())
                        .build())
                .build();
    }

    // 3. DÀNH CHO ADMIN & MANAGER: Cập nhật trạng thái
    public ReportDetailResponse updateReport(Long reportId, ReportUpdateRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo này!"));

        if (request.getStatus() != null) {
            report.setStatus(request.getStatus());
        }
        if (request.getInternalNotes() != null) {
            report.setInternalNotes(request.getInternalNotes());
        }

        reportRepository.save(report);
        return getReportDetail(reportId);
    }

    // 4. DÀNH CHO ADMIN & MANAGER: Lấy danh sách tất cả báo cáo
    public List<ReportDetailResponse> getAllReports() {
        List<Report> reports = reportRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );

        return reports.stream()
                .map(report -> getReportDetail(report.getReportId()))
                .collect(Collectors.toList());
    }
}