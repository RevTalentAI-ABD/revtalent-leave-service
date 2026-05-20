package com.revtalent.leave_service.service;

import com.revtalent.leave_service.dto.LeaveApplyDTO;
import com.revtalent.leave_service.dto.LeaveAppliedEvent;
import com.revtalent.leave_service.dto.LeaveBalanceDTO;
import com.revtalent.leave_service.dto.LeaveHistoryDTO;
import com.revtalent.leave_service.dto.LeaveRequestDTO;
import com.revtalent.leave_service.dto.LeaveResponse;
import com.revtalent.leave_service.model.Employee;
import com.revtalent.leave_service.model.LeaveBalance;
import com.revtalent.leave_service.model.LeaveRequest;
import com.revtalent.leave_service.repository.EmployeeRepository;
import com.revtalent.leave_service.repository.LeaveBalanceRepository;
import com.revtalent.leave_service.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveEventPublisher leaveEventPublisher;

    // ── Helper ───────────────────────────────────────────────────────────────

    private LeaveRequest fetchLeaveEntity(Long leaveId) {
        return leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found: " + leaveId));
    }

    // ── Mapper ───────────────────────────────────────────────────────────────

    private LeaveRequestDTO toDTO(LeaveRequest leave) {
        return LeaveRequestDTO.builder()
                .id(leave.getId())
                .employeeName(leave.getEmployee() != null && leave.getEmployee().getUser() != null
                        ? leave.getEmployee().getUser().getUsername()
                        : "N/A")
                .leaveType(leave.getLeaveType().name())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .totalDays(leave.getTotalDays())
                .status(leave.getStatus().name())
                .reason(leave.getReason())
                .rejectionReason(leave.getRejectionReason())
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

    // ── Balance ──────────────────────────────────────────────────────────────

    public List<LeaveBalanceDTO> getLeaveBalance(Long empId) {
        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployee_Id(empId);
        return balances.stream()
                .map(b -> new LeaveBalanceDTO(
                        b.getLeaveType().name(),
                        b.getUsedDays().intValue(),
                        b.getTotalDays().intValue()
                ))
                .collect(Collectors.toList());
    }

    // ── Apply ────────────────────────────────────────────────────────────────

    public LeaveHistoryDTO applyLeave(LeaveApplyDTO dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found: " + dto.getEmployeeId()));

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setLeaveType(LeaveRequest.LeaveType.valueOf(dto.getLeaveType().toUpperCase()));
        leave.setStartDate(dto.getFromDate());
        leave.setEndDate(dto.getToDate());
        leave.setReason(dto.getReason());

        long days = ChronoUnit.DAYS.between(dto.getFromDate(), dto.getToDate()) + 1;
        leave.setTotalDays(BigDecimal.valueOf(days));
        leave.setStatus(LeaveRequest.Status.APPLIED);

        LeaveRequest saved = leaveRepository.save(leave);

        //  PUBLISH EVENT TO RABBITMQ
        LeaveAppliedEvent event = LeaveAppliedEvent.builder()
                .leaveId(saved.getId())
                .employeeId(saved.getEmployee().getId())
                .employeeName(saved.getEmployee().getUser().getName())
                .leaveType(saved.getLeaveType().name())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .reason(saved.getReason())
                .status(saved.getStatus().name())
                .managerId(saved.getEmployee().getManager() != null ?
                        saved.getEmployee().getManager().getId() : null)
                .build();

        leaveEventPublisher.publishLeaveApplied(event);

        return LeaveHistoryDTO.from(saved);
    }

    public LeaveResponse apply(LeaveRequestDTO req) {
        Employee emp = employeeRepository.findById(req.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(emp);
        leave.setLeaveType(LeaveRequest.LeaveType.valueOf(req.getLeaveType()));
        leave.setStartDate(req.getStartDate());
        leave.setEndDate(req.getEndDate());
        leave.setReason(req.getReason());
        leave.setStatus(LeaveRequest.Status.APPLIED);

        LeaveRequest saved = leaveRepository.save(leave);

        //  PUBLISH EVENT TO RABBITMQ
        LeaveAppliedEvent event = LeaveAppliedEvent.builder()
                .leaveId(saved.getId())
                .employeeId(saved.getEmployee().getId())
                .employeeName(saved.getEmployee().getUser() != null ?
                        saved.getEmployee().getUser().getName() : "Unknown")
                .leaveType(saved.getLeaveType().name())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .reason(saved.getReason())
                .status(saved.getStatus().name())
                .managerId(saved.getEmployee().getManager() != null ?
                        saved.getEmployee().getManager().getId() : null)
                .build();

        leaveEventPublisher.publishLeaveApplied(event);

        return mapToDTO(saved);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    public List<LeaveHistoryDTO> getLeaveHistory(Long empId) {
        return leaveRepository.findByEmployee_Id(empId)
                .stream()
                .map(LeaveHistoryDTO::from)
                .collect(Collectors.toList());
    }

    public LeaveHistoryDTO getLeaveById(Long leaveId) {
        return LeaveHistoryDTO.from(fetchLeaveEntity(leaveId));
    }

    public List<LeaveRequestDTO> getAllLeaves() {
        return leaveRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<LeaveRequestDTO> getPendingLeaves() {
        return leaveRepository.findByStatus(LeaveRequest.Status.APPLIED).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<LeaveResponse> getAll() {
        return leaveRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<LeaveRequestDTO> getPendingLeavesForManager(String username) {
        Employee manager = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        return leaveRepository.findByEmployee_Manager_IdAndStatus(manager.getId(), LeaveRequest.Status.APPLIED)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<LeaveRequestDTO> getAllLeavesForManager(String username) {
        Employee manager = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        return leaveRepository.findByEmployee_Manager_Id(manager.getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    public void approveLeave(Long id) {
        LeaveRequest leave = fetchLeaveEntity(id);
        if (leave.getStatus() != LeaveRequest.Status.APPLIED) {
            throw new RuntimeException("Only pending leaves can be approved");
        }
        leave.setStatus(LeaveRequest.Status.APPROVED);
        leave.setActionedAt(LocalDateTime.now());
        leaveRepository.save(leave);

        leaveBalanceRepository.findByEmployee_IdAndLeaveTypeAndYear(
                leave.getEmployee().getId(),
                leave.getLeaveType(),
                leave.getStartDate().getYear()
        ).ifPresent(balance -> {
            balance.setUsedDays(balance.getUsedDays().add(leave.getTotalDays()));
            leaveBalanceRepository.save(balance);
        });

        //  PUBLISH STATUS UPDATE EVENT
        LeaveAppliedEvent event = LeaveAppliedEvent.builder()
                .leaveId(leave.getId())
                .employeeId(leave.getEmployee().getId())
                .employeeName(leave.getEmployee().getUser() != null ?
                        leave.getEmployee().getUser().getName() : "Unknown")
                .leaveType(leave.getLeaveType().name())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .status(leave.getStatus().name())
                .managerId(leave.getEmployee().getManager() != null ?
                        leave.getEmployee().getManager().getId() : null)
                .build();

        leaveEventPublisher.publishLeaveStatusUpdated(event);
    }

    public LeaveResponse approve(Long id) {
        LeaveRequest leave = fetchLeaveEntity(id);
        leave.setStatus(LeaveRequest.Status.APPROVED);
        leave.setActionedAt(LocalDateTime.now());

        leaveBalanceRepository.findByEmployee_IdAndLeaveTypeAndYear(
                leave.getEmployee().getId(),
                leave.getLeaveType(),
                leave.getStartDate().getYear()
        ).ifPresent(balance -> {
            balance.setUsedDays(balance.getUsedDays().add(leave.getTotalDays()));
            leaveBalanceRepository.save(balance);
        });

        LeaveRequest saved = leaveRepository.save(leave);

        // PUBLISH STATUS UPDATE EVENT
        LeaveAppliedEvent event = LeaveAppliedEvent.builder()
                .leaveId(saved.getId())
                .employeeId(saved.getEmployee().getId())
                .employeeName(saved.getEmployee().getUser() != null ?
                        saved.getEmployee().getUser().getName() : "Unknown")
                .leaveType(saved.getLeaveType().name())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .status(saved.getStatus().name())
                .managerId(saved.getEmployee().getManager() != null ?
                        saved.getEmployee().getManager().getId() : null)
                .build();

        leaveEventPublisher.publishLeaveStatusUpdated(event);

        return mapToDTO(saved);
    }

    public void rejectLeave(Long id, String rejectionReason) {
        LeaveRequest leave = fetchLeaveEntity(id);
        if (leave.getStatus() != LeaveRequest.Status.APPLIED) {
            throw new RuntimeException("Only pending leaves can be rejected");
        }
        leave.setStatus(LeaveRequest.Status.REJECTED);
        leave.setRejectionReason(rejectionReason);
        leave.setActionedAt(LocalDateTime.now());
        leaveRepository.save(leave);

        //  PUBLISH STATUS UPDATE EVENT
        LeaveAppliedEvent event = LeaveAppliedEvent.builder()
                .leaveId(leave.getId())
                .employeeId(leave.getEmployee().getId())
                .employeeName(leave.getEmployee().getUser() != null ?
                        leave.getEmployee().getUser().getName() : "Unknown")
                .leaveType(leave.getLeaveType().name())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .status(leave.getStatus().name())
                .managerId(leave.getEmployee().getManager() != null ?
                        leave.getEmployee().getManager().getId() : null)
                .build();

        leaveEventPublisher.publishLeaveStatusUpdated(event);
    }

    public LeaveResponse reject(Long id) {
        LeaveRequest leave = fetchLeaveEntity(id);
        leave.setStatus(LeaveRequest.Status.REJECTED);
        leave.setActionedAt(LocalDateTime.now());

        LeaveRequest saved = leaveRepository.save(leave);

        //  PUBLISH STATUS UPDATE EVENT
        LeaveAppliedEvent event = LeaveAppliedEvent.builder()
                .leaveId(saved.getId())
                .employeeId(saved.getEmployee().getId())
                .employeeName(saved.getEmployee().getUser() != null ?
                        saved.getEmployee().getUser().getName() : "Unknown")
                .leaveType(saved.getLeaveType().name())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .status(saved.getStatus().name())
                .build();

        leaveEventPublisher.publishLeaveStatusUpdated(event);

        return mapToDTO(saved);
    }

    public void cancelLeave(Long leaveId) {
        LeaveRequest leave = fetchLeaveEntity(leaveId);
        if (leave.getStatus() == LeaveRequest.Status.CANCELLED) {
            throw new RuntimeException("Leave is already cancelled");
        }
        leave.setStatus(LeaveRequest.Status.CANCELLED);
        leaveRepository.save(leave);
    }

    public LeaveHistoryDTO updateLeaveStatus(Long leaveId, String status) {
        LeaveRequest leave = fetchLeaveEntity(leaveId);
        try {
            leave.setStatus(LeaveRequest.Status.valueOf(status.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status value: " + status);
        }
        return LeaveHistoryDTO.from(leaveRepository.save(leave));
    }
}