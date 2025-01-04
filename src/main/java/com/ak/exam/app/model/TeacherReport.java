package com.ak.exam.app.model;

import com.ak.exam.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "teacher_report_tbl")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeacherReport {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String reportCode;
    private String reportHeader;
    private String reportBody;
    private Date createdAt;
    private Date updatedAt;

    // Many-to-One Relationship with User
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}
