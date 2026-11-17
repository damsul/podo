package com.podo.domain.payroom.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

// 정산 완료·취소는 방장 또는 그 결제의 결제자(돈을 받는 사람)만 할 수 있다
public class NotSettlementManagerException extends BusinessException {
    public NotSettlementManagerException() {
        super(ErrorCode.NOT_SETTLEMENT_MANAGER);
    }
}