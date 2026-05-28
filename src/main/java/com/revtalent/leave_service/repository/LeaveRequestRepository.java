package com.revtalent.leave_service.repository;

import com.revtalent.leave_service.model.LeaveRequest;
import com.revtalent.leave_service.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployee_Id(Long empId);
    List<LeaveRequest> findByEmployee_IdAndStatus(Long empId, LeaveRequest.Status status);
    List<LeaveRequest> findByEmployee(Employee employee);
    List<LeaveRequest> findByStatus(LeaveRequest.Status status);
    List<LeaveRequest> findByEmployee_Manager_Id(Long managerId);
    List<LeaveRequest> findByEmployee_Manager_IdAndStatus(Long managerId, LeaveRequest.Status status);
    int countByStatus(LeaveRequest.Status status);
}