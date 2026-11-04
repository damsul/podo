package com.podo.domain.payroom.dto.response;

import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.RoomMember;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberInfo {

    private Long userId;
    private String name;
    private boolean isOwner;

    public static MemberInfo from(RoomMember roomMember, PayRoom payRoom) {
        return MemberInfo.builder()
                .userId(roomMember.getUser().getId())
                .name(roomMember.getUser().getName())
                .isOwner(payRoom.isOwner(roomMember.getUser()))
                .build();
    }
}
