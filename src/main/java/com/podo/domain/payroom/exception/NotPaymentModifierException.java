package com.podo.domain.payroom.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class NotPaymentModifierException extends BusinessException {
    public NotPaymentModifierException() {
        super(ErrorCode.NOT_PAYMENT_MODIFIER);
    }
}
