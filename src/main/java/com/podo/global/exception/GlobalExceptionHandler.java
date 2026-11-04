package com.podo.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice(basePackages = "com.podo.domain")
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException e, Model model) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("비즈니스 예외 발생: code={}, message={}", errorCode.getCode(), e.getMessage());

        model.addAttribute("status", errorCode.getStatus().value());
        model.addAttribute("code", errorCode.getCode());
        model.addAttribute("message", e.getMessage());

        return "error/error";
    }

    // favicon.ico 에러용
    // 정적 리소스 못 찾는 경우
    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResourceFound(NoResourceFoundException e, Model model) {
        log.debug("정적 리소스 없음: {}", e.getMessage());

        model.addAttribute("status", HttpStatus.NOT_FOUND.value());
        model.addAttribute("code", "C404");
        model.addAttribute("message", "요청하신 페이지를 찾을 수 없습니다");

        return "error/error";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("예상치 못한 예외 발생", e);

        model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("code", "C002");
        model.addAttribute("message", "서버 내부 오류가 발생했습니다");

        return "error/error";
    }
}
