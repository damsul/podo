package com.podo.domain.user.dto.response;

import com.podo.domain.payroom.entity.RoomMember;
import com.podo.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdminUserDetailResponse {

    private Long id;
    private String name;
    private String loginId;
    private String email;
    private String profileImageUrl;
    private String socialLabel;
    private boolean socialUser;
    private LocalDateTime createdAt;
    private boolean deleted;
    private List<RoomInfo> rooms;

    @Getter
    @Builder
    public static class RoomInfo {
        private Long roomId;
        private String title;
        private String roleLabel;
        private LocalDateTime createdAt;
    }

    public static AdminUserDetailResponse of(User user, List<RoomMember> memberships) {
        List<RoomInfo> roomInfos = memberships.stream()
                .map(membership -> RoomInfo.builder()
                        .roomId(membership.getPayRoom().getId())
                        .title(membership.getPayRoom().getTitle())
                        .roleLabel(membership.getPayRoom().getOwner().getId().equals(user.getId()) ? "방장" : "참여자")
                        .createdAt(membership.getPayRoom().getCreatedAt())
                        .build())
                .toList();

        return AdminUserDetailResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .socialLabel(AdminUserListResponse.toSocialLabel(user.getSocialType()))
                .socialUser(user.isSocialUser())
                .createdAt(user.getCreatedAt())
                .deleted(user.isDeleted())
                .rooms(roomInfos)
                .build();

    }
}
