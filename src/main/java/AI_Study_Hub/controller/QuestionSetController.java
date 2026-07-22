package AI_Study_Hub.controller;

import AI_Study_Hub.dto.response.ApiResponse;
import AI_Study_Hub.dto.response.QuestionSetResponseDTO;
import AI_Study_Hub.service.QuestionSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/question-sets")
@RequiredArgsConstructor
public class QuestionSetController {

    private final QuestionSetService questionSetService;

    // API 1: Lấy danh sách bộ câu hỏi
    @GetMapping
    public ResponseEntity<?> getMyQuestionSets() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            List<QuestionSetResponseDTO> result = questionSetService.getMyQuestionSets(username);

            return ResponseEntity.ok(
                    ApiResponse.<List<QuestionSetResponseDTO>>builder()
                            .code(2000)
                            .message("Lấy danh sách bộ câu hỏi thành công")
                            .result(result)
                            .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // THÊM API 2: Lấy tất cả câu hỏi
    @GetMapping("/questions")
    public ResponseEntity<?> getAllQuestions() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            List<QuestionSetResponseDTO.QuestionDetailDTO> result = questionSetService.getAllQuestions(username);

            return ResponseEntity.ok(
                    ApiResponse.<List<QuestionSetResponseDTO.QuestionDetailDTO>>builder()
                            .code(2000)
                            .message("Lấy toàn bộ câu hỏi thành công")
                            .result(result)
                            .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @PostMapping("/questions/create")
    public ResponseEntity<?> createManualQuestion(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, Object> payload) {
        try {
            QuestionSetResponseDTO.QuestionDetailDTO savedQuestion = questionSetService.createManualQuestion(payload);

            return ResponseEntity.ok(
                    ApiResponse.<QuestionSetResponseDTO.QuestionDetailDTO>builder()
                            .code(2000)
                            .message("Lưu câu hỏi thủ công thành công")
                            .result(savedQuestion)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi lưu câu hỏi: " + e.getMessage());
        }
    }
}