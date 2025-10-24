package com.rookies4.MiniProject3.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ==========================================================
    // ✅ Common
    // ==========================================================
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "[ERROR] 입력값이 올바르지 않습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "[ERROR] 요청 데이터가 누락되었거나 형식이 잘못되었습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "[ERROR] 서버 내부에 오류가 발생했습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "[ERROR] 해당 기능에 대한 접근 권한이 없습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "[ERROR] 잘못된 요청입니다."),

    // ==========================================================
    // ✅ Token / Auth
    // ==========================================================
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "[ERROR] 유효하지 않은 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "TOKEN_NOT_FOUND", "[ERROR] 토큰을 찾을 수 없습니다. 다시 로그인해 주세요."),

    // ==========================================================
    // ✅ User
    // ==========================================================
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "[ERROR] 해당 사용자를 찾을 수 없습니다."),
    EMAIL_DUPLICATION(HttpStatus.CONFLICT, "EMAIL_DUPLICATION", "[ERROR] 이미 사용 중인 이메일입니다."),
    USERNAME_DUPLICATION(HttpStatus.CONFLICT, "USERNAME_DUPLICATION", "[ERROR] 이미 사용 중인 닉네임입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "[ERROR] 아이디 또는 비밀번호가 올바르지 않습니다."),

    // ==========================================================
    // ✅ Content
    // ==========================================================
    FILE_NOT_ATTACHED(HttpStatus.BAD_REQUEST, "FILE_NOT_ATTACHED", "[ERROR] 처리할 파일을 첨부해주세요."),
    CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "[ERROR] 해당 콘텐츠를 찾을 수 없습니다."),
    PROCESSING_NOT_COMPLETED(HttpStatus.CONFLICT, "PROCESSING_NOT_COMPLETED", "[ERROR] 콘텐츠 처리가 아직 완료되지 않았습니다."),

    // ==========================================================
    // ✅ Summary
    // ==========================================================
    FAILED_TO_PROCESS_CONTENT(HttpStatus.BAD_REQUEST, "FAILED_TO_PROCESS_CONTENT", "[ERROR] FastAPI 요약 처리에 실패했습니다."),

    // ==========================================================
    // ✅ Quiz / AI
    // ==========================================================
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND", "[ERROR] 해당 퀴즈를 찾을 수 없습니다."),
    QUIZ_GENERATION_FAILED(HttpStatus.BAD_REQUEST, "QUIZ_GENERATION_FAILED", "[ERROR] 퀴즈 생성에 실패했습니다."),
    AI_SERVER_COMMUNICATION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVER_COMMUNICATION_ERROR", "[ERROR] AI 서버와의 통신에 실패했습니다."),

    // ==========================================================
    // ✅ DB / Server
    // ==========================================================
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "[ERROR] 데이터베이스 오류가 발생했습니다.");

    // ==========================================================
    // ✅ 필드
    // ==========================================================
    private final HttpStatus status;
    private final String code;
    private final String message;
}
