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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.*;
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
    private static final Logger logger = LoggerFactory.getLogger(ExamHoleService.class);

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



    /**
     * Assigns multiple users to multiple exam holes with automatic seat numbering
     * @param assignments List of assignment requests containing user IDs and exam hole IDs
     * @return List of created ExamHoleAssignments
     */
    public List<ExamHoleAssignment> assignUsersToExamHoles(List<ExamHoleAssignmentRequest> assignments) {
        List<ExamHoleAssignment> createdAssignments = new ArrayList<>();

        // Group assignments by exam hole to handle seat numbering
        Map<Long, List<ExamHoleAssignmentRequest>> assignmentsByHole = assignments.stream()
                .collect(Collectors.groupingBy(ExamHoleAssignmentRequest::getExamHoleId));

        for (Map.Entry<Long, List<ExamHoleAssignmentRequest>> entry : assignmentsByHole.entrySet()) {
            Long examHoleId = entry.getKey();
            List<ExamHoleAssignmentRequest> holeAssignments = entry.getValue();

            // Get the exam hole
            ExamHole examHole = examHoleRepository.findById(examHoleId)
                    .orElseThrow(() -> new RuntimeException("Exam Hole not found with id: " + examHoleId));

            // Get current max seat number for this exam hole
            String maxSeatNumber = assignmentRepository.findMaxSeatNumberByExamHoleId(examHoleId)
                    .orElse("0");
            int nextSeatNumber = Integer.parseInt(maxSeatNumber) + 1;

            // Create assignments for each user
            for (ExamHoleAssignmentRequest request : holeAssignments) {
                // Validate user exists
                User user = userRepository.findById(request.getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

                // Check if user is already assigned to any exam hole
                if (assignmentRepository.existsByUserId(user.getId())) {
                    throw new IllegalStateException("User " + user.getId() + " is already assigned to an exam hole");
                }

                // Create and save the assignment
                ExamHoleAssignment assignment = ExamHoleAssignment.builder()
                        .examHole(examHole)
                        .user(user)
                        .seatNumber(String.format("%03d", nextSeatNumber++)) // Format: 001, 002, etc.
                        .build();

                createdAssignments.add(assignmentRepository.save(assignment));
            }
        }

        return createdAssignments;
    }


    public void assignUsersFromExcel(List<ExamHoleAssignmentRequest> assignments) {
        for (ExamHoleAssignmentRequest request : assignments) {
            logger.info("Assigning userId={} to examHoleId={} with seatNumber={}",
                    request.getUserId(), request.getExamHoleId(), request.getSeatNumber());

            // Fetch Exam Hole
            ExamHole examHole = examHoleRepository.findById(request.getExamHoleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam Hall not found."));

            // Fetch User
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

            // Check if user is already assigned to this hall
            if (assignmentRepository.existsByExamHoleAndUser(examHole, user)) {
                logger.error("User with ID {} is already assigned to Exam Hall ID {}", user.getId(), examHole.getId());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "User with ID " + user.getId() + " is already assigned to this exam hall.");
            }

            // Check if seat number is already taken
            if (assignmentRepository.existsByExamHoleIdAndSeatNumber(examHole.getId(), request.getSeatNumber())) {
                logger.error("Seat number {} is already taken in Exam Hall ID {}", request.getSeatNumber(), examHole.getId());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Seat number " + request.getSeatNumber() + " is already taken in this exam hall.");
            }

            // Check hall capacity
            long currentAssignments = assignmentRepository.countByExamHoleId(examHole.getId());
            if (currentAssignments >= examHole.getCapacity()) {
                logger.error("Exam Hall ID {} is already full.", examHole.getId());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam Hall is already full.");
            }
            // Update the available capacity of the exam hole
            int availableSlots = examHole.getCapacity() - (int) currentAssignments - 1; // Subtract 1 for the new user
            examHole.setAvailableSlots(availableSlots);
            examHoleRepository.save(examHole); // Save the updated exam hole object

            // Create and save assignment
            ExamHoleAssignment assignment = ExamHoleAssignment.builder()

                    .examHole(examHole)
                    .user(user)
                    .seatNumber(request.getSeatNumber())
                    .build();

            assignmentRepository.save(assignment);
            logger.info("Assigned userId={} to examHoleId={} at seatNumber={}",
                    user.getId(), examHole.getId(), request.getSeatNumber());
        }
    }

    public List<ExamHoleAssignmentRequest> parseExcelFile(MultipartFile file, Long examHoleId) {
        List<ExamHoleAssignmentRequest> assignments = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Validate Headers
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excel file is empty.");
            }

            validateHeaders(headerRow);

            // Process data rows
            for (int rowNumber = 1; rowNumber < sheet.getPhysicalNumberOfRows(); rowNumber++) {
                Row row = sheet.getRow(rowNumber);

                // Skip empty rows
                if (isEmptyRow(row)) {
                    logger.debug("Skipping empty row at index {}", rowNumber);
                    continue;
                }

                try {
                    ExamHoleAssignmentRequest request = processRow(row, rowNumber, examHoleId);
                    if (request != null) {
                        assignments.add(request);
                    }
                } catch (ResponseStatusException e) {
                    logger.error("Error processing row {}: {}", rowNumber + 1, e.getMessage());
                    throw e;
                }
            }

        } catch (ResponseStatusException e) {
            logger.error("Error parsing Excel file: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error parsing Excel file: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Failed to parse Excel file: " + e.getMessage());
        }

        if (assignments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid records found in Excel file");
        }

        return assignments;
    }

    private void validateHeaders(Row headerRow) {
        String[] expectedHeaders = {"id", "seatNumber", "examHoleId", "userId"};
        String[] actualHeaders = new String[4];

        for (int i = 0; i < 4; i++) {
            Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            actualHeaders[i] = getStringCellValue(cell);
        }

        if (!Arrays.equals(expectedHeaders, actualHeaders)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format(
                            "Invalid Excel headers. Expected: %s, but got: %s",
                            Arrays.toString(expectedHeaders),
                            Arrays.toString(actualHeaders)
                    )
            );
        }
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) return true;

        for (int cellNum = 0; cellNum < 4; cellNum++) {
            Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            String value = getStringCellValue(cell);
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private ExamHoleAssignmentRequest processRow(Row row, int rowNumber, Long examHoleId) {
        // Get all cells with proper null handling
        Cell idCell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        Cell seatNumberCell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        Cell examHoleIdCell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        Cell userIdCell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

        // Validate and parse ID
        Long id = parseLongValue(idCell, "id", rowNumber + 1);
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Invalid or missing ID at row %d", rowNumber + 1));
        }

        // Validate and parse seat number
        String seatNumber = getStringCellValue(seatNumberCell);
        if (seatNumber == null || seatNumber.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Missing seat number at row %d", rowNumber + 1));
        }
        if (!seatNumber.matches("^[A-Z]\\d+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Invalid seat number format '%s' at row %d", seatNumber, rowNumber + 1));
        }

        // Validate and parse exam hole ID
        Long excelExamHoleId = parseLongValue(examHoleIdCell, "examHoleId", rowNumber + 1);
        if (!examHoleId.equals(excelExamHoleId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Mismatched exam hole ID at row %d. Expected: %d, Found: %d",
                            rowNumber + 1, examHoleId, excelExamHoleId));
        }

        // Validate and parse user ID
        Long userId = parseLongValue(userIdCell, "userId", rowNumber + 1);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Invalid or missing user ID at row %d", rowNumber + 1));
        }

        return ExamHoleAssignmentRequest.builder()
                .userId(userId)
                .examHoleId(examHoleId)
                .seatNumber(seatNumber.trim())
                .rowNumber(rowNumber + 1)
                .build();
    }

    private Long parseLongValue(Cell cell, String fieldName, int rowNumber) {
        try {
            if (cell == null) return null;

            switch (cell.getCellType()) {
                case NUMERIC:
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return (long) numericValue;
                    }
                    throw new NumberFormatException("Value is not an integer");
                case STRING:
                    String stringValue = cell.getStringCellValue().trim();
                    if (stringValue.isEmpty()) return null;
                    return Long.parseLong(stringValue);
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Invalid %s format at row %d: %s", fieldName, rowNumber, getStringCellValue(cell)));
        }
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) return "";

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    }
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return String.format("%.0f", numericValue);
                    }
                    return String.valueOf(numericValue);
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case BLANK:
                    return "";
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }


}
