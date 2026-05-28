package com.revtalent.leave_service.controller;

import com.revtalent.leave_service.model.LeaveRequest;
import com.revtalent.leave_service.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor

public class CalendarController {

    private final LeaveRequestRepository leaveRequestRepository;

    @GetMapping("/events")
    public ResponseEntity<List<LeaveRequest>> getAllApprovedLeaves() {
        return ResponseEntity.ok(leaveRequestRepository.findByStatus(LeaveRequest.Status.APPROVED));
    }

    @GetMapping("/events/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequest>> getApprovedLeavesForEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveRequestRepository.findByEmployee_IdAndStatus(employeeId, LeaveRequest.Status.APPROVED));
    }
}
