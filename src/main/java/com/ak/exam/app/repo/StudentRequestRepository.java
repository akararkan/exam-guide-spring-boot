package com.ak.exam.app.repo;

import com.ak.exam.app.enums.RequestStatus;
import com.ak.exam.app.model.StudentRequest;
import com.ak.exam.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRequestRepository extends JpaRepository<StudentRequest, Long> {
    /**
     * Find all requests by user entity
     */
    List<StudentRequest> findByUser(User user);

    /**
     * Find all requests by user ID
     */
    List<StudentRequest> findByUserId(Long userId);

    /**
     * Find all requests by user ID and filter by status
     */
    List<StudentRequest> findByUserIdAndRequestStatus(Long userId, RequestStatus requestStatus);

    /**
     * Find all requests by user entity and filter by status
     */
    List<StudentRequest> findByUserAndRequestStatus(User user, RequestStatus requestStatus);

    /**
     * Custom query to fetch requests with optional sorting
     */
    @Query("SELECT sr FROM StudentRequest sr WHERE sr.user.id = :userId ORDER BY sr.requestDate DESC")
    List<StudentRequest> findUserRequestsOrderByDateDesc(@Param("userId") Long userId);

}
