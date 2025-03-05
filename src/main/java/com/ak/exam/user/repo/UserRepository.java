package com.ak.exam.user.repo;
import com.ak.exam.app.model.Department;
import com.ak.exam.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    User findUserByUsername(String username);

    User findUserByEmail(String email);
    Optional<User> findByEmail(String email);

    User findByVerificationCode(String verificationCode);

    Optional<User> findByUsername(String username);
    List<User> findByDepartment(Department department);
    @Query("SELECT u FROM User u LEFT JOIN ExamHoleAssignment eha ON u = eha.user WHERE u.department = :department AND eha.user IS NULL")
    List<User> findByDepartmentAndNotAssigned(@Param("department") Department department);
}