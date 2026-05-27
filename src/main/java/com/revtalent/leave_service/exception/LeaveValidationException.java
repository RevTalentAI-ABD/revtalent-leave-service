package com.revtalent.leave_service.exception;

/** Thrown for any business-rule violation during leave operations. */
public class LeaveValidationException extends RuntimeException {
    public LeaveValidationException(String message) {
        super(message);
    }
}
