package com.podo.domain.payroom.dto.response;

import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.payroom.entity.RoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class PayRoomResponse {

    private Long id;
    private Long ownerId;
    private String ownerName;
    private String title;
    private RoomStatus status;
    private LocalDate dueDate;
    private int roundUnit;
    private LocalDateTime createdAt;

    public static PayRoomResponse from(PayRoom payRoom) {
        return PayRoomResponse.builder()
                .id(payRoom.getId())
                .ownerId(payRoom.getOwner().getId())
                .ownerName(payRoom.getOwner().getName())
                .title(payRoom.getTitle())
                .status(payRoom.getStatus())
                .dueDate(payRoom.getDueDate())
                .roundUnit(payRoom.getRoundUnit())
                .createdAt(payRoom.getCreatedAt())
                .build();
    }
}
