package com.ak.exam.app.api;

import com.ak.exam.app.dto.ExamHoleAssignmentDTO;
import com.ak.exam.app.dto.SeatNumberRequest;
import com.ak.exam.app.model.ExamHole;
import com.ak.exam.app.model.ExamHoleAssignment;
import com.ak.exam.app.repo.ExamHoleAssignmentRepository;
import com.ak.exam.app.repo.ExamHoleRepository;
import com.ak.exam.app.service.ExamHoleService;
import com.ak.exam.user.model.User;
import com.ak.exam.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/examhole-assignment")
@RequiredArgsConstructor
public class ExamHoleAssignmentAPI {
    private final ExamHoleAssignmentRepository examHoleAssignmentRepository;
    private final UserRepository userRepository;
    private final ExamHoleRepository   examHoleRepository;
    private final ExamHoleService examHoleService;

    // Method to get ExamHoleAssignment by ExamHoleId and SeatNumber using path variables
    @GetMapping("/get/{examHoleId}/{seatNumber}")
    public ExamHoleAssignmentDTO getExamHoleAssignment(
            @PathVariable("examHoleId") Long examHoleId,
            @PathVariable("seatNumber") String seatNumber) {
        ExamHoleAssignment assignment = examHoleAssignmentRepository
                .findByExamHoleIdAndSeatNumber(examHoleId, seatNumber)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        // Map to DTO
        ExamHoleAssignmentDTO dto = new ExamHoleAssignmentDTO();
        dto.setId(assignment.getId());
        dto.setExamHoleId(assignment.getExamHole().getId());
        dto.setUserId(assignment.getUser().getId());
        dto.setSeatNumber(assignment.getSeatNumber());

        return dto;
    }

    // Method to get ExamHoleAssignment by ExamHoleId and UserId using path variables
    @GetMapping("/get/{examHoleId}/user/{userId}")
    public ExamHoleAssignmentDTO getExamHoleAssignmentByUser(
            @PathVariable("examHoleId") Long examHoleId,
            @PathVariable("userId") Long userId) {
        ExamHoleAssignment assignment = examHoleAssignmentRepository
                .findByExamHoleIdAndUserId(examHoleId, userId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        // Map to DTO
        ExamHoleAssignmentDTO dto = new ExamHoleAssignmentDTO();
        dto.setId(assignment.getId());
        dto.setExamHoleId(assignment.getExamHole().getId());
        dto.setUserId(assignment.getUser().getId());
        dto.setSeatNumber(assignment.getSeatNumber());

        return dto;
    }
    @GetMapping("/getExamHolesForUser/{userId}")
    public ResponseEntity<List<ExamHoleAssignmentDTO>> getExamHolesForUser(
            @PathVariable("userId") Long userId) {
        List<ExamHoleAssignment> assignments = examHoleAssignmentRepository.findByUserId(userId);

        // Map to DTOs
        List<ExamHoleAssignmentDTO> dtos = assignments.stream().map(assignment -> {
            ExamHoleAssignmentDTO dto = new ExamHoleAssignmentDTO();
            dto.setId(assignment.getId());
            dto.setExamHoleId(assignment.getExamHole().getId());
            dto.setUserId(assignment.getUser().getId());
            dto.setSeatNumber(assignment.getSeatNumber());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Method to get all ExamHoleAssignments
    @GetMapping("/getAll")
    public Iterable<ExamHoleAssignmentDTO> getAllExamHoleAssignments() {
        Iterable<ExamHoleAssignment> assignments = examHoleAssignmentRepository.findAll();

        // Map to DTOs
        List<ExamHoleAssignmentDTO> dtos = new ArrayList<>();
        for (ExamHoleAssignment assignment : assignments) {
            ExamHoleAssignmentDTO dto = new ExamHoleAssignmentDTO();
            dto.setId(assignment.getId());
            dto.setExamHoleId(assignment.getExamHole().getId());
            dto.setUserId(assignment.getUser().getId());
            dto.setSeatNumber(assignment.getSeatNumber());
            dtos.add(dto);
        }

        return dtos;
    }

    // **New Endpoint**: Get all users in a specific ExamHole
    @GetMapping("/getUsersInExamHole/{examHoleId}/users")
    public ResponseEntity<List<ExamHoleAssignmentDTO>> getUsersInExamHole(
            @PathVariable("examHoleId") Long examHoleId) {
        List<ExamHoleAssignment> assignments = examHoleAssignmentRepository.findByExamHoleId(examHoleId);

        // Map to DTOs
        List<ExamHoleAssignmentDTO> dtos = assignments.stream().map(assignment -> {
            ExamHoleAssignmentDTO dto = new ExamHoleAssignmentDTO();
            dto.setId(assignment.getId());
            dto.setExamHoleId(assignment.getExamHole().getId());
            dto.setUserId(assignment.getUser().getId());
            dto.setSeatNumber(assignment.getSeatNumber());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Method to add a user to an ExamHole with a seat number
    @PostMapping("/addUserToExamHole/{examHoleId}/users/{userId}")
    public ResponseEntity<String> addUserToExamHole(
            @PathVariable Long examHoleId,
            @PathVariable Long userId,
            @RequestBody SeatNumberRequest seatNumberRequest) {
        try {
            // Create or fetch the ExamHole and User entities
            ExamHole examHole = examHoleRepository.findById(examHoleId)
                    .orElseThrow(() -> new RuntimeException("ExamHole not found"));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if the user is already assigned to this exam hole
            if (examHoleAssignmentRepository.existsByExamHoleAndUser(examHole, user)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User already assigned to this exam hole.");
            }

            // Create the new ExamHoleAssignment
            ExamHoleAssignment assignment = ExamHoleAssignment.builder()
                    .examHole(examHole)
                    .user(user)
                    .seatNumber(seatNumberRequest.getSeatNumber())
                    .build();

            examHoleAssignmentRepository.save(assignment);

            return ResponseEntity.status(HttpStatus.CREATED).body("User assigned to exam hole successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error assigning user to exam hole: " + e.getMessage());
        }
    }

    // Method to update the user's seat number in an ExamHole
    @PutMapping("/updateUserSeatInExamHole/{examHoleId}/users/{userId}")
    public ResponseEntity<String> updateUserSeatInExamHole(
            @PathVariable Long examHoleId,
            @PathVariable Long userId,
            @RequestBody SeatNumberRequest seatNumberRequest) {
        try {
            // Fetch the ExamHoleAssignment by ExamHoleId and UserId
            ExamHoleAssignment assignment = examHoleAssignmentRepository
                    .findByExamHoleIdAndUserId(examHoleId, userId)
                    .orElseThrow(() -> new RuntimeException("Assignment not found"));

            // Update the seat number
            assignment.setSeatNumber(seatNumberRequest.getSeatNumber());

            // Save the updated assignment
            examHoleAssignmentRepository.save(assignment);

            return ResponseEntity.status(HttpStatus.OK).body("Seat number updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating seat number: " + e.getMessage());
        }
    }

    /**
     * Assigns seats automatically with default level grouping (1&3 together, 2&4 together)
     *
     * @param examHoleID The ID of the exam hole
     * @param requestBody Contains the selected departments
     * @return Response with status and message
     */
    @PostMapping("/assign-seats/{examHoleID}")
    public ResponseEntity<Map<String, Object>> assignSeats(
            @PathVariable Long examHoleID,
            @RequestBody Map<String, List<String>> requestBody) {

        try {
            List<String> selectedDepartments = requestBody.get("departments");

            if (selectedDepartments == null || selectedDepartments.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "status", "error",
                                "message", "No departments selected"
                        ));
            }

            // Use the default grouping: Levels 1&3 together, Levels 2&4 together
            examHoleService.distributeUsersToSeats(examHoleID, selectedDepartments);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Seat distribution completed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Assigns seats with custom level grouping
     *
     * @param examHoleID The ID of the exam hole
     * @param requestBody Contains selected departments and level groups
     * @return Response with status and message
     */
    @PostMapping("/assign-seats-custom/{examHoleID}")
    public ResponseEntity<Map<String, Object>> assignSeatsCustom(
            @PathVariable Long examHoleID,
            @RequestBody Map<String, Object> requestBody) {

        try {
            @SuppressWarnings("unchecked")
            List<String> selectedDepartments = (List<String>) requestBody.get("departments");

            @SuppressWarnings("unchecked")
            List<Integer> firstLevelGroup = (List<Integer>) requestBody.get("firstLevelGroup");

            @SuppressWarnings("unchecked")
            List<Integer> secondLevelGroup = (List<Integer>) requestBody.get("secondLevelGroup");

            if (selectedDepartments == null || selectedDepartments.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "status", "error",
                                "message", "No departments selected"
                        ));
            }

            if (firstLevelGroup == null || firstLevelGroup.isEmpty() ||
                    secondLevelGroup == null || secondLevelGroup.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "status", "error",
                                "message", "Level groups must be specified"
                        ));
            }

            // Use custom level grouping
            examHoleService.distributeUsersToSeats(examHoleID, selectedDepartments, firstLevelGroup, secondLevelGroup);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Seat distribution completed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }
    }



}
