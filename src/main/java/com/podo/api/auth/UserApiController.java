package com.podo.api.auth;

import com.podo.domain.user.dto.response.UserResponse;
import com.podo.domain.user.service.UserService;
import com.podo.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @GetMapping("/search")
    public ApiResponse<List<UserResponse>> search(@RequestParam String keyword) {
        return ApiResponse.success(userService.search(keyword));
    }

}
