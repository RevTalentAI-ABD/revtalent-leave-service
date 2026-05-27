package com.revtalent.leave_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestDTO {

    // ── Request fields (from HRModule) ───────────────────────────────────────
    private Long employeeId;        // used when applying leave

    // ── Response fields (from HEAD) ──────────────────────────────────────────
    private Long id;
    private String employeeName;

    // ── Common fields ────────────────────────────────────────────────────────
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;

    // ── Response-only fields ─────────────────────────────────────────────────
    private BigDecimal totalDays;
    private String status;
    private String rejectionReason;
    private String rejectedByRole;
    private String approveComment;
    private String applicantRole;
    
    private Long managerId;
    private Long hrId;
}