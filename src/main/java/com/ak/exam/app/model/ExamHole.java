// src/main/java/com/ak/exam/app/model/ExamHole.java

package com.ak.exam.app.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "exam_hole_tbl")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExamHole {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Integer number;
    private String holeName;
    private Integer capacity;
    private Integer availableSlots;
    private Integer row;
    private Integer col;
    private Date createdAt;
    private Date updatedAt;

    @OneToMany(mappedBy = "examHole", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference("examhole-assignments")
    private List<ExamHoleAssignment> assignments;
}
