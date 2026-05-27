package com.revtalent.leave_service.controller;

import com.revtalent.leave_service.dto.CalendarEventDTO;
import com.revtalent.leave_service.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final LeaveService leaveService;

    @GetMapping("/events")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CalendarEventDTO>> getAllApprovedLeaves() {
        return ResponseEntity.ok(leaveService.getAllApprovedLeavesEvents());
    }

    @GetMapping("/events/employee/{employeeId}")
    @PreAuthorize("hasRole('HR_ADMIN') or hasRole('MANAGER') or @securityService.isSelf(authentication, #employeeId)")
    public ResponseEntity<List<CalendarEventDTO>> getApprovedLeavesForEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveService.getApprovedLeavesForEmployeeEvents(employeeId));
    }
}
