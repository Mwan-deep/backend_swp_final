package AI_Study_Hub.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class DashboardSummaryResponse {

    // 1. Thống kê tổng quan (Stats Grid)
    private long totalDocuments;
    private int totalStudyMinutes;

    // 2. Dữ liệu Biểu đồ tuần (Weekly Chart)
    private Map<String, Integer> weeklyActivity;

    // 3. Dữ liệu Biểu đồ tháng (Monthly Uploads)
    private int monthlyGrowthPercentage;
    private List<Long> monthlyUploadsPerWeek;

    // 4. Danh sách hoạt động gần nhất (Vừa bổ sung)
    private List<String> recentActivities;

    // 5. Gợi ý từ Gemini AI (Vừa bổ sung)
    private List<String> aiSuggestions;
}