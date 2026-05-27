package com.revtalent.leave_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "leave_balance",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_leave_balance",
                        columnNames = {"employee_id", "leave_type", "year"})
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_balance_employee"))
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false,
            columnDefinition = "ENUM('CASUAL','SICK','ANNUAL')")
    private LeaveRequest.LeaveType leaveType;

    @Column(nullable = false)
    private Integer year;

    @Builder.Default
    @Column(name = "total_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal totalDays = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "used_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal usedDays = BigDecimal.ZERO;
}
