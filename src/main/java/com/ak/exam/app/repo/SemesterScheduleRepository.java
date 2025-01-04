package com.ak.exam.app.repo;

import com.ak.exam.app.model.SemesterSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SemesterScheduleRepository extends JpaRepository<SemesterSchedule, Long> {

}
