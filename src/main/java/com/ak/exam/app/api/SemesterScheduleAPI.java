package com.ak.exam.app.api;

import com.ak.exam.app.model.SemesterSchedule;
import com.ak.exam.app.service.SemesterScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/semester-schedule")
@RequiredArgsConstructor
public class SemesterScheduleAPI {

    private final SemesterScheduleService semesterScheduleService;

    // Add new SemesterSchedule
    @PostMapping("/addSemesterSchedule")
    public ResponseEntity<SemesterSchedule> addSemesterSchedule(@RequestBody  SemesterSchedule semesterSchedule) {
        return semesterScheduleService.addSemesterSchedule(semesterSchedule);
    }

    // Get all SemesterSchedules
    @GetMapping("/getAllSemesterSchedules")
    public ResponseEntity<List<SemesterSchedule>> getAllSemesterSchedules() {
        return semesterScheduleService.getAllSemesterSchedules();
    }

    // Get SemesterSchedule by ID
    @GetMapping("/getSemesterScheduleById/{id}")
    public ResponseEntity<SemesterSchedule> getSemesterScheduleById(@PathVariable Long id) {
        return semesterScheduleService.getSemesterScheduleById(id);
    }

    // Update SemesterSchedule by ID
    @PutMapping("/updateSemesterScheduleById/{id}")
    public ResponseEntity<SemesterSchedule> updateSemesterScheduleById(
            @PathVariable Long id,
            @RequestBody  SemesterSchedule semesterScheduleDetails) {
        return semesterScheduleService.updateSemesterScheduleById(id, semesterScheduleDetails);
    }

    // Delete SemesterSchedule by ID
    @DeleteMapping("/deleteSemesterScheduleById/{id}")
    public ResponseEntity<HttpStatus> deleteSemesterScheduleById(@PathVariable Long id) {
        return semesterScheduleService.deleteSemesterScheduleById(id);
    }
}
