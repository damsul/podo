package com.podo.domain.payroom.exception;

import com.podo.global.exception.BusinessException;
import com.podo.global.exception.ErrorCode;

public class ParticipantNotFoundException extends BusinessException {

    public ParticipantNotFoundException() {
        super(ErrorCode.PARTICIPANT_NOT_FOUND);
    }

    public ParticipantNotFoundException(Long participantId) {
        super(ErrorCode.PARTICIPANT_NOT_FOUND, "결제 참여자를 찾을 수 없습니다. ID: " + participantId);
    }
}
