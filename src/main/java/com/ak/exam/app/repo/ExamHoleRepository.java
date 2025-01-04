package com.ak.exam.app.repo;

import com.ak.exam.app.model.ExamHole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamHoleRepository extends JpaRepository<ExamHole,  Long> {
    List<ExamHole> findByAvailableSlotsGreaterThan(Integer slots);
}
