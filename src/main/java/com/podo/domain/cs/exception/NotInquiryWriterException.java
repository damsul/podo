package com.podo.domain.cs.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class NotInquiryWriterException extends BusinessException {
    public NotInquiryWriterException() {
        super(ErrorCode.NOT_INQUIRY_WRITER);
    }
}
