package com.podo.domain.cs.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class InquiryAlreadyAnsweredException extends BusinessException {
    public InquiryAlreadyAnsweredException() {
        super(ErrorCode.INQUIRY_ALREADY_ANSWER);
    }
}
