package com.ak.exam.app.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "exam_schedule_tbl")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExamSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Date examDate;
    private LocalTime startExamTime; // Added 'examTime' field
    private LocalTime endExamTime;
    private Date createdAt;
    private Date updatedAt;

    @OneToMany(mappedBy = "examSchedule",cascade = CascadeType.ALL, fetch = FetchType.LAZY , orphanRemoval = true)
    @JsonBackReference("examSchedule-course")
    private List<Course> courses;


}
