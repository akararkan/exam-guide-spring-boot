package com.ak.exam.app.service;

import com.ak.exam.app.dto.TeacherReportDTO;
import com.ak.exam.app.model.TeacherReport;
import com.ak.exam.app.repo.TeacherReportRepository;
import com.ak.exam.user.dto.UserDTO;
import com.ak.exam.user.model.User;
import com.ak.exam.user.repo.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TeacherReportService {

    private final TeacherReportRepository teacherReportRepository;
    private final UserRepository userRepository;
    // Utility method to generate a unique report code
    private String generateReportCode() {
        return "RPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public ResponseEntity<TeacherReport> addTeacherReport(TeacherReport teacherReport, Long userId) {
        User user = userRepository.findById(userId).orElseThrow();

        TeacherReport report = TeacherReport.builder()
                .id(teacherReport.getId())
                .user(user)
                .reportCode(generateReportCode())
                .reportHeader(teacherReport.getReportHeader())
                .reportBody(teacherReport.getReportBody())
                .createdAt(teacherReport.getCreatedAt())
                .updatedAt(teacherReport.getUpdatedAt())
                .build();
        teacherReportRepository.save(report);
        return ResponseEntity.ok(report);
    }

    public TeacherReportDTO convertToDTO(TeacherReport teacherReport) {
        UserDTO userDTO = UserDTO.builder()
                .id(teacherReport.getUser().getId())
                .username(teacherReport.getUser().getUsername())
                .email(teacherReport.getUser().getEmail())
                .build();

        return TeacherReportDTO.builder()
                .id(teacherReport.getId())
                .user(userDTO)
                .reportCode(teacherReport.getReportCode())
                .reportHeader(teacherReport.getReportHeader())
                .reportBody(teacherReport.getReportBody())
                .createdAt(teacherReport.getCreatedAt())
                .updatedAt(teacherReport.getUpdatedAt())
                .build();
    }

    // Update Teacher Report and User by ID
    public ResponseEntity<TeacherReport> updateById(Long id, Long userId, TeacherReport teacherReport) {
        TeacherReport existingReport = teacherReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        // Update the report fields
        existingReport.setReportCode(teacherReport.getReportCode());
        existingReport.setReportHeader(teacherReport.getReportHeader());
        existingReport.setReportBody(teacherReport.getReportBody());
        existingReport.setUpdatedAt(teacherReport.getUpdatedAt());

        // Update the associated user based on userId
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        existingReport.setUser(user);

        teacherReportRepository.save(existingReport);
        return ResponseEntity.ok(existingReport);
    }

    // Get Teacher Report by ID
    public ResponseEntity<TeacherReportDTO> getById(Long id) {
        TeacherReport teacherReport = teacherReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        TeacherReportDTO teacherReportDTO = convertToDTO(teacherReport);
        return ResponseEntity.ok(teacherReportDTO);
    }

    // Get Teacher Reports by User ID
    public ResponseEntity<List<TeacherReportDTO>> getByUserId(Long userId) {
        // Fetch the user by userId, if not found throw exception
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Retrieve teacher reports associated with the user
        List<TeacherReport> reports = teacherReportRepository.findByUserId(userId);

        // Convert the list of reports to DTOs
        List<TeacherReportDTO> reportDTOs = reports.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // Return the reports as a response
        return ResponseEntity.ok(reportDTOs);
    }


    // Get All Teacher Reports
    public ResponseEntity<List<TeacherReportDTO>> getAll() {
        List<TeacherReport> reports = teacherReportRepository.findAll();
        List<TeacherReportDTO> reportDTOs = reports.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(reportDTOs);
    }

    // Delete Teacher Report by ID
    public ResponseEntity<Void> deleteById(Long id) {
        TeacherReport teacherReport = teacherReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        teacherReportRepository.delete(teacherReport);
        return ResponseEntity.noContent().build();
    }
}
