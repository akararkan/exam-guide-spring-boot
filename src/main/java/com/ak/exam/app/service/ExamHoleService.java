package com.ak.exam.app.service;

import com.ak.exam.app.dto.ExamHoleAssignmentRequest;
import com.ak.exam.app.model.Department;
import com.ak.exam.app.model.ExamHole;
import com.ak.exam.app.model.ExamHoleAssignment;
import com.ak.exam.app.repo.DepartmentRepository;
import com.ak.exam.app.repo.ExamHoleAssignmentRepository;
import com.ak.exam.app.repo.ExamHoleRepository;
import com.ak.exam.app.dto.UserSeatDTO;
import com.ak.exam.user.model.User;
import com.ak.exam.user.repo.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.springframework.transaction.annotation.Propagation;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ak.exam.user.enums.Role.STUDENT;
import static com.ak.exam.user.enums.Role.TEACHER;

@Service
@Transactional
@RequiredArgsConstructor
public class ExamHoleService {

    private final ExamHoleRepository examHoleRepository;
    private final UserRepository userRepository;
    private final ExamHoleAssignmentRepository assignmentRepository;
    private final DepartmentRepository departmentRepository;

    @Autowired
    private JavaMailSender mailSender;

    // Define a pattern for seat number validation (e.g., "A1", "B2")
    private static final Pattern SEAT_NUMBER_PATTERN = Pattern.compile("^[A-Z]\\d+$");
    private static final Logger logger = LoggerFactory.getLogger(ExamHoleService.class);
    private final ExamHoleAssignmentRepository examHoleAssignmentRepository;

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
    public ResponseEntity<String> addUserToExamHole(Long examHoleId, Long userId, String seatNumber) {
        ExamHole examHole = examHoleRepository.findById(examHoleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam Hall not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Validate and normalize seat number
        if (seatNumber == null || seatNumber.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat number cannot be empty");
        }

        // Normalize seat number - trim and convert to uppercase
        seatNumber = seatNumber.trim().toUpperCase();

        // For teachers, use "Teacher Stage" as the seat number
        if (user.getRole() == TEACHER) {
            seatNumber = "Teacher Stage";
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
                .seatNumber(seatNumber) // Use normalized seat number
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
    public ResponseEntity<String> editUserSeatInExamHole(Long examHoleId, Long userId, String newSeatNumber) {
        ExamHole examHole = examHoleRepository.findById(examHoleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam Hall not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Validate and normalize seat number
        if (newSeatNumber == null || newSeatNumber.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat number cannot be empty");
        }

        // Normalize seat number - trim and convert to uppercase
        newSeatNumber = newSeatNumber.trim().toUpperCase();

        // For teachers, always use "Teacher Stage"
        if (user.getRole() == TEACHER) {
            newSeatNumber = "Teacher Stage";
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
     *
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

                // Set seat number based on role
                String seatNumber;
                if (user.getRole() == TEACHER) {
                    seatNumber = "Teacher Stage";
                } else {
                    seatNumber = String.format("%03d", nextSeatNumber++); // Format: 001, 002, etc.
                }

                // Create and save the assignment
                ExamHoleAssignment assignment = ExamHoleAssignment.builder()
                        .examHole(examHole)
                        .user(user)
                        .seatNumber(seatNumber)
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

            // For teachers, always use "Teacher Stage" as seat number
            String seatNumber = request.getSeatNumber();
            if (user.getRole() == TEACHER) {
                seatNumber = "Teacher Stage";
            }

            // Update the available capacity of the exam hole
            int availableSlots = examHole.getCapacity() - (int) currentAssignments - 1; // Subtract 1 for the new user
            examHole.setAvailableSlots(availableSlots);
            examHoleRepository.save(examHole); // Save the updated exam hole object

            // Create and save assignment
            ExamHoleAssignment assignment = ExamHoleAssignment.builder()
                    .examHole(examHole)
                    .user(user)
                    .seatNumber(seatNumber)
                    .build();

            assignmentRepository.save(assignment);
            logger.info("Assigned userId={} to examHoleId={} at seatNumber={}",
                    user.getId(), examHole.getId(), seatNumber);
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

        // Normalize seat number - trim and convert to uppercase
        seatNumber = seatNumber.trim().toUpperCase();

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
                .seatNumber(seatNumber) // Using the normalized seat number
                .rowNumber(rowNumber + 1)
                .build();
    }

    /**
     * Validates if a seat number is in the correct format
     * Accepts any string, including "Teacher Stage" for teachers
     */
    private boolean isValidSeatNumber(String seatNumber) {
        // Accept any seat number that is not null or empty
        return seatNumber != null && !seatNumber.isEmpty();
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

    /**
     * Dynamically distributes users to seats in an exam hole based on selected departments and level grouping
     *
     * @param examHoleID          The ID of the exam hole to distribute students to
     * @param selectedDepartments List of department names to include in the distribution
     * @param firstLevelGroup     The first level group to assign (e.g., 1 and 3)
     * @param secondLevelGroup    The second level group to assign (e.g., 2 and 4)
     */
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void distributeUsersToSeats(Long examHoleID, List<String> selectedDepartments,
                                       List<Integer> firstLevelGroup, List<Integer> secondLevelGroup) {
        try {
            // Validate exam hole
            ExamHole examHole = examHoleRepository.findById(examHoleID)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam Hall not found"));

            logger.info("Starting seat distribution for Exam Hall: {}", examHole.getHoleName());
            logger.info("First level group: {}, Second level group: {}", firstLevelGroup, secondLevelGroup);

            // Define seating columns as per requirements
            List<String> seatingColumns = Arrays.asList("A", "C", "D", "G", "I", "J", "L");

            // Validate columns are valid
            for (String column : seatingColumns) {
                if (!column.matches("[A-Z]")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid column format: " + column + ". Columns must be uppercase letters.");
                }
            }

            // Group departments by level
            Map<Integer, List<String>> departmentsByLevel = new HashMap<>();

            for (String deptName : selectedDepartments) {
                int level = extractLevel(deptName);
                departmentsByLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(deptName);
                logger.info("Added department {} to level {}", deptName, level);
            }

            // Collect departments for each group based on the provided level groups
            List<String> group1Departments = new ArrayList<>();
            List<String> group2Departments = new ArrayList<>();

            // Collect departments for first level group
            for (Integer level : firstLevelGroup) {
                if (departmentsByLevel.containsKey(level)) {
                    group1Departments.addAll(departmentsByLevel.get(level));
                }
            }

            // Collect departments for second level group
            for (Integer level : secondLevelGroup) {
                if (departmentsByLevel.containsKey(level)) {
                    group2Departments.addAll(departmentsByLevel.get(level));
                }
            }

            logger.info("Group 1 (Levels {}) departments: {}", firstLevelGroup, group1Departments);
            logger.info("Group 2 (Levels {}) departments: {}", secondLevelGroup, group2Departments);

            // Define the maximum number of students
            final int TOTAL_CAPACITY = 56; // 7 columns × 8 rows
            int maxRows = 8;

            // Map to store users by department for efficient lookup
            Map<String, Queue<User>> departmentUsersMap = new HashMap<>();

            // First, assign teachers to "Teacher Stage"
            int teachersAssigned = 0;

            // Collect all users for all departments
            for (String departmentName : selectedDepartments) {
                List<User> allUsers = collectUsersForDepartment(departmentName);

                // Separate teachers and students
                Queue<User> departmentUsers = new LinkedList<>();

                for (User user : allUsers) {
                    if (user.getRole() == TEACHER) {
                        // Assign teachers to "Teacher Stage" immediately
                        try {
                            if (assignUserToSeat(examHole, user, "Teacher Stage", departmentName, new HashSet<>(), new HashMap<>())) {
                                teachersAssigned++;
                                logger.info("Assigned teacher {} to Teacher Stage", user.getFname() + " " + user.getLname());
                            }
                        } catch (Exception e) {
                            logger.error("Error assigning teacher to Teacher Stage: {}", e.getMessage());
                        }
                    } else {
                        // Queue students for later assignment
                        departmentUsers.add(user);
                    }
                }

                departmentUsersMap.put(departmentName, departmentUsers);
                logger.info("Collected {} students for department {}", departmentUsers.size(), departmentName);
            }

            logger.info("Assigned {} teachers to Teacher Stage", teachersAssigned);

            // Set to track emails for notification
            Set<String> userEmails = new HashSet<>();
            Map<Long, String> userSeatMap = new HashMap<>(); // Store user ID -> seat number for emails

            // Initialize counters
            int totalAssigned = teachersAssigned; // Include teachers in total
            int group1DeptIndex = 0;
            int group2DeptIndex = 0;

            // Distribute students alternating between group 1 and group 2
            // Group 1 will be placed in even-indexed columns (0-based): A, D, I, L
            // Group 2 will be placed in odd-indexed columns (0-based): C, G, J
            for (int colIndex = 0; colIndex < seatingColumns.size(); colIndex++) {
                String column = seatingColumns.get(colIndex);
                boolean isEvenColumn = colIndex % 2 == 0;

                List<String> currentDepartments = isEvenColumn ? group1Departments : group2Departments;
                if (currentDepartments.isEmpty()) {
                    logger.info("No departments for column {} (group {})", column, isEvenColumn ? 1 : 2);
                    continue;
                }

                int deptIndex = isEvenColumn ? group1DeptIndex : group2DeptIndex;

                logger.info("Assigning students to column {} from group {}", column, isEvenColumn ? 1 : 2);

                // Assign students row by row in this column
                for (int row = 1; row <= maxRows; row++) {
                    if (totalAssigned >= TOTAL_CAPACITY) {
                        logger.info("Reached maximum capacity of {} students", TOTAL_CAPACITY);
                        break;
                    }

                    // Ensure we have a valid department index
                    if (currentDepartments.isEmpty()) break;
                    deptIndex = deptIndex % currentDepartments.size();

                    String departmentName = currentDepartments.get(deptIndex);
                    Queue<User> users = departmentUsersMap.get(departmentName);

                    // Try to find an available user
                    User userToAssign = findNextAvailableUser(users);

                    // If no user in current department, try others in the same group
                    if (userToAssign == null) {
                        boolean foundUser = false;

                        // Try a complete loop through all departments in this group
                        int originalDeptIndex = deptIndex;
                        do {
                            deptIndex = (deptIndex + 1) % currentDepartments.size();
                            departmentName = currentDepartments.get(deptIndex);
                            users = departmentUsersMap.get(departmentName);
                            userToAssign = findNextAvailableUser(users);

                            if (userToAssign != null) {
                                foundUser = true;
                                break;
                            }
                        } while (deptIndex != originalDeptIndex);

                        if (!foundUser) {
                            logger.debug("No available users for column {} row {}", column, row);
                            continue;
                        }
                    }

                    // Create seat number
                    String seatNumber = column + row;

                    try {
                        if (assignUserToSeat(examHole, userToAssign, seatNumber, departmentName, userEmails, userSeatMap)) {
                            totalAssigned++;
                        }
                    } catch (Exception e) {
                        logger.error("Error assigning user to seat {}: {}", seatNumber, e.getMessage());
                        // Continue with next user
                    }

                    // Move to next department for next row
                    deptIndex = (deptIndex + 1) % currentDepartments.size();
                }

                // Update the appropriate department index
                if (isEvenColumn) {
                    group1DeptIndex = deptIndex;
                } else {
                    group2DeptIndex = deptIndex;
                }
            }

            // Update exam hole available slots in a separate transaction
            updateExamHoleCapacity(examHoleID, totalAssigned);

            // Send email notifications
            if (!userEmails.isEmpty()) {
                sendEmailNotifications(examHole, userEmails, userSeatMap);
            }

            logger.info("Completed seat distribution. Total users assigned: {}", totalAssigned);
        } catch (Exception e) {
            logger.error("Error during seat distribution: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to distribute seats: " + e.getMessage());
        }
    }

    /**
     * Updates the exam hole capacity in a separate transaction
     */
    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateExamHoleCapacity(Long examHoleId, int assignedCount) {
        try {
            ExamHole examHole = examHoleRepository.findById(examHoleId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam Hall not found"));
            examHole.setAvailableSlots(examHole.getCapacity() - assignedCount);
            examHoleRepository.save(examHole);
            logger.info("Updated exam hole {} available slots to {}",
                    examHoleId, examHole.getAvailableSlots());
        } catch (Exception e) {
            logger.error("Error updating exam hole capacity: {}", e.getMessage(), e);
        }
    }

    /**
     * Overloaded method for backward compatibility and convenience using the default level groups (1&3, 2&4)
     */
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void distributeUsersToSeats(Long examHoleID, List<String> selectedDepartments) {
        // Default to the original grouping: Levels 1&3 in first group, Levels 2&4 in second group
        List<Integer> firstLevelGroup = Arrays.asList(1, 3);
        List<Integer> secondLevelGroup = Arrays.asList(2, 4);

        distributeUsersToSeats(examHoleID, selectedDepartments, firstLevelGroup, secondLevelGroup);
    }

    /**
     * Collects users for a specific department, including both STUDENT and TEACHER roles
     * Only includes those not assigned to any exam hole
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    protected List<User> collectUsersForDepartment(String departmentName) {
        Department department = departmentRepository.findByName(departmentName);
        if (department == null) {
            department = departmentRepository.findFirstByNameContainingIgnoreCase(departmentName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Department not found: " + departmentName));
        }

        // Fetch all users from this department (both STUDENTS and TEACHERS)
        List<User> departmentUsers = userRepository.findByDepartment(department);

        // Filter out users who are already assigned to any exam hole
        List<User> unassignedUsers = new ArrayList<>();
        for (User user : departmentUsers) {
            if (!assignmentRepository.existsByUserId(user.getId())) {
                unassignedUsers.add(user);
            }
        }

        logger.info("Found {} unassigned users out of {} total in department {}",
                unassignedUsers.size(), departmentUsers.size(), departmentName);

        return unassignedUsers;
    }

    /**
     * Find the next available user in a queue
     */
    private User findNextAvailableUser(Queue<User> users) {
        if (users == null || users.isEmpty()) {
            return null;
        }

        return users.poll();
    }

    /**
     * Assign a user to a specific seat
     */
    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean assignUserToSeat(ExamHole examHole, User user, String seatNumber,
                                    String departmentName, Set<String> userEmails,
                                    Map<Long, String> userSeatMap) {
        try {
            // For teachers, always use "Teacher Stage"
            if (user.getRole() == TEACHER) {
                seatNumber = "Teacher Stage";
            }

            // Check if seat is already taken
            if (assignmentRepository.existsByExamHoleIdAndSeatNumber(examHole.getId(), seatNumber)) {
                logger.warn("Seat {} is already taken in exam hole {}", seatNumber, examHole.getId());
                return false;
            }

            // Double-check if user is already assigned to any exam hole
            if (assignmentRepository.existsByUserId(user.getId())) {
                logger.warn("User {} is already assigned to a seat in some exam hole", user.getId());
                return false;
            }

            // Create the assignment
            ExamHoleAssignment assignment = ExamHoleAssignment.builder()
                    .examHole(examHole)
                    .user(user)
                    .seatNumber(seatNumber)
                    .build();

            assignmentRepository.save(assignment);

            // Add email for notification
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                userEmails.add(user.getEmail());
                userSeatMap.put(user.getId(), seatNumber);
            }

            logger.info("Assigned user {} (ID: {}) to {} in department {}",
                    user.getFname() + " " + user.getLname(),
                    user.getId(), seatNumber, departmentName);

            return true;
        } catch (Exception e) {
            logger.error("Failed to save assignment: {}", e.getMessage(), e);
            throw e; // Re-throw to be caught by the caller
        }
    }

    /**
     * Helper method to extract level from department name
     */
    private int extractLevel(String departmentName) {
        // Extract the level number from department name (e.g., "IT 4" -> 4)
        String[] parts = departmentName.split(" ");
        for (String part : parts) {
            if (part.matches("\\d+")) {
                return Integer.parseInt(part);
            }
        }
        logger.warn("Could not extract level from department name: {}", departmentName);
        return 0; // Default level if not found
    }

    /**
     * Sends email notifications to students about their seat assignments
     */
    private void sendEmailNotifications(ExamHole examHole, Set<String> userEmails, Map<Long, String> userSeatMap) {
        if (userEmails.isEmpty()) {
            logger.info("No emails to send notifications to");
            return;
        }

        logger.info("Sending email notifications to {} users", userEmails.size());

        try {
            // Create email message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);

            helper.setSubject("Exam Hall Assignment - " + examHole.getHoleName());

            // Use the full setFrom method with sender name
            String fromAddress = "akar.arkanf19@gmail.com";
            String senderName = "Exam Management System";
            helper.setFrom(fromAddress, senderName);

            // Use BCC for all recipients to protect privacy
            String[] emailArray = userEmails.toArray(new String[0]);
            helper.setBcc(emailArray);

            // Create HTML content
            String htmlContent = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>" +
                    "<h2 style='color: #2c3e50; text-align: center;'>Exam Hall Assignment</h2>" +
                    "<p>Dear User,</p>" +
                    "<p>You have been assigned to the upcoming exam in <strong>" +
                    examHole.getHoleName() + "</strong>.</p>" +
                    "<p>Your seat assignment can be found by logging into the exam system.</p>" +
                    "<div style='background-color: #f8f9fa; padding: 15px; margin: 15px 0; border-left: 4px solid #4e73df;'>" +
                    "<p><strong>Exam Location:</strong> " + examHole.getHoleName() + "</p>" +
                    "<p><strong>Hall Number:</strong> " + examHole.getNumber() + "</p>" +
                    "</div>" +
                    "<p>Please remember to:</p>" +
                    "<ul>" +
                    "<li>Bring your ID card</li>" +
                    "<li>Arrive at least 30 minutes before the exam starts</li>" +
                    "<li>Turn off mobile phones before entering the exam hall</li>" +
                    "<li>Bring necessary stationery (pens, pencils, calculator if permitted)</li>" +
                    "</ul>" +
                    "<p>Good luck on your exam!</p>" +
                    "<p style='text-align: center; margin-top: 30px; font-size: 12px; color: #6c757d;'>This is an automated message. Please do not reply to this email.</p>" +
                    "</div></body></html>";

            helper.setText(htmlContent, true);

            // Send the email
            mailSender.send(message);

            logger.info("Successfully sent email notifications to users");
        } catch (MessagingException e) {
            logger.error("Failed to send email notifications: {}", e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Failed to set sender name: {}", e.getMessage(), e);
        }
    }
}