package com.revtalent.leave_service.repository;

import com.revtalent.leave_service.model.LeaveBalance;
import com.revtalent.leave_service.model.LeaveRequest;
import com.revtalent.leave_service.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByEmployee_Id(Long empId);
    Optional<LeaveBalance> findByEmployee_IdAndLeaveType(Long empId, LeaveRequest.LeaveType leaveType);
    Optional<LeaveBalance> findByEmployee(Employee employee);
    Optional<LeaveBalance> findByEmployeeAndYear(Employee employee, Integer year);
    Optional<LeaveBalance> findByEmployee_IdAndLeaveTypeAndYear(Long empId, LeaveRequest.LeaveType leaveType, Integer year);
}