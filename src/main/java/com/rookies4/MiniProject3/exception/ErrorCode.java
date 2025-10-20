package com.rookies4.MiniProject3.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부에 오류가 발생했습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C003", "해당 기능에 대한 접근 권한이 없습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "C004", "잘못된 요청입니다."),

    // Token
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "TOKEN_NOT_FOUND", "토큰을 찾을 수 없습니다. 다시 로그인해 주세요."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "해당 사용자를 찾을 수 없습니다."),
    USERNAME_DUPLICATION(HttpStatus.CONFLICT, "U002", "이미 사용 중인 아이디입니다."),
    NICKNAME_DUPLICATION(HttpStatus.CONFLICT, "U003", "이미 사용 중인 닉네임입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "U004", "아이디 또는 비밀번호가 올바르지 않습니다."),

    // Group
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "G001", "해당 모임을 찾을 수 없습니다."),
    ALREADY_JOINED_OR_PENDING(HttpStatus.CONFLICT, "G002", "이미 가입 신청했거나 가입된 모임입니다."),
    LEADER_CANNOT_JOIN(HttpStatus.BAD_REQUEST, "G003", "모임의 리더는 가입 신청할 수 없습니다."),
    LEADER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "G004", "모임의 리더는 탈퇴할 수 없습니다. 모임을 삭제하거나 리더를 위임해주세요."),
    MAX_MEMBERS_REACHED(HttpStatus.BAD_REQUEST, "G005", "모임의 최대 인원수에 도달했습니다."),
    JOIN_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "G006", "가입 신청 정보를 찾을 수 없습니다."),
    UPDATE_MAX_MEMBER_INVALID(HttpStatus.BAD_REQUEST, "G007", "최대 인원수는 현재 인원수보다 적게 설정할 수 없습니다."),
    NOT_A_MEMBER(HttpStatus.BAD_REQUEST, "G008", "해당 모임의 멤버가 아닙니다."),
    NOT_APPROVED_MEMBER(HttpStatus.BAD_REQUEST, "G009", "가입 승인된 멤버만 탈퇴가 가능합니다."),


    // Schedule
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "해당 일정을 찾을 수 없습니다."),
    INVALID_SCHEDULE_FOR_GROUP(HttpStatus.BAD_REQUEST, "S002", "해당 모임의 일정이 아닙니다."),

    // MetaData
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "존재하지 않는 지역입니다."),
    SPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "M002", "존재하지 않는 종목입니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}