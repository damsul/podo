package com.podo.domain.user.dto.response;

import com.podo.domain.user.entity.SocialType;
import com.podo.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminUserListResponse {

    private Long id;
    private String name;
    private String loginId;
    private String email;
    private String socialLabel;
    private LocalDateTime createdAt;
    private boolean deleted;

    public static AdminUserListResponse of(User user) {
        return AdminUserListResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .socialLabel(toSocialLabel(user.getSocialType()))
                .createdAt(user.getCreatedAt())
                .deleted(user.isDeleted())
                .build();
    }

    static String toSocialLabel(SocialType type) {
        if (type == null) return "일반";
        return switch (type.name()) {
            case "KAKAO" -> "카카오";
            case "GOOGLE" -> "구글";
            case "NAVER" -> "네이버";
            default -> type.name();
        };
    }
}
