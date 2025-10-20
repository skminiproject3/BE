package com.rookies4.MiniProject3.exception;

public enum ErrorCode {
    VALIDATION_ERROR(400, "VALIDATION_ERROR", "닉네임은 2자 이상 10자 이하로 입력해주세요."),
    INVALID_CREDENTIALS(401, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}