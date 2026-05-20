package com.revtalent.leave_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "employee",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_employee_code", columnNames = "employee_code"),
                @UniqueConstraint(name = "uq_employee_user", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_employee_department", columnList = "department_id"),
                @Index(name = "idx_employee_manager", columnList = "manager_id"),
                @Index(name = "idx_employee_status", columnList = "status")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_employee_user"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "accountNonExpired",
            "accountNonLocked", "credentialsNonExpired", "enabled",
            "authorities", "password", "passwordHash", "employee"})
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @JsonIgnoreProperties({"employees", "head", "hibernateLazyInitializer"})
    private Department department;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    private String email;

    @Column(name = "employee_code", nullable = true)
    private String employeeCode;

    @Column(nullable = true)
    private String designation;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "joining_date", nullable = true)
    private LocalDate joiningDate;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    private String phone;
    private String address;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "ENUM('ACTIVE','INACTIVE','ON_LEAVE') DEFAULT 'ACTIVE'")
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY)
    private List<Employee> reportees;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = Status.ACTIVE;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (joiningDate == null) joiningDate = LocalDate.now();
        if (employeeCode == null) employeeCode = "EMP" + System.currentTimeMillis();
        if (designation == null) designation = "EMPLOYEE";
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public String getName() {
        return this.user != null ? this.user.getName() : null;
    }

    public enum Status { ACTIVE, INACTIVE, ON_LEAVE }
}