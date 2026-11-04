package com.podo.domain.payroom.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class NotAccountModifierException extends BusinessException {
    public NotAccountModifierException() {
        super(ErrorCode.NOT_ACCOUNT_MODIFIER);
    }
}
