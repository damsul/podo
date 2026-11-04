package com.podo.domain.user.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class SelfWithdrawException extends BusinessException {
    public SelfWithdrawException() {
        super(ErrorCode.CANNOT_WITHDRAW_SELF);
    }
}
