package com.podo.api.auth;

import com.podo.domain.user.dto.request.LoginRequest;
import com.podo.domain.user.dto.request.RefreshTokenRequest;
import com.podo.domain.user.dto.response.TokenResponse;
import com.podo.domain.user.service.AuthService;
import com.podo.global.common.ApiResponse;
import com.podo.global.security.PrincipalUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request.getRefreshToken()));
    }

    // 로그인한 사용자만 호출 가능 (Access 토큰 필요)
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal PrincipalUser principal) {
        authService.logout(principal.getUserId());
        return ApiResponse.success();
    }
}