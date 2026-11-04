package com.podo.domain.user.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException() {
        super(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
