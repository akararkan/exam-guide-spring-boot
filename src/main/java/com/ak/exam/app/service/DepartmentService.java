package com.ak.exam.app.service;

import com.ak.exam.app.model.Department;
import com.ak.exam.app.repo.DepartmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    // Add a new department
    public ResponseEntity<Department> addDepartment(Department department) {
        Department newDepartment = Department.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .level(department.getLevel())
                .createdAt(new Date())
                .updatedAt(null)
                .build();
        departmentRepository.save(newDepartment);
        return new ResponseEntity<>(newDepartment, HttpStatus.CREATED);
    }

    // Get all departments
    public ResponseEntity<List<Department>> getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        return new ResponseEntity<>(departments, HttpStatus.OK);
    }

    // Get department by ID
    public ResponseEntity<Department> getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .map(department -> new ResponseEntity<>(department, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Update department by ID
    public ResponseEntity<Department> updateDepartmentById(Long id, Department department) {
        return departmentRepository.findById(id)
                .map(existingDepartment -> {
                    existingDepartment.setName(department.getName());
                    existingDepartment.setDescription(department.getDescription());
                    existingDepartment.setLevel(department.getLevel());
                    existingDepartment.setUpdatedAt(new Date());
                    departmentRepository.save(existingDepartment);
                    return new ResponseEntity<>(existingDepartment, HttpStatus.OK);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Search department by name
    public ResponseEntity<List<Department>> searchDepartmentByName(String name) {
        List<Department> departments = departmentRepository.findByNameContainingIgnoreCase(name);
        return new ResponseEntity<>(departments, HttpStatus.OK);
    }

    // Delete department by ID
    public ResponseEntity<HttpStatus> deleteDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .map(department -> {
                    departmentRepository.delete(department);
                    return new ResponseEntity<HttpStatus>(HttpStatus.NO_CONTENT);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
