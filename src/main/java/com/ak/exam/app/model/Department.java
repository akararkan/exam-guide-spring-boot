package com.ak.exam.app.model;

import com.ak.exam.user.model.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "department_tbl")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Integer level;
    private Date createdAt;
    private Date updatedAt;@ManyToOne(fetch = FetchType.LAZY , optional = false)
    @JoinColumn(name = "semesterSchedule_id" , nullable = false)
    @JsonBackReference("semesterSchedule-course")
    private SemesterSchedule semesterSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-course")
    private User user;

    // One Department can have many Users
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    private List<User> users; // List of Users in this Department


    @OneToMany(mappedBy = "department",cascade = CascadeType.ALL,fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonBackReference("department-course")
    @JsonIgnore
    private List<Course> courses;

}
