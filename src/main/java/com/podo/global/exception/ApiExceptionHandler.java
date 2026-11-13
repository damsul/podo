package com.podo.global.exception;

import com.podo.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice(basePackages = "com.podo.api")
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("API 비즈니스 예외: code={}, message={}", errorCode.getCode(), e.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), e.getMessage()));
    }

    // 로그인 실패: 아이디와 비밀번호 중 무엇이 틀렸는지 알려 주지 않는다
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException e) {
        log.warn("API 인증 실패: {}", e.getMessage());
        return error(ErrorCode.LOGIN_FAILED, ErrorCode.LOGIN_FAILED.getMessage());
    }

    // @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("잘못된 입력값입니다.");

        log.warn("API 검증 실패: {}", message);
        return error(ErrorCode.INVALID_INPUT, message);
    }

    // 요청 본문이 없거나 JSON 형식이 잘못됨 (쉼표 누락, 숫자 자리에 문자 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("API 요청 본문 오류: {}", e.getMessage());
        return error(ErrorCode.INVALID_INPUT, "요청 본문의 형식이 올바르지 않습니다.");
    }

    // 경로·쿼리 값의 타입이 맞지 않음 (예: /api/rooms/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("API 타입 불일치: name={}, value={}", e.getName(), e.getValue());
        return error(ErrorCode.INVALID_INPUT, "'" + e.getName() + "' 값의 형식이 올바르지 않습니다.");
    }

    // 필수 쿼리 파라미터 누락 (예: /api/users/search?keyword= 빠짐)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("API 필수 파라미터 누락: {}", e.getParameterName());
        return error(ErrorCode.INVALID_INPUT, "'" + e.getParameterName() + "' 값이 필요합니다.");
    }

    // multipart 요청에서 필수 파일 누락
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPart(MissingServletRequestPartException e) {
        log.warn("API 필수 파일 누락: {}", e.getRequestPartName());
        return error(ErrorCode.INVALID_INPUT, "'" + e.getRequestPartName() + "' 파일이 필요합니다.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("API 예상치 못한 예외", e);
        return error(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage());
    }

    private ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), message));
    }
}