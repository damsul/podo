package com.podo.domain.user.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class DuplicateLoginIdException extends BusinessException {

    public DuplicateLoginIdException() {
        super(ErrorCode.DUPLICATE_LOGIN_ID);
    }

    public DuplicateLoginIdException(String loginId) {
        super(ErrorCode.DUPLICATE_LOGIN_ID, "이미 사용 중인 로그인 ID입니다: " + loginId);
    }
}
