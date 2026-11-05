package com.podo.domain.payroom.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountCreateRequest {

    @NotNull(message = "계좌 소유자를 선택해주세요.")
    private Long userId;

    @NotBlank(message = "은행을 입력해주세요.")
    @Size(max = 30, message = "은행명은 30자 이하로 입력해주세요.")
    private String bank;

    @NotBlank(message = "계좌번호를 입력해주세요.")
    @Size(max = 50, message = "계좌번호는 50자 이하로 입력해주세요.")
    private String accountNumber;
}