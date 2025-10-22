package com.rookies4.MiniProject3.exception;

public class EntityNotFoundException extends CustomException {
    public EntityNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}