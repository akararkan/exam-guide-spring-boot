package com.ak.exam.app.repo;

import com.ak.exam.app.model.StudentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRequestRepository extends JpaRepository<StudentRequest, Long> {

}
