package com.podo.domain.cs.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class NoticeNotFoundException extends BusinessException {

    public NoticeNotFoundException(Long id) {
        super(ErrorCode.NOTICE_NOT_FOUND, "공지사항을 찾을 수 없습니다. ID: " + id);
    }
}
