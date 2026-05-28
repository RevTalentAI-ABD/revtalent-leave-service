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
    private Status status = Status.APPLIED;

    private String reason;
    private String rejectionReason;

    private LocalDateTime appliedAt;
    private LocalDateTime actionedAt;

    @PrePersist
    protected void onCreate() { appliedAt = LocalDateTime.now(); }

    public enum LeaveType { ANNUAL, SICK, CASUAL, MATERNITY, PATERNITY, UNPAID }
    public enum Status { APPLIED, APPROVED, REJECTED, CANCELLED }
}