package com.podo.domain.payroom.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class NotRoomMemberException extends BusinessException {

    public NotRoomMemberException() {
        super(ErrorCode.NOT_ROOM_MEMBER);
    }
}
