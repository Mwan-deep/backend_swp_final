package AI_Study_Hub.controller;

import AI_Study_Hub.dto.response.StudyMaterialResponseDTO;
import AI_Study_Hub.entity.Account;
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final AccountRepository accountRepository;

    @PostMapping("/toggle")
    public ResponseEntity<String> toggleFavorite(@RequestParam("materialId") Long materialId) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Account currentUser = accountRepository.findByUserName(username)
                    .orElseThrow(() -> new RuntimeException("Lỗi định danh"));

            String resultMessage = favoriteService.toggleFavorite(currentUser.getAccountId(), materialId);
            return ResponseEntity.ok(resultMessage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/my-favorites")
    public ResponseEntity<?> getUserFavorites() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Account currentUser = accountRepository.findByUserName(username)
                    .orElseThrow(() -> new RuntimeException("Lỗi định danh"));

            // Đã trả về DTO
            List<StudyMaterialResponseDTO> favoriteMaterials = favoriteService.getUserFavoriteMaterials(currentUser.getAccountId());
            return ResponseEntity.ok(favoriteMaterials);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
}