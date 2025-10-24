package com.rookies4.MiniProject3.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.FieldError;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String errorCode;
    private String message;
    private String field;

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, FieldError fieldError) {
        return ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .message(fieldError.getDefaultMessage())
                .field(fieldError.getField())
                .build();
    }
}
