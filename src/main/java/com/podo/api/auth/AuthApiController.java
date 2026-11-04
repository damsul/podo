package com.podo.api.auth;

import com.podo.domain.user.dto.request.LoginRequest;
import com.podo.domain.user.dto.request.RefreshTokenRequest;
import com.podo.domain.user.dto.response.TokenResponse;
import com.podo.domain.user.entity.RefreshToken;
import com.podo.domain.user.exception.InvalidRefreshTokenException;
import com.podo.domain.user.repository.RefreshTokenRepository;
import com.podo.global.common.ApiResponse;
import com.podo.global.security.from.CustomUserDetails;
import com.podo.global.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/login")
    @Transactional
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLoginId(), request.getPassword())
        );

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        Long userId = principal.getUserId();

        String accessToken = jwtTokenProvider.createToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        saveOrUpdateRefreshToken(userId, refreshToken);

        return ApiResponse.success(new TokenResponse(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    @Transactional
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String oldRefreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(oldRefreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        Long userId = jwtTokenProvider.getUserId(oldRefreshToken);

        RefreshToken savedToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!savedToken.getToken().equals(oldRefreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        if (savedToken.isExpired()) {
            throw new InvalidRefreshTokenException();
        }

        String newAccessToken = jwtTokenProvider.createToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        savedToken.updateToken(newRefreshToken, LocalDateTime.now().plusDays(14));

        return ApiResponse.success(new TokenResponse(newAccessToken, newRefreshToken));

    }

    private void saveOrUpdateRefreshToken(Long userId, String refreshToken) {
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(14);

        refreshTokenRepository.findByUserId(userId)
                .ifPresentOrElse(
                        existing -> existing.updateToken(refreshToken, expiryDate),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .userId(userId)
                                        .token(refreshToken)
                                        .expiryDate(expiryDate)
                                        .build()
                        )
                );
    }
}
