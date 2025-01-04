// src/main/java/com/ak/exam/app/dto/UserSeatDTO.java

package com.ak.exam.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSeatDTO {
    private Long userId;
    private String fname;
    private String lname;
    private String email;
    private String seatNumber;
}
