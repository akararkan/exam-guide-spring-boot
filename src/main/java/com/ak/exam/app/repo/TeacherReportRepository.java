package com.ak.exam.app.repo;

import com.ak.exam.app.model.TeacherReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherReportRepository extends JpaRepository<TeacherReport, Long> {
    List<TeacherReport> findByUserId(Long userId);
}
