package AI_Study_Hub.controller;

import AI_Study_Hub.dto.response.ApiResponse;
import AI_Study_Hub.dto.response.DashboardSummaryResponse;
import AI_Study_Hub.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping; // Bắt buộc phải import thư viện này
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // API: GET http://localhost:8080/api/v1/dashboard/summary
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getDashboardSummary() {
        // 1. Lấy username từ JWT Token
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Truyền username đó xuống tầng Service xử lý
        DashboardSummaryResponse summary = dashboardService.getDashboardSummary(userName);

        return ApiResponse.<DashboardSummaryResponse>builder()
                .code(2000)
                .message("Lấy dữ liệu Dashboard thành công")
                .result(summary)
                .build();
    }

    // API: POST http://localhost:8080/api/v1/dashboard/ping
    // Nhận tín hiệu "Nhịp tim" mỗi phút từ Frontend React
    @PostMapping("/ping")
    public ApiResponse<Void> pingActiveTime() {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        dashboardService.recordActiveMinute(userName);
        return ApiResponse.<Void>builder()
                .code(2000)
                .message("Ping success")
                .build();
    }
}