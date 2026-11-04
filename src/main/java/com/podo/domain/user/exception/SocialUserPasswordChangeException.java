package com.podo.domain.user.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class SocialUserPasswordChangeException extends BusinessException {
    public SocialUserPasswordChangeException() {
        super(ErrorCode.SOCIAL_USER_PASSWORD_CHANGE);
    }
}
