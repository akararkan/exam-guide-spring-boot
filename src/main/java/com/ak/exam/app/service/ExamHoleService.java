package com.ak.exam.app.service;

import com.ak.exam.app.model.ExamHole;
import com.ak.exam.app.model.ExamHoleAssignment;
import com.ak.exam.app.repo.ExamHoleAssignmentRepository;
import com.ak.exam.app.repo.ExamHoleRepository;
import com.ak.exam.app.dto.UserSeatDTO;
import com.ak.exam.user.model.User;
import com.ak.exam.user.repo.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ExamHoleService {

    private final ExamHoleRepository examHoleRepository;
    private final UserRepository userRepository;
    private final ExamHoleAssignmentRepository assignmentRepository;

    // Define a pattern for seat number validation (e.g., "A1", "B2")
    private static final Pattern SEAT_NUMBER_PATTERN = Pattern.compile("^[A-Z]\\d+$");

    // Create a new ExamHole
    public ResponseEntity<ExamHole> addExamHole(ExamHole examHole) {
        // Initialize availableSlots based on capacity
        ExamHole newExamHole = ExamHole.builder()
                .number(examHole.getNumber())
                .holeName(examHole.getHoleName())
                .capacity(examHole.getCapacity())
                .availableSlots(examHole.getCapacity()) // Initially, all slots are available
                .row(examHole.getRow())
                .col(examHole.getCol())
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        ExamHole savedExamHole = examHoleRepository.save(newExamHole);
        return new ResponseEntity<>(savedExamHole, HttpStatus.CREATED);
    }

    // Get ExamHole by ID
    public ResponseEntity<ExamHole> getExamHoleById(Long id) {
        return examHoleRepository.findById(id)
                .map(examHole -> new ResponseEntity<>(examHole, HttpStatus.OK))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam Hall not found"));
    }

    // Get all ExamHoles
    public ResponseEntity<List<ExamHole>> getAllExamHoles() {
        List<ExamHole> examHoles = examHoleRepository.findAll();
        return new ResponseEntity<>(examHoles, HttpStatus.OK);
    }

    // Update ExamHole
    public ResponseEntity<ExamHole> updateExamHole(Long id, ExamHole examHoleDetails) {
        ExamHole existingExamHole = examHoleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam Hall not found"));

        existingExamHole.setNumber(examHoleDetails.getNumber());
        existingExamHole.setHoleName(examHoleDetails.getHoleName());

        // Update capacity and adjust availableSlots accordingly
        if (examHoleDetails.getCapacity() != null) {
            int newCapacity = examHoleDetails.getCapacity();
            int currentAssignments = existingExamHole.getAssignments().size();
            if (newCapacity < currentAssignments) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New capacity less than current assignments");
            }
            existingExamHole.setCapacity(newCapacity);
            existingExamHole.setAvailableSlots(newCapacity - currentAssignments);
        }

        if (examHoleDetails.getRow() != null) {
            existingExamHole.setRow(examHoleDetails.getRow());
        }

        if (examHoleDetails.getCol() != null) {
            existingExamHole.setCol(examHoleDetails.getCol());
        }

        existingExamHole.setUpdatedAt(new Date());

        ExamHole updatedExamHole = examHoleRepository.save(existingExamHole);
        return new ResponseEntity<>(updatedExamHole, HttpStatus.OK);
    }

    // Delete ExamHole
    public ResponseEntity<HttpStatus> deleteExamHole(Long id) {
        ExamHole examHole = examHoleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam Hall not found"));

        // Delete all assignments associated with the hall
        List<ExamHoleAssignment> assignments = examHole.getAssignments();
        if (assignments != null && !assignments.isEmpty()) {
            assignmentRepository.deleteAll(assignments);
        }

        examHoleRepository.delete(examHole);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Add User to ExamHole with Seat Number
    public ResponseEntity<String> addUserToExamHole(Long examHoleId, Long userId, String seatNumber) { // Changed Integer to String
        ExamHole examHole = examHoleRepository.findById(examHoleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam Hall not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Validate seat number format
        if (seatNumber == null || seatNumber.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat number cannot be empty");
        }

        if (!SEAT_NUMBER_PATTERN.matcher(seatNumber).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid seat number format. Expected format like 'A1', 'B2', etc.");
        }

        // Check if seat number is already taken
        Optional<ExamHoleAssignment> existingSeat = assignmentRepository.findByExamHoleIdAndSeatNumber(examHoleId, seatNumber);
        if (existingSeat.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat number already taken");
        }

        // Check if user is already assigned to the hall
        Optional<ExamHoleAssignment> existingAssignment = assignmentRepository.findByExamHoleIdAndUserId(examHoleId, userId);
        if (existingAssignment.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already assigned to this hall");
        }

        // Check if there are available slots
        if (examHole.getAvailableSlots() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No available slots in this hall");
        }

        // Create new assignment
        ExamHoleAssignment assignment = ExamHoleAssignment.builder()
                .examHole(examHole)
                .user(user)
                .seatNumber(seatNumber)
                .build();

        assignmentRepository.save(assignment);

        // Update available slots
        examHole.setAvailableSlots(examHole.getAvailableSlots() - 1);
        examHoleRepository.save(examHole);

        return new ResponseEntity<>("User added to hall successfully with seat number " + seatNumber, HttpStatus.OK);
    }

    // Remove User from ExamHole
    public ResponseEntity<String> removeUserFromExamHole(Long examHoleId, Long userId) {
        ExamHole examHole = examHoleRepository.findById(examHoleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam Hall not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Find the assignment
        ExamHoleAssignment assignment = assignmentRepository.findByExamHoleIdAndUserId(examHoleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not assigned to this hall"));

        // Remove the assignment
        assignmentRepository.delete(assignment);

        // Update available slots
        examHole.setAvailableSlots(examHole.getAvailableSlots() + 1);
        examHoleRepository.save(examHole);

        return new ResponseEntity<>("User removed from hall successfully", HttpStatus.OK);
    }

    // Edit User's Seat Number in ExamHole
    public ResponseEntity<String> editUserSeatInExamHole(Long examHoleId, Long userId, String newSeatNumber) { // Changed Integer to String
        ExamHole examHole = examHoleRepository.findById(examHoleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam Hall not found"));

        // Validate new seat number format
        if (newSeatNumber == null || newSeatNumber.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat number cannot be empty");
        }

        if (!SEAT_NUMBER_PATTERN.matcher(newSeatNumber).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid seat number format. Expected format like 'A1', 'B2', etc.");
        }

        // Find the existing assignment
        ExamHoleAssignment assignment = assignmentRepository.findByExamHoleIdAndUserId(examHoleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not assigned to this hall"));

        // Check if the new seat number is already taken
        Optional<ExamHoleAssignment> existingSeat = assignmentRepository.findByExamHoleIdAndSeatNumber(examHoleId, newSeatNumber);
        if (existingSeat.isPresent() && !existingSeat.get().getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat number already taken");
        }

        // Update the seat number
        assignment.setSeatNumber(newSeatNumber);
        assignmentRepository.save(assignment);

        return new ResponseEntity<>("User's seat number updated successfully to " + newSeatNumber, HttpStatus.OK);
    }

    // Get Users in an ExamHole with Seat Numbers
    public ResponseEntity<List<UserSeatDTO>> getUsersInExamHole(Long examHoleId) {
        ExamHole examHole = examHoleRepository.findById(examHoleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam Hall not found"));

        List<ExamHoleAssignment> assignments = examHole.getAssignments();

        List<UserSeatDTO> userSeatDTOs = assignments.stream()
                .map(assignment -> new UserSeatDTO(
                        assignment.getUser().getId(),
                        assignment.getUser().getFname(),
                        assignment.getUser().getLname(),
                        assignment.getUser().getEmail(),
                        assignment.getSeatNumber() // Now a String
                ))
                .collect(Collectors.toList());

        return new ResponseEntity<>(userSeatDTOs, HttpStatus.OK);
    }

    // Get ExamHoles for a specific User
    public ResponseEntity<List<ExamHole>> getExamHolesForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<ExamHole> examHoles = user.getAssignments().stream()
                .map(ExamHoleAssignment::getExamHole)
                .collect(Collectors.toList());

        return new ResponseEntity<>(examHoles, HttpStatus.OK);
    }

    // Check if User is in ExamHole
    public ResponseEntity<Boolean> isUserInExamHole(Long examHoleId, Long userId) {
        boolean exists = assignmentRepository.findByExamHoleIdAndUserId(examHoleId, userId).isPresent();
        return new ResponseEntity<>(exists, HttpStatus.OK);
    }

    // Get Available ExamHoles (with space)
    public ResponseEntity<List<ExamHole>> getAvailableExamHoles() {
        List<ExamHole> availableExamHoles = examHoleRepository.findByAvailableSlotsGreaterThan(0);
        return new ResponseEntity<>(availableExamHoles, HttpStatus.OK);
    }
}
