package AI_Study_Hub.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportCreateRequest {
    @NotNull(message = "Mã tài liệu không được để trống")
    Long materialId;

    @NotNull(message = "Lý do báo cáo không được để trống")
    String description;
}