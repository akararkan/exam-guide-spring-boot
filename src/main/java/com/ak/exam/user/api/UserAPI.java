package com.ak.exam.user.api;

import com.ak.exam.app.model.Department;
import com.ak.exam.app.repo.DepartmentRepository;
import com.ak.exam.user.dto.UserDTO;
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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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


}
