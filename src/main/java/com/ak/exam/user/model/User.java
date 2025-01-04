package com.ak.exam.user.model;


import com.ak.exam.app.model.*;
import com.ak.exam.user.enums.Role;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Entity
@Data
@Table(name = "user_tbl")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User implements Serializable , UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String fname;
    private String lname;
    private String username;
    private String email;
    private String password;
    private String phone;
    private Role role;
    private List<String> authorities;
    private boolean isActive;
    private boolean isEnabled;
    private boolean isNotLocked;
    private String verificationCode;
    private boolean isVerified;
    private Date joinDate;
    private Date lastLoginDate;
    private Date createDate;
    private Date lastUpdateDate;

    // One-to-Many Relationship with TeacherReport
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @JsonIgnore
    private List<TeacherReport> teacherReports;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference // Prevent circular references in JSON serialization
    @JsonIgnore
    private List<StudentRequest> studentRequests; // List of requests created by the user

    // Many Users belong to one Department
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id") // This is the foreign key column in the User table

    private Department department;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("examhole-assignments")
    @JsonIgnore
    private List<ExamHoleAssignment> assignments;

    // One User has many Courses
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("user-course")
    @JsonIgnore
    private List<Course> courses;




    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAuthorities();
    }


    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    public boolean isEnabled(boolean isEnabled) {
        return this.isEnabled = isEnabled;
    }
    public boolean isVerified(boolean isVerified) {
        return this.isVerified = isVerified;
    }


}
