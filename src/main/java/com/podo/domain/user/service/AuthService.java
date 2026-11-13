package com.podo.domain.user.service;

import com.podo.domain.user.dto.request.LoginRequest;
import com.podo.domain.user.dto.response.TokenResponse;
import com.podo.domain.user.entity.RefreshToken;
import com.podo.domain.user.exception.InvalidRefreshTokenException;
import com.podo.domain.user.repository.RefreshTokenRepository;
import com.podo.domain.user.repository.UserRepository;
import com.podo.global.security.from.CustomUserDetails;
import com.podo.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final long REFRESH_TOKEN_DAYS = 14;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    // 로그인: 아이디·비밀번호 확인 후 Access + Refresh 발급, Refresh는 사용자당 1개만 저장
    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLoginId(), request.getPassword())
        );

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        Long userId = principal.getUserId();

        String accessToken = jwtTokenProvider.createToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        saveOrUpdateRefreshToken(userId, refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }

    // 재발급: Refresh 토큰 검증 → DB 저장값과 비교 → 새 토큰으로 교체(회전)
    @Transactional
    public TokenResponse refresh(String oldRefreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(oldRefreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        Long userId = jwtTokenProvider.getUserId(oldRefreshToken);

        boolean active = userRepository.findById(userId)
                .map(user -> !user.isDeleted())
                .orElse(false);
        if (!active) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken savedToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!savedToken.getToken().equals(oldRefreshToken) || savedToken.isExpired()) {
            throw new InvalidRefreshTokenException();
        }

        String newAccessToken = jwtTokenProvider.createToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        savedToken.updateToken(newRefreshToken, LocalDateTime.now().plusDays(REFRESH_TOKEN_DAYS));

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    // 로그아웃: 저장된 Refresh 토큰을 지워 더 이상 재발급되지 않게 한다
    // (이미 발급된 Access 토큰은 만료 시간(1시간)까지 유효)
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.findByUserId(userId)
                .ifPresent(refreshTokenRepository::delete);
    }

    private void saveOrUpdateRefreshToken(Long userId, String refreshToken) {
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(REFRESH_TOKEN_DAYS);

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