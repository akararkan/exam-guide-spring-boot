package com.ak.exam.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CourseDTO {
    private Long id;
    private String name;
    private String description;
    private Integer year;
    private DepartmentDTO department;
    private ExamScheduleDTO examSchedule;
    private SemesterScheduleDTO semesterSchedule;
    private Long userId; // Include the userId
}
