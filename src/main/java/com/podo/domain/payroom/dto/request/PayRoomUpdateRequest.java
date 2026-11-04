package com.podo.domain.payroom.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class PayRoomUpdateRequest {

    @NotBlank(message = "방 제목은 필수입니다")
    @Size(max = 50, message = "방 제목은 최대 50자까지 입력 가능합니다")
    private String title;

    private LocalDate dueDate;

    private Integer roundUnit;
}
