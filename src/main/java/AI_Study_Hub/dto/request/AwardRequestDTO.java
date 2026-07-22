package AI_Study_Hub.dto.request;
import lombok.Data;
@Data
public class AwardRequestDTO {
    private Long accountId;
    private String category;
    private String badgeName;
    private String month;
}