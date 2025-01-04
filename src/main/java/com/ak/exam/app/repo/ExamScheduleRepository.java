package com.ak.exam.app.repo;

import com.ak.exam.app.model.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
}
