package AI_Study_Hub.service;

import AI_Study_Hub.dto.response.StudyMaterialResponseDTO;
import AI_Study_Hub.entity.Account;
import AI_Study_Hub.entity.ActivityLog;
import AI_Study_Hub.entity.Favorite;
import AI_Study_Hub.entity.StudyMaterial;
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.repository.ActivityLogRepository;
import AI_Study_Hub.repository.FavoriteRepository;
import AI_Study_Hub.repository.StudyMaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final AccountRepository accountRepository;
    private final StudyMaterialRepository materialRepository;
    private final StudyMaterialService studyMaterialService;

    // ĐÃ THÊM: Repository để lưu log
    private final ActivityLogRepository activityLogRepository;

    @Transactional
    public String toggleFavorite(Long accountId, Long materialId) {
        Optional<Favorite> existingFavorite = favoriteRepository.findByAccount_AccountIdAndStudyMaterial_MaterialId(accountId, materialId);

        if (existingFavorite.isPresent()) {
            favoriteRepository.delete(existingFavorite.get());
            return "Đã bỏ yêu thích tài liệu!";
        } else {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Tài khoản"));
            StudyMaterial material = materialRepository.findById(materialId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Tài liệu"));

            Favorite newFavorite = Favorite.builder()
                    .account(account)
                    .studyMaterial(material)
                    .build();

            favoriteRepository.save(newFavorite);

            // ĐÃ THÊM: Ghi nhận log khi thêm vào mục yêu thích
            ActivityLog log = new ActivityLog();
            log.setAccount(account);
            log.setActionType("FAVORITE_DOCUMENT");
            log.setDescription("Đã thêm vào yêu thích: " + material.getTitle());
            log.setCreatedAt(LocalDateTime.now());
            activityLogRepository.save(log);

            return "Đã thêm tài liệu vào danh sách yêu thích!";
        }
    }

    @Transactional(readOnly = true)
    public List<StudyMaterialResponseDTO> getUserFavoriteMaterials(Long accountId) {
        List<Favorite> favorites = favoriteRepository.findByAccount_AccountId(accountId);

        return favorites.stream()
                .map(Favorite::getStudyMaterial)
                .map(studyMaterialService::mapToDTO)
                .collect(Collectors.toList());
    }
}