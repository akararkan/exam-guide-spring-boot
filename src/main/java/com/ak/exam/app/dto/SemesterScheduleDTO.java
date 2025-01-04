package com.ak.exam.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SemesterScheduleDTO {
    private Long id;
    private String semesterName;
    private Date startDate;
    private Date endDate;
}
