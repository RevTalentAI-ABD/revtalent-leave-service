package com.revtalent.leave_service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LeaveRequestDTO {
    private Long employeeId;
    private Long id;
    private String employeeName;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private BigDecimal totalDays;
    private String status;
    private String rejectionReason;
}