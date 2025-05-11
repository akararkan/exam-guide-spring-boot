package com.ak.exam.app.api;

import com.ak.exam.app.dto.StudentRequestDTO;
import com.ak.exam.app.model.StudentRequest;
import com.ak.exam.app.service.StudentRequestService;
import com.ak.exam.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/student-requests")
@RequiredArgsConstructor
public class StudentRequestAPI {

    private final StudentRequestService studentRequestService;

    // Endpoint to add a new StudentRequest
    @PostMapping("/addStudentRequest/{userId}")
    public ResponseEntity<StudentRequestDTO> addStudentRequest(
            @RequestPart("attachmentFile") MultipartFile attachmentFile,
            @RequestPart("studentRequest") StudentRequest studentRequest,
            @PathVariable Long userId) throws IOException {
        // Call the service method to add a new student request
        return studentRequestService.addStudentRequest(studentRequest, userId, attachmentFile);
    }

    // Endpoint to get all StudentRequests
    @GetMapping("/getAllStudentRequests")
    public ResponseEntity<List<StudentRequestDTO>> getAllStudentRequests() {
        // Call the service method to fetch all student requests
        return studentRequestService.getAllStudentRequests();
    }

    // Endpoint to get all StudentRequests for a specific user
    @GetMapping("/getStudentRequestsByUserId/{userId}")
    public ResponseEntity<List<StudentRequestDTO>> getStudentRequestsByUserId(@PathVariable Long userId) {
        // Call the service method to fetch student requests by user ID
        return studentRequestService.getStudentRequestsByUserId(userId);
    }

    // Endpoint to get a specific StudentRequest by its ID
    @GetMapping("/getStudentRequestById/{id}")
    public ResponseEntity<StudentRequestDTO> getStudentRequestById(@PathVariable Long id) {
        // Call the service method to fetch a student request by ID
        return studentRequestService.getStudentRequestById(id);
    }

    // Endpoint to update an existing StudentRequest
    @PutMapping("/updateStudentRequest/{id}/{userId}")
    public ResponseEntity<StudentRequestDTO> updateStudentRequest(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody StudentRequest studentRequest) throws IOException {
        // Call the service method to update the student request
        return studentRequestService.updateStudentRequest(id, studentRequest, userId);
    }

    // Endpoint to delete a StudentRequest by ID
    @DeleteMapping("/deleteStudentRequest/{id}")
    public ResponseEntity<Void> deleteStudentRequest(@PathVariable Long id) {
        // Call the service method to delete the student request
        return studentRequestService.deleteStudentRequest(id);
    }

    // Endpoint to get the image by its filename
    @GetMapping("/images/{fileName}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) throws IOException {
        return studentRequestService.getImage(fileName);
    }
}