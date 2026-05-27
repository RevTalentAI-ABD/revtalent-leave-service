package com.revtalent.leave_service.util;

import com.revtalent.leave_service.model.Employee;
import com.revtalent.leave_service.model.LeaveRequest;
import com.revtalent.leave_service.repository.EmployeeRepository;
import com.revtalent.leave_service.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public boolean isSelf(Authentication authentication, Long empId) {
        if (authentication == null || authentication.getName() == null) return false;
        return employeeRepository.findById(empId)
                .map(emp -> emp.getUser() != null
                        && authentication.getName().equals(emp.getUser().getUsername()))
                .orElse(false);
    }

    public boolean isLeaveOwner(Authentication authentication, Long leaveId) {
        if (authentication == null || authentication.getName() == null) return false;
        return leaveRequestRepository.findById(leaveId)
                .map(leave -> leave.getEmployee() != null
                        && leave.getEmployee().getUser() != null
                        && authentication.getName().equals(leave.getEmployee().getUser().getUsername()))
                .orElse(false);
    }
}
