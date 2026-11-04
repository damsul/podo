package com.podo.global.exception;

public class InvalidImageException extends BusinessException {
    public InvalidImageException() {
        super(ErrorCode.INVALID_IMAGE_FILE);
    }
}
