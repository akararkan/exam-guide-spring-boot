package com.ak.exam.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String fname;
    private String lname;
    private String username;
    private String phone;
    private String password;
    private String email;
    private String role;
    private Long departmentId; // Add this field
    private Date joinDate;
    private Date lastLoginDate;
}
