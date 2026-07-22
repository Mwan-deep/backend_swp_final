package AI_Study_Hub.controller;

import AI_Study_Hub.dto.request.AwardRequestDTO;
import AI_Study_Hub.dto.response.ApiResponse;
import AI_Study_Hub.dto.response.CommunityStatDTO;
import AI_Study_Hub.dto.response.HallOfFameDTO;
import AI_Study_Hub.dto.response.LeaderboardDTO;
import AI_Study_Hub.service.CommunityService;
import AI_Study_Hub.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final LeaderboardService leaderboardService;
    private final CommunityService communityService; // ĐÃ THÊM

    // ====================================================
    // API CŨ: Lấy bảng xếp hạng Leaderboard
    // ====================================================
    @GetMapping("/leaderboard")
    public ApiResponse<List<LeaderboardDTO>> getLeaderboard(
            @RequestParam(value = "filter", defaultValue = "allTime") String filter) {

        List<LeaderboardDTO> result = leaderboardService.getLeaderboard();

        return ApiResponse.<List<LeaderboardDTO>>builder()
                .message("Lấy bảng xếp hạng thành công")
                .result(result)
                .build();
    }

    // ====================================================
    // API MỚI: Thống kê & Biểu dương (Quản trị viên)
    // ====================================================
    @GetMapping("/stats")
    public ApiResponse<List<CommunityStatDTO>> getCommunityStats() {
        return ApiResponse.<List<CommunityStatDTO>>builder()
                .message("Lấy thống kê cộng đồng thành công")
                .result(communityService.getCommunityStats())
                .build();
    }

    @GetMapping("/hall-of-fame")
    public ApiResponse<List<HallOfFameDTO>> getHallOfFame() {
        return ApiResponse.<List<HallOfFameDTO>>builder()
                .message("Lấy danh sách Bảng vàng thành công")
                .result(communityService.getHallOfFame())
                .build();
    }

    @PostMapping("/hall-of-fame")
    public ResponseEntity<?> awardBadgeToUser(@RequestBody AwardRequestDTO request) {
        try {
            communityService.awardBadgeToUser(request);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .message("Biểu dương người dùng thành công!")
                    .result("SUCCESS")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .message("Lỗi khi cấp huy hiệu: " + e.getMessage())
                    .result("ERROR")
                    .build());
        }
    }

    @DeleteMapping("/hall-of-fame/{id}")
    public ResponseEntity<?> revokeAward(@PathVariable("id") Long id) {
        try {
            communityService.revokeAward(id);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .message("Thu hồi danh hiệu thành công!")
                    .result("SUCCESS")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .message("Lỗi khi thu hồi: " + e.getMessage())
                    .result("ERROR")
                    .build());
        }
    }
}