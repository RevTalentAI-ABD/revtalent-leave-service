package com.revtalent.leave_service.exception;

/** Thrown when a user attempts to approve or reject their own leave request. */
public class SelfApprovalException extends RuntimeException {
    public SelfApprovalException(String message) {
        super(message);
    }
}
