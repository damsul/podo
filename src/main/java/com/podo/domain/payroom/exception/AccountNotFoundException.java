package com.podo.domain.payroom.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class AccountNotFoundException extends BusinessException {
    public AccountNotFoundException() {
        super(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    public AccountNotFoundException(Long accountId) {
        super(ErrorCode.ACCOUNT_NOT_FOUND, "계좌를 찾을 수 없습니다. ID: " + accountId);
    }
}
