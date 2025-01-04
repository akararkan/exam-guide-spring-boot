// src/main/java/com/ak/exam/app/model/ExamHoleAssignment.java

package com.ak.exam.app.model;

import com.ak.exam.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "exam_hole_assignment_tbl",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"exam_hole_id", "seat_number"})}
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExamHoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_hole_id", nullable = false)
    private ExamHole examHole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "seat_number", length = 10) // Set appropriate length
    private String seatNumber; // Changed from Integer to String
}
