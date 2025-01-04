package com.ak.exam.app.api;

import com.ak.exam.app.model.Department;
import com.ak.exam.app.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/department")
@RequiredArgsConstructor
public class DepartmentAPI {

    private final DepartmentService departmentService;

    // Add a new department
    @PostMapping("/addDepartment")
    public ResponseEntity<Department> addDepartment(@RequestBody  Department department) {
        return departmentService.addDepartment(department);
    }

    // Get all departments
    @GetMapping("/getAllDepartments")
    public ResponseEntity<List<Department>> getAllDepartments() {
        return departmentService.getAllDepartments();
    }

    // Get department by ID
    @GetMapping("/getDepartmentById/{id}")
    public ResponseEntity<Department> getDepartmentById(@PathVariable Long id) {
        return departmentService.getDepartmentById(id);
    }

    // Update department by ID
    @PutMapping("/updateDepartmentById/{id}")
    public ResponseEntity<Department> updateDepartmentById(
            @PathVariable Long id, @RequestBody Department department) {
        return departmentService.updateDepartmentById(id, department);
    }

    // Search department by name
    @GetMapping("/searchDepartmentByName")
    public ResponseEntity<List<Department>> searchDepartmentByName(@RequestParam String name) {
        return departmentService.searchDepartmentByName(name);
    }

    // Delete department by ID
    @DeleteMapping("/deleteDepartmentById/{id}")
    public ResponseEntity<HttpStatus> deleteDepartmentById(@PathVariable Long id) {
        return departmentService.deleteDepartmentById(id);
    }
}
