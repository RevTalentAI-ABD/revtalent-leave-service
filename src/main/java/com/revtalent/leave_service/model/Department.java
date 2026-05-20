package com.revtalent.leave_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "department",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_department_name", columnNames = "name")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_employee_id",
            foreignKey = @ForeignKey(name = "fk_department_head"))
    @JsonIgnoreProperties({"department", "manager", "documents", "leaveRequests",
            "leaveBalances", "attendanceRecords", "payrolls", "hibernateLazyInitializer"})
    private Employee head;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"department", "manager", "documents", "leaveRequests",
            "leaveBalances", "attendanceRecords", "payrolls", "hibernateLazyInitializer"})
    private List<Employee> employees;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}