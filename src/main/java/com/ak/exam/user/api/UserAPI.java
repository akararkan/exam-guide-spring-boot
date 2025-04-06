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
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @PostMapping("/addAllUsers")
    public ResponseEntity<List<Token>> addAllUsers(@RequestBody List<User> users) {
        try {
            // Ensure all users have a department
            if (users.isEmpty() || users.get(0).getDepartment() == null || users.get(0).getDepartment().getId() == null) {
                return ResponseEntity.badRequest()
                        .body(List.of(new Token(null, "Department ID is required for all users")));
            }

            Long departmentId = users.get(0).getDepartment().getId(); // Extract department ID from the first user

            // Fetch the department using the departmentId
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found with ID: " + departmentId));

            // Transform each user using the Builder pattern
            List<User> newUsers = users.stream().map(user ->
                    User.builder()
                            .fname(user.getFname())
                            .lname(user.getLname())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .password(user.getPassword()) // Ensure encryption if needed
                            .phone(user.getPhone())
                            .joinDate(new Date())
                            .isActive(true)
                            .isNotLocked(true)
                            .isEnabled(false)
                            .isVerified(false)
                            .role(user.getRole())
                            .authorities(Collections.singletonList(user.getRole().getAuthorities().toString()))
                            .department(department)
                            .createDate(new Date())
                            .build()
            ).toList();

            // Save all users in batch
            userRepository.saveAll(newUsers);

            // Generate tokens for each user (if applicable)
            List<Token> responses = newUsers.stream().map(user -> Token.builder()
                    .token(null) // Generate token if needed
                    .response("User " + user.getEmail() + " added successfully.")
                    .build()).toList();

            return ResponseEntity.status(HttpStatus.CREATED).body(responses);

        } catch (Exception e) {
            logger.error("Error adding users: {}", e.getMessage());
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
                        .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null) // Map department ID
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
    // Add the new endpoint for resetting the password
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
            logger.error("Error exporting users to Excel: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/importUsers")
    public ResponseEntity<List<Token>> importUsersFromExcel(@RequestPart("file") MultipartFile file) {
        try {
            // Load workbook
            XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
            XSSFSheet sheet = workbook.getSheetAt(0);

            List<User> users = new ArrayList<>();

            // Assuming first row is header
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Read department ID - assuming it's in column 7
                String departmentName = row.getCell(7) != null ?
                        row.getCell(7).getStringCellValue() : null;

                Department department = null;
                if (departmentName != null && !departmentName.isEmpty()) {
                    department = departmentRepository.findByName(departmentName);
                }

                // Create user from row data
                User user = User.builder()
                        .fname(getCellStringValue(row.getCell(1)))
                        .lname(getCellStringValue(row.getCell(2)))
                        .username(getCellStringValue(row.getCell(3)))
                        .email(getCellStringValue(row.getCell(4)))
                        .phone(getCellStringValue(row.getCell(5)))
                        .role(getCellStringValue(row.getCell(6)) != null ?
                                Role.valueOf(getCellStringValue(row.getCell(6))) : null)
                        .department(department)
                        .joinDate(new Date())
                        .isActive(true)
                        .isNotLocked(true)
                        .isEnabled(false)
                        .isVerified(false)
                        .createDate(new Date())
                        .build();

                users.add(user);
            }

            workbook.close();

            // Use your existing method to add the users
            return addAllUsers(users);

        } catch (Exception e) {
            logger.error("Error importing users from Excel: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(new Token(null, "Error importing users: " + e.getMessage())));
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return null;
        }
    }


}
