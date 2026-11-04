package com.podo.domain.user.dto.response;

import com.podo.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String loginId;
    private String name;
    private String email;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private boolean socialUser;

    /**
     * Entity -> Dto 변환 (정적 팩토리 메서드)
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .socialUser(user.isSocialUser())
                .build();
    }
}
