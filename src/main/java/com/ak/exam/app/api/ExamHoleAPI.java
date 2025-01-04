package com.ak.exam.app.api;
import com.ak.exam.app.dto.UserSeatDTO;
import com.ak.exam.app.model.ExamHole;
import com.ak.exam.app.service.ExamHoleService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/v1/examhole")
public class ExamHoleAPI {

    @Autowired
    private ExamHoleService examHoleService;

    // Create a new ExamHole
    @PostMapping("/addExamHole")
    public ResponseEntity<ExamHole> addExamHole(@RequestBody ExamHole examHole) {
        return examHoleService.addExamHole(examHole);
    }

    // Get ExamHole by ID
    @GetMapping("/getExamHoleById/{id}")
    public ResponseEntity<ExamHole> getExamHoleById(@PathVariable Long id) {
        return examHoleService.getExamHoleById(id);
    }

    // Get all ExamHoles
    @GetMapping("/getAllExamHoles")
    public ResponseEntity<List<ExamHole>> getAllExamHoles() {
        return examHoleService.getAllExamHoles();
    }

    // Update ExamHole
    @PutMapping("/updateExamHole/{id}")
    public ResponseEntity<ExamHole> updateExamHole(@PathVariable Long id, @RequestBody ExamHole examHoleDetails) {
        return examHoleService.updateExamHole(id, examHoleDetails);
    }

    // Delete ExamHole
    @DeleteMapping("/deleteExamHole/{id}")
    public ResponseEntity<HttpStatus> deleteExamHole(@PathVariable Long id) {
        return examHoleService.deleteExamHole(id);
    }

    // Add User to ExamHole with Seat Number
    @PostMapping("/addUserToExamHole/{examHoleId}/users/{userId}")
    public ResponseEntity<String> addUserToExamHole(
            @PathVariable Long examHoleId,
            @PathVariable Long userId,
            @RequestBody SeatNumberRequest seatNumberRequest) {
        return examHoleService.addUserToExamHole(examHoleId, userId, seatNumberRequest.getSeatNumber());
    }

    // Remove User from ExamHole
    @DeleteMapping("/removeUserFromExamHole/{examHoleId}/users/{userId}")
    public ResponseEntity<String> removeUserFromExamHole(
            @PathVariable Long examHoleId,
            @PathVariable Long userId) {
        return examHoleService.removeUserFromExamHole(examHoleId, userId);
    }

    // Edit User's Seat Number in ExamHole
    @PutMapping("/editUserSeatInExamHole/{examHoleId}/users/{userId}")
    public ResponseEntity<String> editUserSeatInExamHole(
            @PathVariable Long examHoleId,
            @PathVariable Long userId,
            @RequestBody SeatNumberRequest seatNumberRequest) {
        return examHoleService.editUserSeatInExamHole(examHoleId, userId, seatNumberRequest.getSeatNumber());
    }

    // Get Users in an ExamHole with Seat Numbers
    @GetMapping("/getUsersInExamHole/{examHoleId}/users")
    public ResponseEntity<List<UserSeatDTO>> getUsersInExamHole(@PathVariable Long examHoleId) {
        return examHoleService.getUsersInExamHole(examHoleId);
    }

    // Get ExamHoles for a specific User
    @GetMapping("/getExamHolesForUser/{userId}")
    public ResponseEntity<List<ExamHole>> getExamHolesForUser(@PathVariable Long userId) {
        return examHoleService.getExamHolesForUser(userId);
    }

    // Check if User is in ExamHole
    @GetMapping("/isUserInExamHole/{examHoleId}/users/{userId}")
    public ResponseEntity<Boolean> isUserInExamHole(
            @PathVariable Long examHoleId,
            @PathVariable Long userId) {
        return examHoleService.isUserInExamHole(examHoleId, userId);
    }

    // Get Available ExamHoles (with space)
    @GetMapping("/getAvailableExamHoles")
    public ResponseEntity<List<ExamHole>> getAvailableExamHoles() {
        return examHoleService.getAvailableExamHoles();
    }

    // DTO for seat number in POST and PUT requests
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SeatNumberRequest {
        private String seatNumber;
    }
}
