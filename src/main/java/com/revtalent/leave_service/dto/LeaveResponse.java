package com.revtalent.leave_service.dto;

import lombok.*;
import java.time.LocalDate;

@Data @Builder
public class LeaveResponse {
    private Long id;
    private String leaveType;
    private String status;
    private String reason;
    private LocalDate startDate;
    private LocalDate endDate;
}