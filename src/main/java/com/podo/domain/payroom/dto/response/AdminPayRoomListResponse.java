package com.podo.domain.payroom.dto.response;

import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.RoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminPayRoomListResponse {

    private Long id;
    private String title;
    private String ownerName;
    private String ownerLoginId;
    private long memberCount;
    private RoomStatus status;
    private LocalDate dueDate;
    private int roundUnit;
    private LocalDateTime createdAt;

    public static AdminPayRoomListResponse of(PayRoom payRoom, long memberCount) {
        return AdminPayRoomListResponse.builder()
                .id(payRoom.getId())
                .title(payRoom.getTitle())
                .ownerName(payRoom.getOwner().getName())
                .ownerLoginId(payRoom.getOwner().getLoginId())
                .memberCount(memberCount)
                .status(payRoom.getStatus())
                .dueDate(payRoom.getDueDate())
                .roundUnit(payRoom.getRoundUnit())
                .createdAt(payRoom.getCreatedAt())
                .build();
    }
}
