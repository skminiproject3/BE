package com.rookies4.MiniProject3.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @Valid 어노테이션을 사용한 DTO의 유효성 검증에 실패했을 때 발생하는 예외를 처리합니다.
     * HTTP 400 Bad Request 상태와 함께 발생한 필드와 에러 메시지를 응답합니다.
     *
     * @param e MethodArgumentNotValidException 객체
     * @return ErrorResponse 객체를 담은 ResponseEntity
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("handleMethodArgumentNotValidException", e);
        final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult().getFieldError());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Spring Security를 통한 로그인 시 아이디 또는 비밀번호가 일치하지 않을 때 발생하는 예외를 처리합니다.
     * HTTP 401 Unauthorized 상태와 함께 에러 메시지를 응답합니다.
     *
     * @param e BadCredentialsException 객체
     * @return ErrorResponse 객체를 담은 ResponseEntity
     */
    @ExceptionHandler(BadCredentialsException.class)
    protected ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        log.error("handleBadCredentialsException", e);
        final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_CREDENTIALS);
        return ResponseEntity.status(ErrorCode.INVALID_CREDENTIALS.getStatus()).body(response);
    }

    /**
     * 인증된 사용자가 특정 리소스에 접근할 권한이 없을 때 발생하는 예외를 처리합니다.
     * (예: 일반 사용자가 관리자 API를 호출, 팀장이 아닌 사용자가 멤버 관리 시도)
     * HTTP 403 Forbidden 상태와 함께 접근 거부 메시지를 응답합니다.
     *
     * @param e AccessDeniedException 객체
     * @return ErrorResponse 객체를 담은 ResponseEntity
     */
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.error("handleAccessDeniedException", e);
        final ErrorResponse response = ErrorResponse.of(ErrorCode.ACCESS_DENIED);
        return ResponseEntity.status(ErrorCode.ACCESS_DENIED.getStatus()).body(response);
    }

    /**
     * DB에서 특정 엔티티(User, Group 등)를 찾지 못했을 때 발생하는 커스텀 예외를 처리합니다.
     * HTTP 404 Not Found 상태와 함께 해당 리소스 없음 메시지를 응답합니다.
     *
     * @param e EntityNotFoundException 객체
     * @return ErrorResponse 객체를 담은 ResponseEntity
     */
    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException e) {
        log.error("handleEntityNotFoundException: {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.of(e.getErrorCode());
        return new ResponseEntity<>(response, e.getErrorCode().getStatus());
    }

    /**
     * 서비스의 비즈니스 로직 상 제약 조건에 위배될 때 발생하는 커스텀 예외를 처리합니다.
     * (예: 중복된 아이디/닉네임으로 가입 시도, 최대 인원수 초과 등)
     * ErrorCode에 정의된 상태 코드와 메시지를 응답합니다.
     *
     * @param e BusinessLogicException 객체
     * @return ErrorResponse 객체를 담은 ResponseEntity
     */
    @ExceptionHandler(BusinessLogicException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessLogicException(BusinessLogicException e) {
        log.error("handleBusinessLogicException: {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.of(e.getErrorCode());
        return new ResponseEntity<>(response, e.getErrorCode().getStatus());
    }

    /**
     * 위에서 정의한 구체적인 커스텀 예외(EntityNotFound, BusinessLogic) 외의
     * 다른 CustomException들을 처리하기 위한 핸들러입니다.
     *
     * @param e CustomException 객체
     * @return ErrorResponse 객체를 담은 ResponseEntity
     */
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("handleCustomException: {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.of(e.getErrorCode());
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(response);
    }

    /**
     * 잘못된 인수나 상태로 인해 발생하는 예외를 처리합니다.
     * 주로 커스텀 예외로 미처 변환하지 못한 비즈니스 로직 오류를 처리하기 위한 대비책입니다.
     * HTTP 400 Bad Request 상태와 예외 메시지를 직접 응답합니다.
     *
     * @param e IllegalArgumentException 객체
     * @return ErrorResponse 객체를 담은 ResponseEntity
     */
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("handleIllegalArgumentException", e);
        final ErrorResponse response = ErrorResponse.builder()
                .errorCode(ErrorCode.BAD_REQUEST.getCode())
                .message(e.getMessage())
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 위에서 정의되지 않은 모든 예외를 최종적으로 처리합니다. (Catch-all)
     * 예상치 못한 서버 오류가 발생했을 때 사용됩니다.
     * HTTP 500 Internal Server Error 상태와 함께 서버 내부 오류 메시지를 응답합니다.
     *
     * @param e Exception 객체
     * @return ErrorResponse 객체를 담은 ResponseEntity
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("handleException", e);
        final ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.internalServerError().body(response);
    }
}
