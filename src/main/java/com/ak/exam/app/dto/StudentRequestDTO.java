package com.ak.exam.app.dto;

import com.ak.exam.app.enums.RequestStatus;
import com.ak.exam.user.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentRequestDTO {

    private Long id;
    private String requestHeader;
    private String requestBody;
    private Date requestDate;
    private RequestStatus requestStatus;
    private Date createdAt;
    private Date updatedAt;
    private String attachmentFile; // Store image URL as a string


    private UserDTO user; // Include UserDTO for associated user information
}
