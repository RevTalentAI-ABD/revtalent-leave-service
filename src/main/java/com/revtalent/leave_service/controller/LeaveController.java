package com.revtalent.leave_service.controller;

import com.revtalent.leave_service.dto.LeaveApplyDTO;
import com.revtalent.leave_service.dto.LeaveHistoryDTO;
import com.revtalent.leave_service.dto.LeaveRequestDTO;
import com.revtalent.leave_service.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    // ── Employee endpoints ────────────────────────────────────────────────────

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

    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody LeaveApplyDTO dto) {
        return ResponseEntity.ok(leaveService.applyLeave(dto));
    }

    @PostMapping("/apply/hr")
    public ResponseEntity<?> applyViaHR(@RequestBody LeaveRequestDTO req) {
        return ResponseEntity.ok(leaveService.apply(req));
    }

    @DeleteMapping("/{leaveId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long leaveId) {
        leaveService.cancelLeave(leaveId);
        return ResponseEntity.noContent().build();
    }

    // ── Manager / HR endpoints ────────────────────────────────────────────────


    @GetMapping
    public ResponseEntity<List<LeaveRequestDTO>> getAllLeaves() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        if (isManager) {
            return ResponseEntity.ok(leaveService.getAllLeavesForManager(auth.getName()));
        }
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<LeaveRequestDTO>> getPending() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        if (isManager) {
            return ResponseEntity.ok(leaveService.getPendingLeavesForManager(auth.getName()));
        }
        return ResponseEntity.ok(leaveService.getPendingLeaves());
    }
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id,
                                     @RequestParam(required = false) String comment) {
        leaveService.approveLeave(id);
        return ResponseEntity.ok(Map.of("message", "Leave approved successfully"));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        leaveService.rejectLeave(id, reason);
        return ResponseEntity.ok(Map.of("message", "Leave rejected successfully"));
    }

}