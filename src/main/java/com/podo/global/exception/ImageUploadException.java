package com.podo.global.exception;

public class ImageUploadException extends BusinessException {
    public ImageUploadException() {
        super(ErrorCode.IMAGE_UPLOAD_FAILED);
    }
}
