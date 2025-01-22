package com.ak.exam.app.api;

import com.ak.exam.app.model.ExamHoleAssignment;
import com.ak.exam.app.model.ExamSchedule;
import com.ak.exam.app.dto.ExamHoleAssignmentRequest;
import com.ak.exam.app.service.ExamScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exam-schedule")
@RequiredArgsConstructor
public class ExamScheduleAPI {

    private final ExamScheduleService examScheduleService;


    // Add new ExamSchedule
    @PostMapping("/addExamSchedule")
    public ResponseEntity<ExamSchedule> addExamSchedule(@RequestBody  ExamSchedule examSchedule) {
        return examScheduleService.addExamSchedule(examSchedule);
    }

    // Get all ExamSchedules
    @GetMapping("/getAllExamSchedules")
    public ResponseEntity<List<ExamSchedule>> getAllExamSchedules() {
        return examScheduleService.getAllExamSchedules();
    }

    // Get ExamSchedule by ID
    @GetMapping("/getExamScheduleById/{examScheduleId}")
    public ResponseEntity<ExamSchedule> getExamScheduleById(@PathVariable Long examScheduleId) {
        return examScheduleService.getExamScheduleById(examScheduleId);
    }

    // Update ExamSchedule
    @PutMapping("/updateExamScheduleById/{examScheduleId}")
    public ResponseEntity<ExamSchedule> updateExamScheduleById(@PathVariable Long examScheduleId,
                                                               @RequestBody  ExamSchedule examScheduleDetails) {
        return examScheduleService.updateExamSchedule(examScheduleId, examScheduleDetails);
    }

    // Delete ExamSchedule by ID
    @DeleteMapping("/deleteExamScheduleById/{examScheduleId}")
    public ResponseEntity<String> deleteExamScheduleById(@PathVariable Long examScheduleId) {
        return examScheduleService.deleteExamScheduleById(examScheduleId);
    }
}
