package com.podo.domain.user.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }

    public UserNotFoundException(Long userId) {
        super(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다. ID: " + userId);
    }

    public UserNotFoundException(String loginId) {
        super(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다. loginId: " + loginId);
    }
}
