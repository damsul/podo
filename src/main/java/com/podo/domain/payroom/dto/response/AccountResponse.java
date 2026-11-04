package com.podo.domain.payroom.dto.response;

import com.podo.domain.payroom.entity.Account;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String bank;
    private String accountNumber;
    private String holderName;

    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUser().getId())
                .userName(account.getUser().getName())
                .bank(account.getBank())
                .accountNumber(account.getAccountNumber())
                .holderName(account.getHolderName())
                .build();
    }
}
