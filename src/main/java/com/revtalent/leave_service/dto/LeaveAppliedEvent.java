package com.revtalent.leave_service.dto;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDate;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class LeaveAppliedEvent implements Serializable {
    private Long leaveId;
    private Long employeeId;
    private String employeeName;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String status;
    private Long managerId;
}