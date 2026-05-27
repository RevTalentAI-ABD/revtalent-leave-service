package com.revtalent.leave_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_request",
        indexes = {
                @Index(name = "idx_leave_emp_status", columnList = "employee_id, status"),
                @Index(name = "idx_leave_dates", columnList = "start_date, end_date")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Employee approvedBy;

    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;

    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal totalDays = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING_MANAGER;

    private String reason;
    private String rejectionReason;
    private String rejectedByRole; // MANAGER or HR_ADMIN
    private String approveComment;

    private Long managerId;
    private LocalDateTime managerActionAt;
    
    private Long hrId;
    private LocalDateTime hrActionAt;

    private LocalDateTime appliedAt;
    private LocalDateTime actionedAt;

    @PrePersist
    protected void onCreate() { appliedAt = LocalDateTime.now(); }

    @Column(name = "applicant_role")
    private String applicantRole; // "EMPLOYEE" or "MANAGER"

    /**
     * Only 3 leave types are supported.
     * - CASUAL  : 5 days/year  – for short personal errands
     * - SICK    : 10 days/year – for medical reasons
     * - ANNUAL  : 15 days/year – planned vacation
     *
     * Approval rules:
     *   EMPLOYEE leave → approved/rejected by MANAGER
     *   MANAGER  leave → approved/rejected by HR_ADMIN
     */
    public enum LeaveType { CASUAL, SICK, ANNUAL }

    public enum Status { PENDING_MANAGER, PENDING_HR, APPROVED, REJECTED, CANCELLED }
}
