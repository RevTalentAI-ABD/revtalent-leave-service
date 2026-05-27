package com.revtalent.leave_service.service;

import com.revtalent.leave_service.dto.*;
import com.revtalent.leave_service.exception.LeaveValidationException;
import com.revtalent.leave_service.exception.ResourceNotFoundException;
import com.revtalent.leave_service.exception.SelfApprovalException;
import com.revtalent.leave_service.model.Employee;
import com.revtalent.leave_service.model.LeaveBalance;
import com.revtalent.leave_service.model.LeaveRequest;
import com.revtalent.leave_service.repository.EmployeeRepository;
import com.revtalent.leave_service.repository.LeaveBalanceRepository;
import com.revtalent.leave_service.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveEventPublisher leaveEventPublisher;

    // ── Supported leave types ─────────────────────────────────────────────────
    private static final List<LeaveRequest.LeaveType> SUPPORTED_TYPES =
            Arrays.asList(LeaveRequest.LeaveType.CASUAL,
                          LeaveRequest.LeaveType.SICK,
                          LeaveRequest.LeaveType.ANNUAL);

    // ── Default allocations ───────────────────────────────────────────────────

    /**
     * Returns the annual default allocation for each leave type.
     *   CASUAL : 5 days
     *   SICK   : 10 days
     *   ANNUAL : 15 days
     */
    private BigDecimal getDefaultDays(LeaveRequest.LeaveType type) {
        switch (type) {
            case CASUAL: return BigDecimal.valueOf(5);
            case SICK:   return BigDecimal.valueOf(10);
            case ANNUAL: return BigDecimal.valueOf(15);
            default:     return BigDecimal.ZERO;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LeaveRequest fetchLeaveEntity(Long leaveId) {
        return leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave", leaveId));
    }

    /**
     * Parses and validates the requested leave type against the 3 allowed types.
     */
    private LeaveRequest.LeaveType parseLeaveType(String leaveTypeStr) {
        try {
            LeaveRequest.LeaveType type =
                    LeaveRequest.LeaveType.valueOf(leaveTypeStr.trim().toUpperCase());
            if (!SUPPORTED_TYPES.contains(type)) {
                throw new LeaveValidationException(
                        "Invalid leave type '" + leaveTypeStr +
                        "'. Supported types: CASUAL, SICK, ANNUAL.");
            }
            return type;
        } catch (IllegalArgumentException ex) {
            throw new LeaveValidationException(
                    "Invalid leave type '" + leaveTypeStr +
                    "'. Supported types: CASUAL, SICK, ANNUAL.");
        }
    }

    /**
     * Validates date range, available balance, and overlap against existing
     * APPLIED or APPROVED leaves for the same employee.
     */
    private void validateDatesAndBalance(Employee employee,
                                         LocalDate startDate,
                                         LocalDate endDate,
                                         LeaveRequest.LeaveType leaveType) {
        if (startDate.isAfter(endDate)) {
            throw new LeaveValidationException("startDate cannot be after endDate.");
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        LeaveBalance balance = leaveBalanceRepository
                .findByEmployee_IdAndLeaveTypeAndYear(
                        employee.getId(), leaveType, startDate.getYear())
                .orElseGet(() -> {
                    LeaveBalance newBalance = LeaveBalance.builder()
                            .employee(employee)
                            .leaveType(leaveType)
                            .year(startDate.getYear())
                            .totalDays(getDefaultDays(leaveType))
                            .usedDays(BigDecimal.ZERO)
                            .build();
                    return leaveBalanceRepository.save(newBalance);
                });

        BigDecimal remaining = balance.getTotalDays().subtract(balance.getUsedDays());
        if (remaining.compareTo(BigDecimal.valueOf(days)) < 0) {
            throw new LeaveValidationException(
                    "Insufficient leave balance. You have " + remaining
                            + " day(s) remaining but requested " + days + " day(s).");
        }

        boolean hasOverlap = leaveRepository
                .existsOverlappingLeave(employee.getId(), startDate, endDate);
        if (hasOverlap) {
            throw new LeaveValidationException(
                    "Your requested dates overlap with an existing leave application. " +
                    "Please choose different dates or cancel the conflicting request first.");
        }
    }

    /**
     * Resolves the applicant role from the Employee's linked User.
     * Only EMPLOYEE and MANAGER are valid applicant roles.
     */
    private String resolveApplicantRole(Employee employee) {
        if (employee.getUser() == null || employee.getUser().getRole() == null)
            return "EMPLOYEE";
        String role = employee.getUser().getRole().name();
        // HR_ADMIN is treated as EMPLOYEE for leave-application purposes
        // if you want HR to apply as HR; adjust here if needed
        return role;
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private LeaveRequestDTO toDTO(LeaveRequest leave) {
        return LeaveRequestDTO.builder()
                .id(leave.getId())
                .employeeId(leave.getEmployee() != null ? leave.getEmployee().getId() : null)
                .employeeName(leave.getEmployee() != null && leave.getEmployee().getUser() != null
                        ? leave.getEmployee().getUser().getName()
                        : "N/A")
                .leaveType(leave.getLeaveType().name())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .totalDays(leave.getTotalDays())
                .status(leave.getStatus().name())
                .reason(leave.getReason())
                .rejectionReason(leave.getRejectionReason())
                .approveComment(leave.getApproveComment())
                .applicantRole(leave.getApplicantRole())
                .build();
    }

    private LeaveResponse mapToDTO(LeaveRequest l) {
        return LeaveResponse.builder()
                .id(l.getId())
                .leaveType(l.getLeaveType().name())
                .status(l.getStatus().name())
                .reason(l.getReason())
                .startDate(l.getStartDate())
                .endDate(l.getEndDate())
                .build();
    }

    private void publishEvent(LeaveRequest leave) {
        LeaveAppliedEvent event = LeaveAppliedEvent.builder()
                .leaveId(leave.getId())
                .employeeId(leave.getEmployee().getId())
                .employeeName(leave.getEmployee().getUser() != null
                        ? leave.getEmployee().getUser().getName() : "Unknown")
                .leaveType(leave.getLeaveType().name())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .reason(leave.getReason())
                .status(leave.getStatus().name())
                .managerId(leave.getEmployee().getManager() != null
                        ? leave.getEmployee().getManager().getId() : null)
                .build();
        leaveEventPublisher.publishLeaveApplied(event);
    }

    private void publishStatusEvent(LeaveRequest leave) {
        LeaveAppliedEvent event = LeaveAppliedEvent.builder()
                .leaveId(leave.getId())
                .employeeId(leave.getEmployee().getId())
                .employeeName(leave.getEmployee().getUser() != null
                        ? leave.getEmployee().getUser().getName() : "Unknown")
                .leaveType(leave.getLeaveType().name())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .status(leave.getStatus().name())
                .managerId(leave.getEmployee().getManager() != null
                        ? leave.getEmployee().getManager().getId() : null)
                .build();
        leaveEventPublisher.publishLeaveStatusUpdated(event);
    }

    // ── Balance ───────────────────────────────────────────────────────────────

    /**
     * Returns the leave balance for an employee.
     * Initialises missing balances with the default allocation for the current year.
     * Only CASUAL, SICK, and ANNUAL balances are created/returned.
     */
    public List<LeaveBalanceDTO> getLeaveBalance(Long empId) {
        Employee employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", empId));
        int year = LocalDate.now().getYear();

        for (LeaveRequest.LeaveType type : SUPPORTED_TYPES) {
            leaveBalanceRepository.findByEmployee_IdAndLeaveTypeAndYear(empId, type, year)
                .orElseGet(() -> {
                    LeaveBalance newBalance = LeaveBalance.builder()
                            .employee(employee)
                            .leaveType(type)
                            .year(year)
                            .totalDays(getDefaultDays(type))
                            .usedDays(BigDecimal.ZERO)
                            .build();
                    return leaveBalanceRepository.save(newBalance);
                });
        }

        return leaveBalanceRepository.findByEmployee_Id(empId).stream()
                .map(b -> new LeaveBalanceDTO(
                        b.getLeaveType().name(),
                        b.getUsedDays().intValue(),
                        b.getTotalDays().intValue()))
                .collect(Collectors.toList());
    }

    // ── Apply ─────────────────────────────────────────────────────────────────

    /**
     * Applies a leave request.
     * Both EMPLOYEE and MANAGER roles may apply via this endpoint.
     * The applicantRole stored on the request determines who approves it:
     *   applicantRole = EMPLOYEE  →  Manager approves
     *   applicantRole = MANAGER   →  HR_ADMIN approves
     */
    @Transactional
    public LeaveHistoryDTO applyLeave(LeaveApplyDTO dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee", dto.getEmployeeId()));

        LeaveRequest.LeaveType leaveType = parseLeaveType(dto.getLeaveType());
        validateDatesAndBalance(employee, dto.getFromDate(), dto.getToDate(), leaveType);

        long days = ChronoUnit.DAYS.between(dto.getFromDate(), dto.getToDate()) + 1;

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setLeaveType(leaveType);
        leave.setStartDate(dto.getFromDate());
        leave.setEndDate(dto.getToDate());
        leave.setReason(dto.getReason());
        leave.setTotalDays(BigDecimal.valueOf(days));
        String role = resolveApplicantRole(employee);
        leave.setApplicantRole(role);
        
        if ("MANAGER".equals(role)) {
            leave.setStatus(LeaveRequest.Status.PENDING_HR);
        } else {
            leave.setStatus(LeaveRequest.Status.PENDING_MANAGER);
        }

        LeaveRequest saved = leaveRepository.save(leave);
        publishEvent(saved);
        return LeaveHistoryDTO.from(saved);
    }

    /**
     * Alternative apply entry point (used by HR module).
     */
    @Transactional
    public LeaveResponse apply(LeaveRequestDTO req) {
        Employee emp = employeeRepository.findById(req.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee", req.getEmployeeId()));

        LeaveRequest.LeaveType leaveType = parseLeaveType(req.getLeaveType());
        validateDatesAndBalance(emp, req.getStartDate(), req.getEndDate(), leaveType);

        long days = ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1;

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(emp);
        leave.setLeaveType(leaveType);
        leave.setStartDate(req.getStartDate());
        leave.setEndDate(req.getEndDate());
        leave.setReason(req.getReason());
        leave.setTotalDays(BigDecimal.valueOf(days));
        String role = resolveApplicantRole(emp);
        leave.setApplicantRole(role);

        if ("MANAGER".equals(role)) {
            leave.setStatus(LeaveRequest.Status.PENDING_HR);
        } else {
            leave.setStatus(LeaveRequest.Status.PENDING_MANAGER);
        }

        LeaveRequest saved = leaveRepository.save(leave);
        publishEvent(saved);
        return mapToDTO(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<LeaveHistoryDTO> getLeaveHistory(Long empId) {
        return leaveRepository.findByEmployee_Id(empId).stream()
                .map(LeaveHistoryDTO::from)
                .collect(Collectors.toList());
    }

    public LeaveHistoryDTO getLeaveById(Long leaveId) {
        return LeaveHistoryDTO.from(fetchLeaveEntity(leaveId));
    }

    /**
     * HR sees all leaves submitted by MANAGERs (pending + historical).
     */
    public List<LeaveRequestDTO> getAllLeaves() {
        return leaveRepository.findByApplicantRole("MANAGER").stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * HR sees pending (PENDING_HR) leaves.
     */
    public List<LeaveRequestDTO> getPendingLeaves() {
        return leaveRepository
                .findByStatus(LeaveRequest.Status.PENDING_HR)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Manager sees all EMPLOYEE leaves from their department (pending + historical).
     */
    public List<LeaveRequestDTO> getAllLeavesForManager(String username) {
        Employee manager = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Manager not found for username: " + username));
        return leaveRepository
                .findByEmployee_Manager_IdAndApplicantRole(manager.getId(), "EMPLOYEE")
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Manager sees only PENDING_MANAGER EMPLOYEE leaves from their department.
     */
    public List<LeaveRequestDTO> getPendingLeavesForManager(String username) {
        Employee manager = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Manager not found for username: " + username));
        return leaveRepository.findByEmployee_Manager_IdAndApplicantRoleAndStatus(
                        manager.getId(), "EMPLOYEE", LeaveRequest.Status.PENDING_MANAGER)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Any role can view their own leave history by username.
     */
    public List<LeaveRequestDTO> getAllLeavesForEmployee(String username) {
        Employee employee = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found for username: " + username));
        return leaveRepository.findByEmployee_Id(employee.getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── Overlap check ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LeaveOverlapResponseDTO checkOverlap(String username,
                                                LocalDate startDate,
                                                LocalDate endDate) {
        Employee employee = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found for username: " + username));

        List<LeaveRequest> conflicts =
                leaveRepository.findOverlappingLeaves(employee.getId(), startDate, endDate);

        List<LeaveRequestDTO> conflictDTOs = conflicts.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return LeaveOverlapResponseDTO.builder()
                .hasOverlap(!conflictDTOs.isEmpty())
                .conflictingLeaves(conflictDTOs)
                .build();
    }

    // ── Clock-in guard ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public boolean isOnApprovedLeaveToday(Long employeeId) {
        LocalDate today = LocalDate.now();
        return leaveRepository.existsApprovedLeaveOnDate(employeeId, today);
    }

    // ── Approve ───────────────────────────────────────────────────────────────

    @Transactional
    public void approveLeaveByManager(Long id, String username, String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            throw new LeaveValidationException("Approve comment is mandatory and must not be blank.");
        }
        LeaveRequest leave = fetchLeaveEntity(id);

        Employee manager = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found for username: " + username));

        if (leave.getEmployee().getManager() == null || !leave.getEmployee().getManager().getId().equals(manager.getId())) {
            throw new LeaveValidationException("You can only approve leaves from employees in your own department.");
        }

        if (leave.getStatus() != LeaveRequest.Status.PENDING_MANAGER) {
            throw new LeaveValidationException("Only PENDING_MANAGER leaves can be approved by Manager.");
        }

        leave.setStatus(LeaveRequest.Status.APPROVED);
        leave.setApproveComment(comment);
        leave.setManagerId(manager.getId());
        leave.setManagerActionAt(LocalDateTime.now());
        leaveRepository.save(leave);

        // Deduct from leave balance
        leaveBalanceRepository.findByEmployee_IdAndLeaveTypeAndYear(
                leave.getEmployee().getId(),
                leave.getLeaveType(),
                leave.getStartDate().getYear()
        ).ifPresent(balance -> {
            balance.setUsedDays(balance.getUsedDays().add(leave.getTotalDays()));
            leaveBalanceRepository.save(balance);
        });

        publishStatusEvent(leave);
    }

    @Transactional
    public void approveLeaveByHR(Long id, String username, String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            throw new LeaveValidationException("Approve comment is mandatory and must not be blank.");
        }
        LeaveRequest leave = fetchLeaveEntity(id);

        if (leave.getStatus() != LeaveRequest.Status.PENDING_HR) {
            throw new LeaveValidationException("Only PENDING_HR leaves can be approved by HR.");
        }

        Employee hr = employeeRepository.findByUser_Username(username).orElse(null);

        leave.setStatus(LeaveRequest.Status.APPROVED);
        leave.setApproveComment(comment);
        if (hr != null) {
            leave.setHrId(hr.getId());
        }
        leave.setHrActionAt(LocalDateTime.now());
        leaveRepository.save(leave);

        // Deduct from leave balance
        leaveBalanceRepository.findByEmployee_IdAndLeaveTypeAndYear(
                leave.getEmployee().getId(),
                leave.getLeaveType(),
                leave.getStartDate().getYear()
        ).ifPresent(balance -> {
            balance.setUsedDays(balance.getUsedDays().add(leave.getTotalDays()));
            leaveBalanceRepository.save(balance);
        });

        publishStatusEvent(leave);
    }

    // ── Reject ────────────────────────────────────────────────────────────────

    @Transactional
    public void rejectLeave(Long id, String username, String rejectionReason, boolean isManager, boolean isHr) {
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new LeaveValidationException("Rejection reason is mandatory and must not be blank.");
        }

        LeaveRequest leave = fetchLeaveEntity(id);

        if (isManager && !isHr) {
            Employee manager = employeeRepository.findByUser_Username(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found for username: " + username));
            if (leave.getEmployee().getManager() == null || !leave.getEmployee().getManager().getId().equals(manager.getId())) {
                throw new LeaveValidationException("You can only reject leaves from employees in your own department.");
            }
            if (leave.getStatus() != LeaveRequest.Status.PENDING_MANAGER) {
                throw new LeaveValidationException("Manager can only reject PENDING_MANAGER leaves.");
            }
            leave.setRejectedByRole("MANAGER");
        } else if (isHr) {
            if (leave.getStatus() != LeaveRequest.Status.PENDING_HR) {
                throw new LeaveValidationException("HR can only reject PENDING_HR leaves.");
            }
            leave.setRejectedByRole("HR_ADMIN");
        } else {
            throw new LeaveValidationException("Unauthorized to reject leave.");
        }

        leave.setStatus(LeaveRequest.Status.REJECTED);
        leave.setRejectionReason(rejectionReason);
        leave.setActionedAt(LocalDateTime.now());
        leaveRepository.save(leave);

        publishStatusEvent(leave);
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Transactional
    public void cancelLeave(Long leaveId) {
        LeaveRequest leave = fetchLeaveEntity(leaveId);
        if (leave.getStatus() == LeaveRequest.Status.CANCELLED) {
            throw new LeaveValidationException("Leave is already cancelled.");
        }
        if (leave.getStatus() == LeaveRequest.Status.APPROVED) {
            // Restore balance when cancelling an approved leave
            leaveBalanceRepository.findByEmployee_IdAndLeaveTypeAndYear(
                    leave.getEmployee().getId(),
                    leave.getLeaveType(),
                    leave.getStartDate().getYear()
            ).ifPresent(balance -> {
                balance.setUsedDays(balance.getUsedDays().subtract(leave.getTotalDays()));
                leaveBalanceRepository.save(balance);
            });
        }
        leave.setStatus(LeaveRequest.Status.CANCELLED);
        leaveRepository.save(leave);
    }

    // ── Admin status update ───────────────────────────────────────────────────

    public LeaveHistoryDTO updateLeaveStatus(Long leaveId, String status) {
        LeaveRequest leave = fetchLeaveEntity(leaveId);
        try {
            leave.setStatus(LeaveRequest.Status.valueOf(status.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new LeaveValidationException("Invalid status value: " + status);
        }
        return LeaveHistoryDTO.from(leaveRepository.save(leave));
    }

    // ── Calendar events ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CalendarEventDTO> getAllApprovedLeavesEvents() {
        return leaveRepository.findByStatus(LeaveRequest.Status.APPROVED)
                .stream()
                .map(leave -> CalendarEventDTO.builder()
                        .id(leave.getId())
                        .title(getEmployeeName(leave) + " - " + leave.getLeaveType().name())
                        .start(leave.getStartDate())
                        .end(leave.getEndDate())
                        .employeeId(leave.getEmployee() != null ? leave.getEmployee().getId() : null)
                        .employeeName(getEmployeeName(leave))
                        .leaveType(leave.getLeaveType().name())
                        .status(leave.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CalendarEventDTO> getApprovedLeavesForEmployeeEvents(Long employeeId) {
        return leaveRepository.findByEmployee_IdAndStatus(employeeId, LeaveRequest.Status.APPROVED)
                .stream()
                .map(leave -> CalendarEventDTO.builder()
                        .id(leave.getId())
                        .title(leave.getLeaveType().name() + " Leave")
                        .start(leave.getStartDate())
                        .end(leave.getEndDate())
                        .employeeId(employeeId)
                        .employeeName(getEmployeeName(leave))
                        .leaveType(leave.getLeaveType().name())
                        .status(leave.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }

    private String getEmployeeName(LeaveRequest leave) {
        if (leave.getEmployee() != null && leave.getEmployee().getUser() != null) {
            return leave.getEmployee().getUser().getName();
        }
        return "N/A";
    }
}
