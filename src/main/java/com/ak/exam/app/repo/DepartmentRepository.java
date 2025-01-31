package com.ak.exam.app.repo;

import com.ak.exam.app.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByNameContainingIgnoreCase(String name);  // Method to search by name (case-insensitive)
    List<Department> findByLevel(Integer level);
}
