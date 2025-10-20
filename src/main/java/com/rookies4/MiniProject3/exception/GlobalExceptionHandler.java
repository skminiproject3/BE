package com.rookies4.MiniProject3.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 모든 @RestController 에서 발생하는 예외를 전역적으로 처리하는 클래스
@RestControllerAdvice
public class GlobalExceptionHandler {

    // IllegalArgumentException 예외 발생 시 호출되는 핸들러
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}