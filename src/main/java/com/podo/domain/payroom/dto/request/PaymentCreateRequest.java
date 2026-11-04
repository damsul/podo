package com.podo.domain.payroom.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PaymentCreateRequest {

    @NotBlank(message = "결제 항목 제목은 필수입니다")
    @Size(max = 100, message = "제목은 최대 100자까지 입력 가능합니다")
    private String title;

    @NotNull
    @Min(value = 1, message = "결제 금액은 0보다 커야 합니다")
    private Integer amount;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate paidAt;

    @NotNull(message = "결제자를 선택해주세요")
    private Long payerId;

    @NotEmpty(message = "참여자가 최소 1명 이상이어야 합니다")
    private List<Long> participantIds;

    @Builder
    public PaymentCreateRequest(String title, Integer amount, LocalDate paidAt,
                                Long payerId, List<Long> participantIds) {
        this.title = title;
        this.amount = amount;
        this.paidAt = paidAt;
        this.payerId = payerId;
        this.participantIds = participantIds;
    }
}
