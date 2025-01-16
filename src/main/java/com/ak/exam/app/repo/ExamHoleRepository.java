package com.ak.exam.app.repo;

import com.ak.exam.app.model.ExamHole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExamHoleRepository extends JpaRepository<ExamHole,  Long> {
    List<ExamHole> findByAvailableSlotsGreaterThan(Integer slots);

}
