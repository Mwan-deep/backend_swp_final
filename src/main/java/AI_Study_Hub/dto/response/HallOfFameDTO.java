package AI_Study_Hub.dto.response;
import lombok.*;
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class HallOfFameDTO {
    Long id; String month; String category; String badgeName;
    AccountInfo account;

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class AccountInfo {
        Long accountId; String userName; String fullName; String avatarUrl;
    }
}