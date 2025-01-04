package com.ak.exam.app.service;

import com.ak.exam.app.model.ExamSchedule;
import com.ak.exam.app.repo.ExamScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ExamScheduleService {

    private final ExamScheduleRepository examScheduleRepository;

    // Add new ExamSchedule
    public ResponseEntity<ExamSchedule> addExamSchedule(ExamSchedule examSchedule) {
        ExamSchedule newExamSchedule = ExamSchedule.builder()
                .examDate(examSchedule.getExamDate())
                .startExamTime(examSchedule.getStartExamTime())
                .endExamTime(examSchedule.getEndExamTime())
                .createdAt(new Date()) // Set the created date
                .updatedAt(null)       // Initially null, will be updated later
                .build();
        examScheduleRepository.save(newExamSchedule);
        return new ResponseEntity<>(newExamSchedule, HttpStatus.CREATED);
    }

    // Update ExamSchedule by ID (using Optional's map method)
    public ResponseEntity<ExamSchedule> updateExamSchedule(Long examScheduleId, ExamSchedule examScheduleDetails) {
        return examScheduleRepository.findById(examScheduleId)
                .map(existingExamSchedule -> {
                    existingExamSchedule.setExamDate(examScheduleDetails.getExamDate());  // Update exam date
                    existingExamSchedule.setEndExamTime(examScheduleDetails.getEndExamTime());
                    existingExamSchedule.setEndExamTime(examScheduleDetails.getEndExamTime());
                    existingExamSchedule.setUpdatedAt(new Date());                        // Set updated timestamp

                    examScheduleRepository.save(existingExamSchedule);
                    return new ResponseEntity<>(existingExamSchedule, HttpStatus.OK);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));  // If not found, return NOT_FOUND
    }

    // Get all ExamSchedules
    public ResponseEntity<List<ExamSchedule>> getAllExamSchedules() {
        List<ExamSchedule> examSchedules = examScheduleRepository.findAll();
        return new ResponseEntity<>(examSchedules, HttpStatus.OK);
    }

    // Get ExamSchedule by ID
    public ResponseEntity<ExamSchedule> getExamScheduleById(Long examScheduleId) {
        ExamSchedule examSchedule = examScheduleRepository.findById(examScheduleId)
                .orElseThrow(() -> new RuntimeException("ExamSchedule not found with id " + examScheduleId));
        return new ResponseEntity<>(examSchedule, HttpStatus.OK);
    }


    public ResponseEntity<String> deleteExamScheduleById(Long examScheduleId) {
        Optional<ExamSchedule> optionalSchedule = examScheduleRepository.findById(examScheduleId);
        if (optionalSchedule.isPresent()) {
            try {
                examScheduleRepository.deleteById(examScheduleId);
                return ResponseEntity.ok("Exam Schedule deleted successfully.");
            } catch (Exception e) {
                // Log the exception
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("An error occurred while deleting the exam schedule.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Exam Schedule not found.");
        }
    }
}
