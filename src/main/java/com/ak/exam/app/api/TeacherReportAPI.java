package com.ak.exam.app.api;

import com.ak.exam.app.dto.TeacherReportDTO;
import com.ak.exam.app.model.TeacherReport;
import com.ak.exam.app.service.TeacherReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing Teacher Reports.
 */
@RestController
@RequestMapping("/api/v1/teacher-reports")
@RequiredArgsConstructor
public class TeacherReportAPI {

    private final TeacherReportService teacherReportService;

    // Add Teacher Report (userId is passed as a path variable)
    @PostMapping("/addTeacherReport/{userId}")
    public ResponseEntity<TeacherReport> addTeacherReport(@RequestBody TeacherReport teacherReport,
                                                          @PathVariable Long userId) {
        return teacherReportService.addTeacherReport(teacherReport, userId);
    }

    // Update Teacher Report by ID (userId is passed as a path variable)
    @PutMapping("/updateTeacherReport/{userId}/{id}")
    public ResponseEntity<TeacherReport> updateTeacherReport(@PathVariable Long id,
                                                             @PathVariable Long userId,
                                                             @RequestBody TeacherReport teacherReport) {
        return teacherReportService.updateById(id, userId, teacherReport);
    }

    // Get Teacher Report by ID
    @GetMapping("/getTeacherReportById/{id}")
    public ResponseEntity<TeacherReportDTO> getTeacherReportById(@PathVariable Long id) {
        return teacherReportService.getById(id);
    }

    // Get Teacher Reports by User ID (userId is passed as a path variable)
    @GetMapping("/getTeacherReportsByUserId/user/{userId}")
    public ResponseEntity<List<TeacherReportDTO>> getTeacherReportsByUserId(@PathVariable Long userId) {
        return teacherReportService.getByUserId(userId);
    }

    // Get All Teacher Reports
    @GetMapping("/getAllTeacherReports")
    public ResponseEntity<List<TeacherReportDTO>> getAllTeacherReports() {
        return teacherReportService.getAll();
    }

    // Delete Teacher Report by ID
    @DeleteMapping("/deleteTeacherReport/{id}")
    public ResponseEntity<Void> deleteTeacherReport(@PathVariable Long id) {
        return teacherReportService.deleteById(id);
    }
}
