package com.podo.domain.payroom.dto.request;

import com.podo.domain.payroom.entity.PayRoom;
import com.podo.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class PayRoomCreateRequest {

    @NotBlank(message = "방 제목은 필수입니다")
    @Size(max = 50, message = "방 제목은 최대 50자까지 입력 가능합니다")
    private String title;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private Integer roundUnit;

    @Builder
    public PayRoomCreateRequest(String title, LocalDate dueDate, Integer roundUnit) {
        this.title = title;
        this.dueDate = dueDate;
        this.roundUnit = roundUnit;
    }

    public PayRoom toEntity(User owner) {
        return PayRoom.builder()
                .owner(owner)
                .title(this.title)
                .dueDate(this.dueDate)
                .roundUnit(this.roundUnit)
                .build();
    }
}