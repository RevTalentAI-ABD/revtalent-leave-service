package com.revtalent.leave_service.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class CalendarEventDTO {
    private Long id;
    private String title;
    private LocalDate start;
    private LocalDate end;
    private Long employeeId;
    private String employeeName;
    private String leaveType;
    private String status;
}