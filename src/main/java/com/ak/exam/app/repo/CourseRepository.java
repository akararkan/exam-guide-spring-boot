package com.ak.exam.app.repo;

import com.ak.exam.app.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // Method to search courses by name (case insensitive)
    List<Course> findByNameContainingIgnoreCase(String name);

    Long id(Long id);
    List<Course> findByUserId(Long userId);
}
