package com.podo.domain.payroom.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class NotRoomOwnerException extends BusinessException {

    public NotRoomOwnerException() {
        super(ErrorCode.NOT_ROOM_OWNER);
    }
}
