package com.ak.exam.app.model;

import com.ak.exam.app.enums.RequestStatus;
import com.ak.exam.user.model.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "student_request_tbl")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String requestHeader;
    private String requestBody;
    private Date requestDate;
    private RequestStatus requestStatus;
    private Date createdAt;
    private Date updatedAt;
    private String attachmentFile; // Store image URL as a string

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference // Prevent circular references in JSON serialization
    private User user; // Reference to the User who created the request
}
