package AI_Study_Hub.controller;

import AI_Study_Hub.dto.response.ChatMessageDTO;
import AI_Study_Hub.entity.Account;
import AI_Study_Hub.entity.ChatSession;
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AccountRepository accountRepository;

    private Account getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Lỗi định danh"));
    }

    // 1. API: Lấy danh sách các phiên chat của User (Sidebar)
    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions() {
        try {
            Account currentUser = getCurrentUser();
            List<ChatSession> sessions = chatService.getUserChatSessions(currentUser.getAccountId());

            // Map sang dạng nhẹ để Frontend dễ hiển thị Sidebar
            List<Map<String, Object>> result = sessions.stream().map(s -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", s.getSessionId());
                map.put("title", s.getTitle());
                map.put("materialId", s.getStudyMaterial() != null ? s.getStudyMaterial().getMaterialId() : null);
                map.put("createdAt", s.getCreatedAt());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("result", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 2. API: Gửi câu hỏi cho AI (Có lưu lịch sử)
    @PostMapping("/ask")
    public ResponseEntity<?> askAI(
            @RequestParam(value = "sessionId", required = false) Long sessionId, // Tùy chọn (null là chat mới)
            @RequestParam(value = "materialId", required = false) Long materialId, // Tùy chọn (null là không đính kèm tài liệu)
            @RequestParam("prompt") String prompt) {

        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bạn chưa nhập câu hỏi!"));
        }

        try {
            Account currentUser = getCurrentUser();
            ChatSession updatedSession = chatService.processUserMessage(currentUser.getAccountId(), sessionId, materialId, prompt);

            // Lấy ra tin nhắn AI cuối cùng vừa được tạo để trả về
            List<ChatMessageDTO> history = chatService.getChatHistory(updatedSession.getSessionId());
            ChatMessageDTO aiResponse = history.get(history.size() - 1);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", updatedSession.getSessionId());
            response.put("title", updatedSession.getTitle()); // Trả về title phòng khi đây là session mới tạo
            response.put("answer", aiResponse.getText());

            return ResponseEntity.ok(Map.of("result", response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi hệ thống Chat: " + e.getMessage()));
        }
    }

    // 3. API: Lấy chi tiết lịch sử tin nhắn của 1 phiên chat
    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(@RequestParam("sessionId") Long sessionId) {
        try {
            List<ChatMessageDTO> history = chatService.getChatHistory(sessionId);
            return ResponseEntity.ok(Map.of("result", history));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi lấy lịch sử chat: " + e.getMessage()));
        }
    }
}