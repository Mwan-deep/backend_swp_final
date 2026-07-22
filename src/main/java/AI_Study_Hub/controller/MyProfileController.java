package AI_Study_Hub.controller;

import AI_Study_Hub.dto.request.AccountUpdateRequest;
import AI_Study_Hub.dto.response.ApiResponse;
import AI_Study_Hub.dto.response.AccountResponse;
import AI_Study_Hub.entity.Device;
import AI_Study_Hub.service.AccountService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/my-profile")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class MyProfileController {

    @Autowired
    AccountService accountService;

    @GetMapping
    public ApiResponse<AccountResponse> getMyProfile(){
        var result = accountService.getMyInfo();
        return ApiResponse.<AccountResponse>builder().result(result).build();
    }

    @PutMapping
    public ApiResponse<AccountResponse> updateMyProfile(@RequestBody AccountUpdateRequest request){
        var result = accountService.updateMyProfile(request);
        return ApiResponse.<AccountResponse>builder().message("Update Successfully!!!").result(result).build();
    }

    @PostMapping("/upload-avatar")
    public ApiResponse<AccountResponse> uploadMyAvatar(@RequestParam("file") MultipartFile file) {
        var result = accountService.uploadMyAvatar(file);
        return ApiResponse.<AccountResponse>builder().message("Upload Avatar Successfully!!!").result(result).build();
    }

    @GetMapping("/devices")
    public ApiResponse<List<Device>> getMyDevices() {
        var result = accountService.getMyDevices();
        return ApiResponse.<List<Device>>builder().result(result).build();
    }

    @DeleteMapping("/devices/{deviceId}")
    public ApiResponse<Void> removeMyDevice(@PathVariable String deviceId) {
        accountService.removeMyDevice(deviceId);
        return ApiResponse.<Void>builder().message("Đăng xuất thiết bị thành công!").build();
    }
}