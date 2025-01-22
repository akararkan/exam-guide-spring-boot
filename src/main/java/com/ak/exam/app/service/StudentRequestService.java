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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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
    public ResponseEntity<StudentRequestDTO> addStudentRequest(StudentRequest studentRequest, Long userId, MultipartFile attachmentFile) throws IOException {
        // Find the user by userId
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Handle image upload and get the image URL
        String imageUrl = uploadImageAndGetUrl(attachmentFile);

        // Create a new StudentRequest and save it
        StudentRequest newStudentRequest = StudentRequest.builder()
                .user(user)
                .requestHeader(studentRequest.getRequestHeader())
                .requestBody(studentRequest.getRequestBody())
                .requestDate(new Date())
                .requestStatus(RequestStatus.PENDING)
                .createdAt(new Date())
                .updatedAt(null)
                .attachmentFile(imageUrl) // Set the image URL here
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

    // Method to update a specific StudentRequest with only the request status
    public ResponseEntity<StudentRequestDTO> updateStudentRequest(Long id, StudentRequest studentRequest, Long userId) {
        // Find the StudentRequest by its ID
        Optional<StudentRequest> existingRequestOptional = studentRequestRepository.findById(id);

        if (existingRequestOptional.isPresent()) {
            // Find the user by userId
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get the existing request
            StudentRequest existingRequest = existingRequestOptional.get();

            // Update only the request status
            existingRequest.setUser(user); // Update the user reference if needed
            existingRequest.setRequestStatus(studentRequest.getRequestStatus()); // Only update the status
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
                .attachmentFile(studentRequest.getAttachmentFile()) // Include attachment URL in the response
                .build();
    }

    // Utility method to handle file upload and return the local file path

    private String uploadImageAndGetUrl(MultipartFile attachmentFile) throws IOException {
        if (attachmentFile == null || attachmentFile.isEmpty()) {
            return null; // Return null if no file is uploaded
        }

        // Define the directory to save images inside the static/images folder
        String directoryPath = "/home/akar/uploads/images/"; // Absolute path for saving images
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs(); // Create directory if it doesn't exist
        }

        // Save the file with a unique name
        String fileName = System.currentTimeMillis() + "_" + attachmentFile.getOriginalFilename();
        File targetFile = new File(directoryPath + fileName);

        // Upload the file to the server
        attachmentFile.transferTo(targetFile);

        // Return only the file name (e.g., 1737561338455_1.jpeg)
        return fileName;
    }


    // Method to get the image file by its name
    public ResponseEntity<Resource> getImage(String fileName) throws IOException {
        // Define the directory where images are stored
        String directoryPath = "/home/akar/uploads/images/"; // Make sure this matches your directory path
        File file = new File(directoryPath + fileName);

        if (file.exists()) {
            Resource resource = new FileSystemResource(file);

            // Set content type (you can also extend this to support more file types)
            String contentType = "image/jpeg"; // Default to JPEG, change based on file type

            // Return the image as a ResponseEntity
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } else {
            // If the file doesn't exist, return a 404 error
            return ResponseEntity.notFound().build();
        }
    }



}
