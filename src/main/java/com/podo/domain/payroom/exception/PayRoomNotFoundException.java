package com.podo.domain.payroom.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class PayRoomNotFoundException extends BusinessException {

    public PayRoomNotFoundException() {
        super(ErrorCode.PAY_ROOM_NOT_FOUND);
    }

    public PayRoomNotFoundException(Long roomId) {
        super(ErrorCode.PAY_ROOM_NOT_FOUND, "페이방을 찾을 수 없습니다. ID: " + roomId);
    }
}
