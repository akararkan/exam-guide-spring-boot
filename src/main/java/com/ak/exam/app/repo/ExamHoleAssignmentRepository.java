// src/main/java/com/ak/exam/app/repository/ExamHoleAssignmentRepository.java

package com.ak.exam.app.repo;

import com.ak.exam.app.model.ExamHole;
import com.ak.exam.app.model.ExamHoleAssignment;
import com.ak.exam.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface ExamHoleAssignmentRepository extends JpaRepository<ExamHoleAssignment, Long> {
    Optional<ExamHoleAssignment> findByExamHoleIdAndSeatNumber(Long examHoleId, String seatNumber);

    Optional<ExamHoleAssignment> findByExamHoleIdAndUserId(Long examHoleId, Long userId);

    List<ExamHoleAssignment> findByUserId(Long userId);

    List<ExamHoleAssignment> findByExamHoleId(Long examHoleId);

    boolean existsByExamHoleAndUser(ExamHole examHole, User user);

    @Query("SELECT MAX(a.seatNumber) FROM ExamHoleAssignment a WHERE a.examHole.id = :examHoleId")
    Optional<String> findMaxSeatNumberByExamHoleId(@Param("examHoleId") Long examHoleId);

    boolean existsByUserId(Long userId);


    boolean existsByExamHoleIdAndSeatNumber(Long examHoleId, String seatNumber);
    long countByExamHoleId(Long examHoleId);
    boolean existsByExamHoleAndSeatNumber(ExamHole examHole, String seatNumber);
}
