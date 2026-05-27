package com.revtalent.leave_service.controller;

import com.revtalent.leave_service.dto.LeaveApplyDTO;
import com.revtalent.leave_service.dto.LeaveHistoryDTO;
import com.revtalent.leave_service.dto.LeaveRequestDTO;
import com.revtalent.leave_service.exception.ResourceNotFoundException;
import com.revtalent.leave_service.model.Employee;
import com.revtalent.leave_service.repository.EmployeeRepository;
import com.revtalent.leave_service.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Leave REST controller.
 *
 * Supported leave types : CASUAL | SICK | ANNUAL
 *
 * Approval hierarchy:
 *   EMPLOYEE leave → approved / rejected by MANAGER
 *   MANAGER  leave → approved / rejected by HR_ADMIN
 */
@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final EmployeeRepository employeeRepository;

    // ── Employee & Manager – apply leave ─────────────────────────────────────

    /**
     * POST /api/leaves/apply
     * Both EMPLOYEE and MANAGER can call this endpoint.
     * The applicantRole is derived from the authenticated user's role,
     * so the correct approver is automatically determined.
     */
    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public ResponseEntity<?> apply(@RequestBody LeaveApplyDTO dto) {
        return ResponseEntity.ok(leaveService.applyLeave(dto));
    }

    /**
     * POST /api/leaves/apply/hr
     * HR-module shortcut to apply on behalf of anyone.
     */
    @PostMapping("/apply/hr")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<?> applyViaHR(@RequestBody LeaveRequestDTO req) {
        return ResponseEntity.ok(leaveService.apply(req));
    }

    // ── Employee – view own history & balance ─────────────────────────────────

    @GetMapping("/balance/{empId}")
    public ResponseEntity<?> getBalance(@PathVariable Long empId) {
        return ResponseEntity.ok(leaveService.getLeaveBalance(empId));
    }

    @GetMapping("/history/{empId}")
    public ResponseEntity<List<LeaveHistoryDTO>> getHistory(@PathVariable Long empId) {
        return ResponseEntity.ok(leaveService.getLeaveHistory(empId));
    }

    @GetMapping("/{leaveId}")
    public ResponseEntity<LeaveHistoryDTO> getLeave(@PathVariable Long leaveId) {
        return ResponseEntity.ok(leaveService.getLeaveById(leaveId));
    }

    @DeleteMapping("/{leaveId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long leaveId) {
        leaveService.cancelLeave(leaveId);
        return ResponseEntity.noContent().build();
    }

    // ── Manager / HR – view pending & all leaves ──────────────────────────────

    /**
     * GET /api/leaves
     * MANAGER → returns EMPLOYEE leaves from their department.
     * HR_ADMIN → returns all MANAGER leaves.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<List<LeaveRequestDTO>> getAllLeaves() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        if (isManager) {
            return ResponseEntity.ok(leaveService.getAllLeavesForManager(auth.getName()));
        }
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    @GetMapping("/pending/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<LeaveRequestDTO>> getPendingForManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(leaveService.getPendingLeavesForManager(auth.getName()));
    }

    @GetMapping("/pending/hr")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<List<LeaveRequestDTO>> getPendingForHR() {
        return ResponseEntity.ok(leaveService.getPendingLeaves());
    }

    @PutMapping("/{id}/approve/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> approveByManager(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Approved by Manager") String comment) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        leaveService.approveLeaveByManager(id, auth.getName(), comment);
        return ResponseEntity.ok("Leave approved by Manager");
    }

    @PutMapping("/{id}/approve/hr")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<?> approveByHR(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Approved by HR") String comment) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        leaveService.approveLeaveByHR(id, auth.getName(), comment);
        return ResponseEntity.ok("Leave approved by HR");
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isManager = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        boolean isHr = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HR_ADMIN"));
        leaveService.rejectLeave(id, auth.getName(), reason, isManager, isHr);
        return ResponseEntity.ok("Leave rejected successfully");
    }
    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequestDTO>> getMyLeaves() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(leaveService.getAllLeavesForEmployee(auth.getName()));
    }
    // GET /api/leaves/balance/my  — balance for the authenticated user only
    @GetMapping("/balance/my")
    public ResponseEntity<?> getMyBalance() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Employee emp = employeeRepository.findByUser_Username(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return ResponseEntity.ok(leaveService.getLeaveBalance(emp.getId()));
    }
}
