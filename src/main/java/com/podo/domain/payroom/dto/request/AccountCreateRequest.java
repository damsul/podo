package com.podo.domain.payroom.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountCreateRequest {
    @NotNull(message = "계좌 소유자를 선택해주세요.")
    private Long userId;

    @NotBlank(message = "은행을 입력해주세요.")
    private String bank;

    @NotBlank(message = "계좌번호를 입력해주세요.")
    private String accountNumber;
}
