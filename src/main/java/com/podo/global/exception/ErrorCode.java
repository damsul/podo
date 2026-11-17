package com.podo.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다"),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "U002", "이미 사용 중인 로그인 ID입니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U003", "이미 사용 중인 이메일입니다"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "U004", "비밀번호가 일치하지 않습니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "U005", "유효하지 않은 리프레시 토큰입니다"),
    SOCIAL_USER_PASSWORD_CHANGE(HttpStatus.FORBIDDEN, "U006", "소셜 로그인 계정은 비밀번호를 변경할 수 없습니다"),
    CANNOT_WITHDRAW_SELF(HttpStatus.FORBIDDEN, "U007", "본인 계정은 탈퇴 처리할 수 없습니다"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "U008", "아이디 또는 비밀번호가 올바르지 않습니다"),

    // PayRoom
    PAY_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "페이방을 찾을 수 없습니다"),
    NOT_ROOM_OWNER(HttpStatus.FORBIDDEN, "R002", "방장만 수행할 수 있는 작업입니다"),
    NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "R003", "방 멤버가 아닙니다"),
    ALREADY_ROOM_MEMBER(HttpStatus.CONFLICT, "R004", "이미 페이방의 멤버입니다"),
    NOT_SETTLEMENT_MANAGER(HttpStatus.FORBIDDEN, "R005", "정산 상태는 방장 또는 받는 사람만 변경할 수 있습니다"),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "결제 항목을 찾을 수 없습니다"),
    NOT_PAYMENT_MODIFIER(HttpStatus.FORBIDDEN, "P002", "결제를 수정/삭제할 권한이 없습니다"),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "결제 참여자를 찾을 수 없습니다"),
    NOT_PAYER_SELF(HttpStatus.FORBIDDEN, "P004", "본인만 결제자로 지정할 수 있습니다"),

    // Account
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "계좌를 찾을 수 없습니다"),
    NOT_ACCOUNT_MODIFIER(HttpStatus.FORBIDDEN, "A002", "본인 계좌만 관리할 수 있습니다"),

    // Notice
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "공지사항을 찾을 수 없습니다"),

    // FAQ
    FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "FAQ를 찾을 수 없습니다"),

    // 1:1 문의
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "I001", "문의를 찾을 수 없습니다"),
    INQUIRY_ALREADY_ANSWER(HttpStatus.CONFLICT, "I002", "답변이 완료된 문의는 수정할 수 없습니다"),
    NOT_INQUIRY_WRITER(HttpStatus.FORBIDDEN, "I003", "본인이 작성한 문의만 접근할 수 있습니다"),

    // IMAGE_FILE
    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "M001", "JPG, PNG, WEBP 형식의 5MB 이하 이미지만 올릴 수 있어요."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M002", "이미지 업로드에 실패했어요. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
