package AI_Study_Hub.controller;

import AI_Study_Hub.dto.request.AccountCreateRequest;
import AI_Study_Hub.dto.request.AccountUpdateRequest;
import AI_Study_Hub.dto.response.ApiResponse;
import AI_Study_Hub.dto.request.ChangePasswordRequest;
import AI_Study_Hub.dto.response.AccountResponse;
import AI_Study_Hub.dto.response.ChangePasswordResponse;
import AI_Study_Hub.entity.Account;
import AI_Study_Hub.entity.Device;
import AI_Study_Hub.service.AccountService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AccountController {

    @Autowired
    AccountService accountService;

    // --- CÁC API CŨ DÀNH CHO ADMIN ---
    @PostMapping
    ApiResponse<AccountResponse> createAccount(@RequestBody @Valid AccountCreateRequest request){
        var result = accountService.createAccount(request);
        return ApiResponse.<AccountResponse>builder().message("Create Successfully!!!").result(result).build();
    }
    @PutMapping("/{id}")
    ApiResponse<AccountResponse> updateProfile(@RequestBody AccountUpdateRequest request, @PathVariable Long id){
        var result = accountService.updateAccount(request, id);
        return ApiResponse.<AccountResponse>builder().message("Update Successfully!!!").result(result).build();
    }
    @DeleteMapping("/{id}")
    ApiResponse<Void> deleteAccount(@PathVariable Long id){
        accountService.deleteAccount(id);
        return ApiResponse.<Void>builder().message("Delete Successfully!!!").result(null).build();
    }
    @GetMapping
        // Đổi List<Account> thành List<AccountResponse>
    ApiResponse<List<AccountResponse>> ManageUser(){
        var result = accountService.getAllAccount();
        return ApiResponse.<List<AccountResponse>>builder()
                .result(result)
                .build();
    }
    @PostMapping("/change_password")
    ApiResponse<ChangePasswordResponse> changePassword(@RequestBody ChangePasswordRequest request){
        var result = accountService.changePassword(request);
        return ApiResponse.<ChangePasswordResponse>builder().message("Change Password Successfully!!!").result(result).build();
    }
    @PostMapping("/createAccountByAdmin")
    ApiResponse<AccountResponse> createAccountByAdmin(@RequestBody @Valid AccountCreateRequest request){
        var result = accountService.createAccountByAdmin(request);
        return ApiResponse.<AccountResponse>builder().message("Create Successfully!!!").result(result).build();
    }
    @GetMapping("/infor/{id}")
    ApiResponse<AccountResponse> getAccountById(@PathVariable Long id){
        var result = accountService.GetAccountById(id);
        return ApiResponse.<AccountResponse>builder().result(result).build();
    }

    // =========================================================================
    // HỆ THỐNG API BẢO MẬT MỚI DÀNH RIÊNG CHO NGƯỜI DÙNG HIỆN TẠI (MY-PROFILE)
    // =========================================================================

    @GetMapping("/my-profile")
    public ApiResponse<AccountResponse> getMyProfile(){
        var result = accountService.getMyInfo();
        return ApiResponse.<AccountResponse>builder()
                .result(result)
                .build();
    }

    @PutMapping("/my-profile")
    public ApiResponse<AccountResponse> updateMyProfile(@RequestBody AccountUpdateRequest request){
        var result = accountService.updateMyProfile(request);
        return ApiResponse.<AccountResponse>builder()
                .message("Update Successfully!!!")
                .result(result)
                .build();
    }

    @PostMapping("/my-profile/upload-avatar")
    public ApiResponse<AccountResponse> uploadMyAvatar(@RequestParam("file") MultipartFile file) {
        var result = accountService.uploadMyAvatar(file);
        return ApiResponse.<AccountResponse>builder()
                .message("Upload Avatar Successfully!!!")
                .result(result)
                .build();
    }

    @GetMapping("/my-profile/devices")
    public ApiResponse<List<Device>> getMyDevices() {
        var result = accountService.getMyDevices();
        return ApiResponse.<List<Device>>builder()
                .result(result)
                .build();
    }

    @DeleteMapping("/my-profile/devices/{deviceId}")
    public ApiResponse<Void> removeMyDevice(@PathVariable String deviceId) {
        accountService.removeMyDevice(deviceId);
        return ApiResponse.<Void>builder()
                .message("Đăng xuất thiết bị thành công!")
                .build();
    }
    @CrossOrigin(origins = "*") // Thêm dòng này để ép trình duyệt cho phép gọi API
    @PutMapping("/{id}/status")
    public ApiResponse<AccountResponse> toggleStatus(@PathVariable("id") Long id){ // Bổ sung ("id")
        var result = accountService.toggleAccountStatus(id);
        return ApiResponse.<AccountResponse>builder()
                .message("Thay đổi trạng thái thành công!!!")
                .result(result)
                .build();
    }
}