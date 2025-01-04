package com.ak.exam.user.service;

import com.ak.exam.app.dto.DepartmentDTO;
import com.ak.exam.app.model.Department;
import com.ak.exam.app.repo.DepartmentRepository;
import com.ak.exam.user.dto.UserDTO;
import com.ak.exam.user.enums.Role;
import com.ak.exam.user.exceptions.InvalidPasswordException;
import com.ak.exam.user.exceptions.UserNotFoundException;
import com.ak.exam.user.exceptions.UserNotVerifiedException;
import com.ak.exam.user.jwt.JwtTokenProvider;
import com.ak.exam.user.jwt.Token;
import com.ak.exam.user.model.User;
import com.ak.exam.user.repo.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.utility.RandomString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
@Transactional
@Qualifier("UserDetailsService")
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    @Autowired
    private JavaMailSender javaMailSender;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("NO USER FOUND BY USERNAME" + username);
        } else {
            user.setLastLoginDate(user.getLastLoginDate());
            user.setLastLoginDate(new Date());
            userRepository.save(user);
            return user;
        }
    }

    public ResponseEntity<Token> createUser(User user, String siteURL, Long departmentId) {
        try {
            // Generate a random verification code
            String randomCode = RandomString.make(64);
            user.setVerificationCode(randomCode);

            // Fetch the department using the departmentId
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            // Create a new User entity
            User newUser = User.builder()
                    .id(user.getId())
                    .fname(user.getFname())
                    .lname(user.getLname())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .password(passwordEncoder.encode(user.getPassword()))
                    .joinDate(new Date())
                    .isActive(true)
                    .isNotLocked(true)
                    .isEnabled(false)
                    .isVerified(false)
                    .role(user.getRole())
                    .authorities(Collections.singletonList(user.getRole().getAuthorities().toString()))
                    .verificationCode(user.getVerificationCode())
                    .department(department)
                    .createDate(new Date())
                    .department(department) // Assign the department to the user
                    .build();



            // Generate JWT token
            String jwtToken = jwtTokenProvider.generateToken(newUser);


            userRepository.save(newUser);
            // Create a response with the token and success message
            Token generatedToken = Token.builder()
                    .token(jwtToken)
                    .response("Registration successful. Please check your email to verify your account.")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(generatedToken);

        } catch (Exception e) {
            // Handle exceptions and return an error response
            Token errorToken = Token.builder()
                    .token(null)
                    .response("Registration failed: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorToken);
        }
    }

    private void sendVerificationEmail(User user, String siteURL)
            throws MessagingException, UnsupportedEncodingException, MessagingException, UnsupportedEncodingException {
        String toAddress = user.getEmail();
        String fromAddress = "akar.arkanf19@gmail.com";
        String senderName = "Kargay Hunar";
        String subject = "Please verify your registration";
        String content = "Dear [[name]],<br><br>"
                + "<div style=\"background-color: #f9f9f9; padding: 20px; border-radius: 5px;\">"
                + "    <p style=\"font-size: 16px; color: #333333;\">Please click the link below to verify your registration:</p>"
                + "    <p style=\"text-align: center;\">"
                + "        <a href=\"[[URL]]\" style=\""
                + "            display: inline-block;"
                + "            background-color: #007bff;"
                + "            color: #ffffff;"
                + "            text-decoration: none;"
                + "            padding: 10px 20px;"
                + "            border-radius: 5px;"
                + "            margin-top: 15px;"
                + "        \" target=\"_self\">VERIFY</a>"
                + "    </p>"
                + "</div>"
                + "<br>"
                + "<p style=\"font-size: 14px; color: #777777;\">Thank you,<br>Hunar factory for Marble Stone.</p>";


        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(fromAddress, senderName);
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", user.getFname() + " " + user.getLname());
        String verifyURL = siteURL + "/api/v1/user/verify?code=" + user.getVerificationCode();

        content = content.replace("[[URL]]", verifyURL);

        helper.setText(content, true);

        javaMailSender.send(message);

        System.out.println("Email has been sent");
    }

    public boolean verify(String verificationCode) {
        User user = userRepository.findByVerificationCode(verificationCode);

        if (user == null) {
            logger.error("User not found for verification code: {}", verificationCode);
            return false;
        }

        if (user.isEnabled()) {
            logger.warn("User already enabled for verification code: {}", verificationCode);
            return false;
        }

        // Verification successful, update user status
        user.setVerificationCode(null);
        user.setEnabled(true);
        user.isVerified(true);
        userRepository.save(user);

        logger.info("User verified successfully for verification code: {}", verificationCode);
        return true;
    }

    public ResponseEntity<Token> login(User user) {
        try {
            Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());

            if (optionalUser.isPresent()) {
                User foundUser = optionalUser.get();

                if (passwordEncoder.matches(user.getPassword(), foundUser.getPassword())) {
                    String jwtToken = jwtTokenProvider.generateToken(foundUser);
                    return ResponseEntity.ok(new Token(jwtToken, "User Successfully logged in"));
                } else {
                    // Handle invalid password case
                    throw new InvalidPasswordException("Invalid password");
                }
            } else {
                // Handle the case where user is not found
                throw new UserNotFoundException("User not found");
            }
        } catch (UserNotFoundException e) {
            // Log exception
            logger.error("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Token(e.getMessage()));
        } catch (InvalidPasswordException e) {
            // Log exception
            logger.error("Invalid password: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Token(e.getMessage()));
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    public void deleteUser(Long id) {
        // Check if user exists
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id)); // Handle user not found

        // Delete user
        userRepository.delete(existingUser);
    }
    // Update user by ID
    public User updateUser(Long id, User user , Long departmentId) {

        // Fetch the department using the departmentId
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));


        // Check if user exists
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id)); // Handle user not found

        // Update specific fields
        existingUser.setFname(user.getFname());
        existingUser.setLname(user.getLname());
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setPassword(user.getPassword());  // Handle password encryption separately if needed
        existingUser.setPhone(user.getPhone());
        existingUser.setRole(user.getRole());
        existingUser.setDepartment(department);
        existingUser.setLastUpdateDate(new Date()); // Set last update date

        // Save and return updated user
        return userRepository.save(existingUser);
    }





}
