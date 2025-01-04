package com.ak.exam.app.service;

import com.ak.exam.app.dto.StudentRequestDTO;
import com.ak.exam.app.enums.RequestStatus;
import com.ak.exam.app.model.StudentRequest;
import com.ak.exam.app.repo.StudentRequestRepository;
import com.ak.exam.user.dto.UserDTO;
import com.ak.exam.user.model.User;
import com.ak.exam.user.repo.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StudentRequestService {

    private final StudentRequestRepository studentRequestRepository;
    private final UserRepository userRepository;

    // Method to add a new StudentRequest
    public ResponseEntity<StudentRequestDTO> addStudentRequest(StudentRequest studentRequest, Long userId) {
        // Find the user by userId
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create a new StudentRequest and save it
        StudentRequest newStudentRequest = StudentRequest.builder()
                .user(user)
                .requestHeader(studentRequest.getRequestHeader())
                .requestBody(studentRequest.getRequestBody())
                .requestDate(new Date())
                .requestStatus(RequestStatus.PENDING)
                .createdAt(new Date())
                .updatedAt(null)
                .build();

        studentRequestRepository.save(newStudentRequest);

        // Convert the saved entity to a DTO and return it
        StudentRequestDTO studentRequestDTO = convertToDTO(newStudentRequest);
        return ResponseEntity.ok(studentRequestDTO);
    }

    // Method to get all StudentRequests (converted to DTOs)
    public ResponseEntity<List<StudentRequestDTO>> getAllStudentRequests() {
        List<StudentRequest> studentRequests = studentRequestRepository.findAll();

        List<StudentRequestDTO> studentRequestDTOs = studentRequests.stream()
                .map(this::convertToDTO) // Convert each StudentRequest to StudentRequestDTO
                .collect(Collectors.toList());

        return ResponseEntity.ok(studentRequestDTOs);
    }

    // Method to get a specific StudentRequest by ID (converted to DTO)
    public ResponseEntity<StudentRequestDTO> getStudentRequestById(Long id) {
        Optional<StudentRequest> studentRequestOptional = studentRequestRepository.findById(id);

        if (studentRequestOptional.isPresent()) {
            StudentRequestDTO studentRequestDTO = convertToDTO(studentRequestOptional.get());
            return ResponseEntity.ok(studentRequestDTO);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Method to update a specific StudentRequest
    // Method to update a specific StudentRequest
    public ResponseEntity<StudentRequestDTO> updateStudentRequest(Long id, StudentRequest studentRequest, Long userId) {
        // Find the StudentRequest by its ID
        Optional<StudentRequest> existingRequestOptional = studentRequestRepository.findById(id);

        if (existingRequestOptional.isPresent()) {
            // Find the user by userId
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get the existing request
            StudentRequest existingRequest = existingRequestOptional.get();

            // Update the fields
            existingRequest.setUser(user); // Set the user based on userId
            existingRequest.setRequestHeader(studentRequest.getRequestHeader());
            existingRequest.setRequestBody(studentRequest.getRequestBody());
            existingRequest.setRequestStatus(studentRequest.getRequestStatus());
            existingRequest.setUpdatedAt(new Date()); // Set updatedAt to current time

            // Save the updated request
            studentRequestRepository.save(existingRequest);

            // Convert the updated entity to DTO and return
            StudentRequestDTO studentRequestDTO = convertToDTO(existingRequest);
            return ResponseEntity.ok(studentRequestDTO);
        } else {
            // If the StudentRequest is not found, return 404 Not Found
            return ResponseEntity.notFound().build();
        }
    }

    // Method to delete a specific StudentRequest
    public ResponseEntity<Void> deleteStudentRequest(Long id) {
        Optional<StudentRequest> studentRequestOptional = studentRequestRepository.findById(id);

        if (studentRequestOptional.isPresent()) {
            studentRequestRepository.delete(studentRequestOptional.get());
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Conversion method from StudentRequest entity to StudentRequestDTO
    public StudentRequestDTO convertToDTO(StudentRequest studentRequest) {
        UserDTO userDTO = UserDTO.builder()
                .id(studentRequest.getUser().getId())
                .username(studentRequest.getUser().getUsername())
                .email(studentRequest.getUser().getEmail())
                .build();

        return StudentRequestDTO.builder()
                .id(studentRequest.getId())
                .requestHeader(studentRequest.getRequestHeader())
                .requestBody(studentRequest.getRequestBody())
                .requestDate(studentRequest.getRequestDate())
                .requestStatus(studentRequest.getRequestStatus())
                .createdAt(studentRequest.getCreatedAt())
                .updatedAt(studentRequest.getUpdatedAt())
                .user(userDTO) // Include UserDTO in the response
                .build();
    }
}
