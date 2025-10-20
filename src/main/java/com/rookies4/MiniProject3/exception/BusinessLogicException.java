package com.rookies4.MiniProject3.exception;

public class BusinessLogicException extends CustomException {
    public BusinessLogicException(ErrorCode errorCode) {
        super(errorCode);
    }
}