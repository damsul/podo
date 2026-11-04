package com.podo.domain.payroom.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class NotPayerSelfException extends BusinessException {

    public NotPayerSelfException() {
        super(ErrorCode.NOT_PAYER_SELF);
    }
}
