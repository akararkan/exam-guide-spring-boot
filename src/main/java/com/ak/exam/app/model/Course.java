package com.ak.exam.app.model;

import com.ak.exam.user.model.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "course_tbl")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Integer year;
    private Date createdAt;
    private Date updatedAt;

    @ManyToOne(fetch = FetchType.LAZY , optional = false)
    @JoinColumn(name = "department_id" , nullable = false)
    @JsonBackReference("department-course")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY , optional = false)
    @JoinColumn(name = "examSchedule_id" , nullable = false)
    @JsonBackReference("examSchedule-course")
    private ExamSchedule examSchedule;


    @ManyToOne(fetch = FetchType.LAZY , optional = false)
    @JoinColumn(name = "semesterSchedule_id" , nullable = false)
    @JsonBackReference("semesterSchedule-course")
    private SemesterSchedule semesterSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-course")
    private User user;
}
