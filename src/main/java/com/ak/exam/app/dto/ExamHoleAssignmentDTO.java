package com.ak.exam.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExamHoleAssignmentDTO {

    private Long id;
    private Long examHoleId;
    private Long userId;
    private String seatNumber;

    // Constructors, getters, and setters
}
