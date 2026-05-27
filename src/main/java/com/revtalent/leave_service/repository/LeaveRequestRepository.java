package com.revtalent.leave_service.repository;

import com.revtalent.leave_service.model.Employee;
import com.revtalent.leave_service.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployee_Id(Long empId);
    List<LeaveRequest> findByEmployee_IdAndStatus(Long empId, LeaveRequest.Status status); // only once
    List<LeaveRequest> findByEmployee(Employee employee);
    List<LeaveRequest> findByStatus(LeaveRequest.Status status);
    List<LeaveRequest> findByEmployee_Manager_Id(Long managerId);
    List<LeaveRequest> findByEmployee_Manager_IdAndStatus(Long managerId, LeaveRequest.Status status);
    int countByStatus(LeaveRequest.Status status);
    List<LeaveRequest> findByApplicantRole(String applicantRole);
    List<LeaveRequest> findByApplicantRoleAndStatus(String applicantRole, LeaveRequest.Status status);
    List<LeaveRequest> findByEmployee_Manager_IdAndApplicantRole(Long managerId, String applicantRole);
    List<LeaveRequest> findByEmployee_Manager_IdAndApplicantRoleAndStatus(
            Long managerId, String applicantRole, LeaveRequest.Status status);

    // ── Overlap queries ───────────────────────────────────────────────────────

    @Query("""
           SELECT COUNT(lr) > 0
           FROM LeaveRequest lr
           WHERE lr.employee.id = :empId
             AND lr.status IN (
                   com.revtalent.leave_service.model.LeaveRequest.Status.PENDING_MANAGER,
                   com.revtalent.leave_service.model.LeaveRequest.Status.PENDING_HR,
                   com.revtalent.leave_service.model.LeaveRequest.Status.APPROVED
                 )
             AND lr.startDate <= :endDate
             AND lr.endDate   >= :startDate
           """)
    boolean existsOverlappingLeave(
            @Param("empId")      Long empId,
            @Param("startDate")  LocalDate startDate,
            @Param("endDate")    LocalDate endDate);

    @Query("""
           SELECT lr
           FROM LeaveRequest lr
           WHERE lr.employee.id = :empId
             AND lr.status IN (
                   com.revtalent.leave_service.model.LeaveRequest.Status.PENDING_MANAGER,
                   com.revtalent.leave_service.model.LeaveRequest.Status.PENDING_HR,
                   com.revtalent.leave_service.model.LeaveRequest.Status.APPROVED
                 )
             AND lr.startDate <= :endDate
             AND lr.endDate   >= :startDate
           ORDER BY lr.startDate ASC
           """)
    List<LeaveRequest> findOverlappingLeaves(
            @Param("empId")      Long empId,
            @Param("startDate")  LocalDate startDate,
            @Param("endDate")    LocalDate endDate);

    @Query("""
           SELECT COUNT(lr) > 0
           FROM LeaveRequest lr
           WHERE lr.employee.id = :empId
             AND lr.status = com.revtalent.leave_service.model.LeaveRequest.Status.APPROVED
             AND lr.startDate <= :date
             AND lr.endDate   >= :date
           """)
    boolean existsApprovedLeaveOnDate(
            @Param("empId") Long empId,
            @Param("date")  LocalDate date);
}