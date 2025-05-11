package com.ak.exam.user.api;

import com.ak.exam.app.model.Department;
import com.ak.exam.app.repo.DepartmentRepository;
import com.ak.exam.user.dto.UserDTO;
import com.ak.exam.user.enums.Role;
import com.ak.exam.user.jwt.Token;
import com.ak.exam.user.model.User;
import com.ak.exam.user.repo.UserRepository;
import com.ak.exam.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserAPI {

    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserAPI.class);
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @PostMapping("/addAllUsers")
    public ResponseEntity<List<Token>> addAllUsers(@RequestBody List<User> users) {
        try {
            logger.info("Starting to add {} users", users.size());

            // Validate users
            if (users.isEmpty()) {
                logger.warn("Empty user list provided");
                return ResponseEntity.badRequest()
                        .body(List.of(new Token(null, "No users provided")));
            }

            List<User> newUsers = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (User user : users) {
                try {
                    // Check if user has department set
                    if (user.getDepartment() == null || user.getDepartment().getId() == null) {
                        logger.warn("User {} has no department", user.getEmail());
                        errors.add("User " + user.getEmail() + " has no department");
                        continue;
                    }

                    Long departmentId = user.getDepartment().getId();
                    logger.info("Processing user {} with department ID: {}", user.getEmail(), departmentId);

                    // Fetch the department for this specific user
                    Department department = departmentRepository.findById(departmentId)
                            .orElse(null);

                    if (department == null) {
                        logger.warn("Department not found with ID: {}", departmentId);
                        errors.add("Department not found with ID: " + departmentId + " for user " + user.getEmail());
                        continue;
                    }

                    logger.info("Found department: {} for user: {}", department.getName(), user.getEmail());

                    // Create user with correct department
                    User newUser = User.builder()
                            .fname(user.getFname())
                            .lname(user.getLname())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .password(user.getPassword())
                            .phone(user.getPhone())
                            .joinDate(new Date())
                            .isActive(true)
                            .isNotLocked(true)
                            .isEnabled(false)
                            .isVerified(false)
                            .role(user.getRole())
                            .authorities(Collections.singletonList(user.getRole().getAuthorities().toString()))
                            .department(department)  // Using the department found for this specific user
                            .createDate(new Date())
                            .build();

                    newUsers.add(newUser);
                    logger.debug("Added user {} to the batch with department {}",
                            newUser.getEmail(), newUser.getDepartment().getName());

                } catch (Exception e) {
                    logger.error("Error processing user {}: {}", user.getEmail(), e.getMessage());
                    errors.add("Error processing user " + user.getEmail() + ": " + e.getMessage());
                }
            }

            if (newUsers.isEmpty()) {
                logger.warn("No valid users to add after processing");
                return ResponseEntity.badRequest()
                        .body(List.of(new Token(null, "No valid users to add. Errors: " + String.join(", ", errors))));
            }

            // Save all valid users in batch
            logger.info("Saving {} users to database", newUsers.size());
            List<User> savedUsers = userRepository.saveAll(newUsers);
            logger.info("Successfully saved {} users", savedUsers.size());

            // Generate tokens for each user
            List<Token> responses = savedUsers.stream().map(user -> Token.builder()
                    .token(null)
                    .response("User " + user.getEmail() + " added successfully with department " +
                            user.getDepartment().getName())
                    .build()).toList();

            // If there were errors, add them to the response
            if (!errors.isEmpty()) {
                responses = new ArrayList<>(responses);
                responses.add(new Token(null, "Warnings: " + String.join(", ", errors)));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(responses);

        } catch (Exception e) {
            logger.error("Error adding users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(new Token(null, "Error adding users: " + e.getMessage())));
        }
    }

    @PostMapping("/register/{departmentId}")
    public ResponseEntity<Token> register(@RequestBody User user, @PathVariable Long departmentId, HttpServletRequest request) throws Exception {
        return userService.createUser(user, getSiteURL(request), departmentId);
    }

    private String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }

    @GetMapping("/verify")
    public String verifyUser(@Param("code") String code) {
        logger.info("Received verification code: {}", code);

        if (userService.verify(code)) {
            logger.info("Verification successful for code: {}", code);
            return "<h1>verify_success</h1>";
        } else {
            logger.warn("Verification failed for code: {}", code);
            return "verify_fail";
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Token> login(@RequestBody User user) {
        return userService.login(user);
    }

    @GetMapping("/getAllUsers")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();

        List<UserDTO> userDTOs = users.stream()
                .map(user -> UserDTO.builder()
                        .id(user.getId())
                        .fname(user.getFname())
                        .lname(user.getLname())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .password(user.getPassword())
                        .role(user.getRole().name())
                        .username(user.getUsername())
                        .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                        .joinDate(new Date())
                        .lastLoginDate(user.getLastLoginDate())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(userDTOs);
    }

    @GetMapping("/getUserById/{id}")
    public ResponseEntity<Optional<User>> getUserById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(userRepository.findById(id));
    }

    @PutMapping("/updateUserById/{id}/{departmentId}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user, @PathVariable Long departmentId) {
        User updatedUser = userService.updateUser(id, user, departmentId);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/deleteUserById/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<Token> resetPassword(@RequestParam String email, @RequestParam String newPassword) {
        return userService.resetPassword(email, newPassword);
    }

    @GetMapping("/exportUsers")
    public ResponseEntity<byte[]> exportUsersToExcel() {
        try {
            List<User> users = userRepository.findAll();

            // Create workbook and sheet
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Users");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                    "ID", "First Name", "Last Name", "Username", "Email",
                    "Phone", "Role", "Department", "Join Date", "Is Active",
                    "Is Enabled", "Is Verified", "Is Not Locked", "Create Date"
            };

            // Style for header
            CellStyle headerStyle = workbook.createCellStyle();
            XSSFFont font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Create header cells
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 1;
            for (User user : users) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(user.getId() != null ? user.getId() : 0);
                row.createCell(1).setCellValue(user.getFname() != null ? user.getFname() : "");
                row.createCell(2).setCellValue(user.getLname() != null ? user.getLname() : "");
                row.createCell(3).setCellValue(user.getUsername() != null ? user.getUsername() : "");
                row.createCell(4).setCellValue(user.getEmail() != null ? user.getEmail() : "");
                row.createCell(5).setCellValue(user.getPhone() != null ? user.getPhone() : "");
                row.createCell(6).setCellValue(user.getRole() != null ? user.getRole().name() : "");
                row.createCell(7).setCellValue(user.getDepartment() != null ? user.getDepartment().getName() : "");

                // Date formatting
                CellStyle dateStyle = workbook.createCellStyle();
                CreationHelper createHelper = workbook.getCreationHelper();
                dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));

                Cell joinDateCell = row.createCell(8);
                if (user.getJoinDate() != null) {
                    joinDateCell.setCellValue(user.getJoinDate());
                    joinDateCell.setCellStyle(dateStyle);
                }

                row.createCell(9).setCellValue(user.isActive());
                row.createCell(10).setCellValue(user.isEnabled());
                row.createCell(11).setCellValue(user.isVerified());
                row.createCell(12).setCellValue(user.isNotLocked());

                Cell createDateCell = row.createCell(13);
                if (user.getCreateDate() != null) {
                    createDateCell.setCellValue(user.getCreateDate());
                    createDateCell.setCellStyle(dateStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            // Prepare response
            byte[] excelContent = outputStream.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "users.xlsx");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            headers.setContentLength(excelContent.length);

            return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error exporting users to Excel: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/importUsers")
    public ResponseEntity<List<Token>> importUsersFromExcel(@RequestPart("file") MultipartFile file) {
        try {
            logger.info("Starting Excel import process");
            // Load workbook
            XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
            XSSFSheet sheet = workbook.getSheetAt(0);

            List<User> users = new ArrayList<>();
            Map<String, Department> departmentCache = new HashMap<>();
            int totalRows = sheet.getLastRowNum();

            logger.info("Found {} rows in the Excel sheet", totalRows);

            // Assuming first row is header
            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    logger.debug("Row {} is null, skipping", i);
                    continue;
                }

                logger.info("Processing row: {}", i);

                // Read department name - assuming it's in column 7
                Cell departmentCell = row.getCell(7);
                Department department = null;
                String departmentName = null;

                if (departmentCell != null) {
                    departmentName = getCellStringValue(departmentCell);
                    logger.info("Row {}: Department value: '{}'", i, departmentName);

                    if (departmentName != null && !departmentName.isEmpty()) {
                        // Use cached department if we've seen this name before
                        if (departmentCache.containsKey(departmentName)) {
                            department = departmentCache.get(departmentName);
                            logger.info("Row {}: Using cached department '{}' with ID {}",
                                    i, departmentName, department.getId());
                        } else {
                            // Find the department by name
                            department = departmentRepository.findByName(departmentName);

                            if (department != null) {
                                // Cache it for future rows
                                departmentCache.put(departmentName, department);
                                logger.info("Row {}: Found department '{}' with ID {}",
                                        i, departmentName, department.getId());
                            } else {
                                logger.warn("Row {}: Department '{}' not found in database", i, departmentName);
                            }
                        }
                    } else {
                        logger.warn("Row {}: Empty department name", i);
                    }
                }

                // Create user from row data
                String roleStr = getCellStringValue(row.getCell(6));
                Role role = null;
                if (roleStr != null && !roleStr.isEmpty()) {
                    try {
                        role = Role.valueOf(roleStr);
                        logger.info("Row {}: Role value: '{}'", i, roleStr);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Row {}: Invalid role value: '{}'", i, roleStr);
                    }
                }

                String fname = getCellStringValue(row.getCell(1));
                String lname = getCellStringValue(row.getCell(2));
                String username = getCellStringValue(row.getCell(3));
                String email = getCellStringValue(row.getCell(4));
                String phone = getCellStringValue(row.getCell(5));
                String password = getCellStringValue(row.getCell(8));

                logger.info("Row {}: Creating user: {}, {}, {}", i, fname, lname, email);

                User user = User.builder()
                        .fname(fname)
                        .lname(lname)
                        .username(username)
                        .email(email)
                        .phone(phone)
                        .role(role)
                        .department(department) // This will be the specific department for this user
                        .password(password)
                        .joinDate(new Date())
                        .isActive(true)
                        .isNotLocked(true)
                        .isEnabled(false)
                        .isVerified(true)
                        .createDate(new Date())
                        .build();

                if (department != null) {
                    logger.info("Row {}: User {} assigned to department {} with ID {}",
                            i, email, department.getName(), department.getId());
                } else {
                    logger.warn("Row {}: User {} has no department assigned", i, email);
                }

                users.add(user);
            }

            workbook.close();

            logger.info("Excel processing complete. Created {} user objects", users.size());

            if (users.isEmpty()) {
                logger.warn("No valid users found in the Excel file");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(List.of(new Token(null, "No valid users found in the Excel file")));
            }

            // Add the users using the improved method
            return addAllUsers(users);

        } catch (Exception e) {
            logger.error("Error importing users from Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(new Token(null, "Error importing users: " + e.getMessage())));
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        // Check if it's a whole number
                        double value = cell.getNumericCellValue();
                        if (value == Math.floor(value)) {
                            return String.valueOf((long) value);
                        } else {
                            return String.valueOf(value);
                        }
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        return cell.getStringCellValue().trim();
                    } catch (Exception e) {
                        try {
                            return String.valueOf(cell.getNumericCellValue());
                        } catch (Exception ex) {
                            return "";
                        }
                    }
                default:
                    return "";
            }
        } catch (Exception e) {
            logger.error("Error reading cell value: {}", e.getMessage(), e);
            return "";
        }
    }
}