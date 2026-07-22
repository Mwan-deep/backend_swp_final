package AI_Study_Hub.dto.response;

import AI_Study_Hub.entity.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountResponse {
    Long accountId;           // Sửa 'id' thành 'accountId'
    String userName;
    // BỎ passwordHash ĐỂ BẢO MẬT DỮ LIỆU
    String fullName;
    String email;
    LocalDate dob;
    String gender;
    String avatarUrl;
    String bio;
    Set<Role> roles;          // Sửa 'role' thành 'roles'
    String accountStatus;     // Biến trạng thái
    LocalDateTime createdAt;  // Thêm để hiển thị ngày đăng ký
    LocalDateTime updatedAt;
}