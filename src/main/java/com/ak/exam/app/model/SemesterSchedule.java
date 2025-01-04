package com.ak.exam.app.model;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "semester_schedule_tbl")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SemesterSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String semesterName;
    private Date startDate;
    private Date endDate;
    private Date createdAt;
    private Date updatedAt;

    @OneToMany(mappedBy = "semesterSchedule" , fetch = FetchType.LAZY , cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("semesterSchedule-course")
    private List<Course> courses;
}
