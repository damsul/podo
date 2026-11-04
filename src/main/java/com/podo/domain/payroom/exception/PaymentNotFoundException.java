package com.podo.domain.payroom.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class PaymentNotFoundException extends BusinessException {

    public PaymentNotFoundException() {
        super(ErrorCode.PAYMENT_NOT_FOUND);
    }

    public PaymentNotFoundException(Long paymentId) {
        super(ErrorCode.PAYMENT_NOT_FOUND, "결제 항목을 찾을 수 없습니다. ID: " + paymentId);
    }
}
