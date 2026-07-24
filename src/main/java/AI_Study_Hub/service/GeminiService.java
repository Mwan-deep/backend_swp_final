package AI_Study_Hub.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    // ĐÃ SỬA: Chuyển sang model gemini-1.5-pro (Phiên bản siêu cấp, suy luận tốt nhất, hỗ trợ 2 triệu Token)
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=";

    public String chatWithGemini(String prompt) {
        RestTemplate restTemplate = new RestTemplate();
        String url = GEMINI_API_URL + apiKey;

        // 1. Cấu hình Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. Xây dựng cấu trúc Body theo đúng chuẩn JSON
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> partsNode = new HashMap<>();
        partsNode.put("parts", Collections.singletonList(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(partsNode));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            // 3. Gửi Request sang Google
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            // 4. Bóc tách JSON bằng thư viện GSON
            JsonObject rootObject = JsonParser.parseString(response.getBody()).getAsJsonObject();

            return rootObject.getAsJsonArray("candidates").get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts").get(0).getAsJsonObject()
                    .get("text").getAsString();

        } catch (Exception e) {
            System.err.println("Lỗi khi gọi API Gemini: " + e.getMessage());
            return "Xin lỗi, AI hiện tại đang quá tải hoặc gặp sự cố kết nối. Vui lòng thử lại sau!";
        }
    }

    // Hàm chuyên dụng để phân tích tài liệu ngay khi vừa Upload
    public Map<String, String> analyzeDocument(String extractedText) {
        // ĐÃ NỚI LỎNG: Vì dùng bản Pro (2 triệu token), bạn có thể nới giới hạn cắt chữ này lên cao hơn
        // Ví dụ: từ 50000 lên 200000 hoặc bỏ luôn lệnh cắt nếu ngân sách của bạn cho phép
        String textToAnalyze = extractedText.length() > 50000 ? extractedText.substring(0, 50000) : extractedText;

        String prompt = "Dưới đây là nội dung của một tài liệu học tập. " +
                "Bạn hãy thực hiện 2 nhiệm vụ sau:\n" +
                "1. Viết một đoạn tóm tắt ngắn gọn (khoảng 3-5 câu) về nội dung chính của tài liệu.\n" +
                "2. Rút trích ra 5 đến 10 từ khóa (keywords) quan trọng nhất của tài liệu, phân cách nhau bằng dấu phẩy.\n\n" +
                "Định dạng câu trả lời bắt buộc phải tuân theo cấu trúc sau (không có ký tự nào khác):\n" +
                "TÓM TẮT: [Nội dung tóm tắt của bạn]\n" +
                "TỪ KHÓA: [Danh sách từ khóa]\n\n" +
                "--- NỘI DUNG TÀI LIỆU ---\n" + textToAnalyze;

        String response = chatWithGemini(prompt);

        // Bóc tách kết quả AI trả về để đưa vào Database
        Map<String, String> result = new HashMap<>();
        try {
            String[] parts = response.split("TỪ KHÓA:");
            String summaryPart = parts[0].replace("TÓM TẮT:", "").trim();
            String keywordPart = parts[1].trim();

            result.put("summary", summaryPart);
            result.put("keywords", keywordPart);
        } catch (Exception e) {
            result.put("summary", "Không thể tự động tóm tắt do lỗi định dạng.");
            result.put("keywords", "AI, Analysis, Error");
        }

        return result;
    }

    public List<String> generateDashboardSuggestions(List<String> recentActivities) {
        // Xử lý Empty State ngay từ đầu: Không gọi AI nếu không có hoạt động
        if (recentActivities == null || recentActivities.isEmpty()) {
            return Arrays.asList(
                    "Chào mừng bạn! Hãy tải lên tài liệu đầu tiên để bắt đầu.",
                    "Làm thử một bài Quiz để AI đánh giá năng lực của bạn nhé."
            );
        }

        // Tận dụng lại hàm chatWithGemini của bạn để code tái sử dụng tốt nhất
        String prompt = "Bạn là một gia sư AI. Dựa vào danh sách hoạt động học tập gần đây của tôi: ["
                + String.join(", ", recentActivities)
                + "]. Hãy đưa ra đúng 3 lời khuyên học tập ngắn gọn, thực tế (mỗi câu không quá 20 chữ) để tôi học tốt hơn. "
                + "BẮT BUỘC trả về kết quả theo định dạng ngăn cách bằng ký tự '|' như sau: Lời khuyên 1 | Lời khuyên 2 | Lời khuyên 3";

        String response = chatWithGemini(prompt);

        // Bóc tách kết quả dựa vào ký tự '|'
        try {
            String[] suggestions = response.split("\\|");
            List<String> result = new ArrayList<>();
            for (String s : suggestions) {
                // Xóa khoảng trắng thừa hoặc dấu chấm/số tự động do AI sinh ra
                String cleanS = s.trim().replaceAll("^\\d+\\.\\s*", "");
                if (!cleanS.isEmpty()) {
                    result.add(cleanS);
                }
            }
            // Nếu xử lý mượt mà, trả về mảng kết quả
            if (!result.isEmpty()) return result;

        } catch (Exception e) {
            System.err.println("Lỗi parse chuỗi gợi ý Gemini: " + e.getMessage());
        }

        // Phương án dự phòng (Fallback) nếu AI trả về chuỗi bị lỗi format
        return Arrays.asList(
                "Hệ thống AI đang bận xử lý, vui lòng thử lại sau.",
                "Hãy tiếp tục duy trì tiến độ học tập tuyệt vời của bạn nhé!"
        );
    }
}
