package com.revtalent.leave_service.repository;

import com.revtalent.leave_service.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeCode(String employeeCode);
    Optional<Employee> findByUser_Username(String username);
    Optional<Employee> findByUser_Email(String email);
    Optional<Employee> findByUser_Id(Long userId);
    List<Employee> findByManager_Id(Long managerId);
    List<Employee> findByStatus(Employee.Status status);
    List<Employee> findByUser_NameContainingIgnoreCase(String name);
    long countByStatus(Employee.Status status);
    long countByManagerId(Long managerId);
}