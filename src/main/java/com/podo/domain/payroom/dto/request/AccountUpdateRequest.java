package com.podo.domain.payroom.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountUpdateRequest {
    private String bank;
    private String accountNumber;
    private String holderName;
}
