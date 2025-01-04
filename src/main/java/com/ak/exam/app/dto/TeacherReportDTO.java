package com.ak.exam.app.dto;

import com.ak.exam.user.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherReportDTO {
    private Long id;


    private String reportCode;

    private String reportHeader;

    private String reportBody;
    private Date createdAt;
    private Date updatedAt;
    private UserDTO user;
}
