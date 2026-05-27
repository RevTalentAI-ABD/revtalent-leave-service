package com.revtalent.leave_service.dto;

import com.revtalent.leave_service.model.LeaveRequest;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LeaveHistoryDTO {
    private Long id;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDays;
    private String status;
    private String reason;
    private String rejectionReason;
    private String rejectedByRole;
    private LocalDateTime appliedAt;
    private LocalDateTime actionedAt;
    private String approvedByName; // Only the name, nothing else


    public static LeaveHistoryDTO from(LeaveRequest leave) {
        LeaveHistoryDTO dto = new LeaveHistoryDTO();
        dto.setId(leave.getId());
        dto.setLeaveType(leave.getLeaveType().name());
        dto.setStartDate(leave.getStartDate());
        dto.setEndDate(leave.getEndDate());

        // ✅ Calculate days if totalDays is null
        if (leave.getTotalDays() != null && leave.getTotalDays().compareTo(BigDecimal.ZERO) > 0) {
            dto.setTotalDays(leave.getTotalDays());
        } else if (leave.getStartDate() != null && leave.getEndDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
            dto.setTotalDays(BigDecimal.valueOf(days));
        } else {
            dto.setTotalDays(BigDecimal.ZERO);
        }

        dto.setStatus(leave.getStatus().name());
        dto.setReason(leave.getReason());
        dto.setRejectionReason(leave.getRejectionReason());
        dto.setRejectedByRole(leave.getRejectedByRole());
        dto.setAppliedAt(leave.getAppliedAt());
        dto.setActionedAt(leave.getActionedAt());

        if (leave.getApprovedBy() != null && leave.getApprovedBy().getUser() != null) {
            dto.setApprovedByName(leave.getApprovedBy().getUser().getName());
        }

        return dto;
    }
}