package com.podo.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    // 토큰 종류 구분: Refresh 토큰을 Access 토큰처럼 쓰지 못하게 막는다
    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    private final String secret;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private SecretKey secretKey;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.secret = secret;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // 1. 토큰 생성
    public String createToken(Long userId) {
        return buildToken(userId, accessTokenExpiration, ACCESS_TYPE);
    }

    public String createRefreshToken(Long userId) {
        return buildToken(userId, refreshTokenExpiration, REFRESH_TYPE);
    }

    private String buildToken(Long userId, long expiration, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId)) // 토큰 주인 (userId)
                .claim(TYPE_CLAIM, type)         // 토큰 종류 (access / refresh)
                .issuedAt(now)                   // 발급 시각
                .expiration(expiry)              // 만료 시각
                .signWith(secretKey)             // 서명
                .compact();
    }

    // 2. 토큰에서 userId 추출
    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    // 3. 토큰 검증 (서명·만료 + 종류)
    public boolean validateAccessToken(String token) {
        return validateTokenType(token, ACCESS_TYPE);
    }

    public boolean validateRefreshToken(String token) {
        return validateTokenType(token, REFRESH_TYPE);
    }

    private boolean validateTokenType(String token, String expectedType) {
        try {
            Claims claims = parseClaims(token);
            String type = claims.get(TYPE_CLAIM, String.class);
            if (!expectedType.equals(type)) {
                log.warn("JWT 토큰 종류 불일치: expected={}, actual={}", expectedType, type);
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 JWT 토큰: {}", e.getMessage());
            return false;
        }
    }

    // 토큰 파싱 (서명 검증 포함)
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}