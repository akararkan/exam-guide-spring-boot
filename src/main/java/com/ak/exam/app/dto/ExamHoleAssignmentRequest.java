package com.ak.exam.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExamHoleAssignmentRequest {
    private Long userId;
    private Long examHoleId;
    private String seatNumber;
    private Integer rowNumber;
}
