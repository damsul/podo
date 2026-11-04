package com.podo.domain.payroom.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class AlreadyRoomMemberException extends BusinessException {
    public AlreadyRoomMemberException() {
        super(ErrorCode.ALREADY_ROOM_MEMBER);
    }
}
