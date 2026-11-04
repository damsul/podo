package com.podo.domain.cs.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class InquiryNotFoundException extends BusinessException {
    public InquiryNotFoundException(Long id) {
        super(ErrorCode.INQUIRY_NOT_FOUND, "문의를 찾을 수 없습니다. ID: " + id);
    }
}
