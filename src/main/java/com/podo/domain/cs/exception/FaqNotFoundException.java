package com.podo.domain.cs.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class FaqNotFoundException extends BusinessException {

    public FaqNotFoundException(Long id) {
        super(ErrorCode.FAQ_NOT_FOUND, "FAQ를 찾을 수 없습니다. ID: " + id);
    }
}
