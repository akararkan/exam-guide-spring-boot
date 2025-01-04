package com.ak.exam.app.service;

import com.ak.exam.app.dto.CourseDTO;
import com.ak.exam.app.dto.DepartmentDTO;
import com.ak.exam.app.dto.ExamScheduleDTO;
import com.ak.exam.app.dto.SemesterScheduleDTO;
import com.ak.exam.app.model.Course;
import com.ak.exam.app.model.Department;
import com.ak.exam.app.model.ExamSchedule;
import com.ak.exam.app.model.SemesterSchedule;
import com.ak.exam.app.repo.CourseRepository;
import com.ak.exam.app.repo.DepartmentRepository;
import com.ak.exam.app.repo.ExamScheduleRepository;
import com.ak.exam.app.repo.SemesterScheduleRepository;
import com.ak.exam.user.model.User;
import com.ak.exam.user.repo.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
@Transactional
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final SemesterScheduleRepository semesterScheduleRepository;
    private final UserRepository userRepository;

    /**
     * Adds a new course and returns the created CourseDTO.
     *
     * @param course    The course details.
     * @param userId    The ID of the user.
     * @param deptId    The ID of the department.
     * @param examId    The ID of the exam schedule.
     * @param semesterId The ID of the semester schedule.
     * @return ResponseEntity containing the created CourseDTO and HTTP status.
     */
    public ResponseEntity<CourseDTO> addCourse(Course course, Long userId, Long deptId, Long examId, Long semesterId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id " + userId));
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("Department not found with id " + deptId));
        ExamSchedule examSchedule = examScheduleRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam Schedule not found with id " + examId));
        SemesterSchedule schedule = semesterScheduleRepository.findById(semesterId)
                .orElseThrow(() -> new RuntimeException("Semester Schedule not found with id " + semesterId));

        Course newCourse = Course.builder()
                .name(course.getName())
                .description(course.getDescription())
                .year(course.getYear())
                .department(department)
                .examSchedule(examSchedule)
                .semesterSchedule(schedule)
                .user(user)
                .createdAt(new Date())
                .updatedAt(null)
                .build();

        Course savedCourse = courseRepository.save(newCourse);
        CourseDTO courseDTO = convertToDTO(savedCourse);

        return new ResponseEntity<>(courseDTO, HttpStatus.CREATED);
    }

    /**
     * Updates an existing course and returns the updated CourseDTO.
     *
     * @param courseId      The ID of the course to update.
     * @param courseDetails The new course details.
     * @param userId        The ID of the user.
     * @param deptId        The ID of the new department.
     * @param examId        The ID of the new exam schedule.
     * @param semesterId    The ID of the new semester schedule.
     * @return ResponseEntity containing the updated CourseDTO and HTTP status.
     */
    public ResponseEntity<CourseDTO> updateCourse(Long courseId, Course courseDetails, Long userId, Long deptId, Long examId, Long semesterId) {
        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id " + courseId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id " + userId));
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("Department not found with id " + deptId));
        ExamSchedule examSchedule = examScheduleRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam Schedule not found with id " + examId));
        SemesterSchedule schedule = semesterScheduleRepository.findById(semesterId)
                .orElseThrow(() -> new RuntimeException("Semester Schedule not found with id " + semesterId));

        existingCourse.setName(courseDetails.getName());
        existingCourse.setDescription(courseDetails.getDescription());
        existingCourse.setYear(courseDetails.getYear());
        existingCourse.setUser(user);
        existingCourse.setDepartment(department);
        existingCourse.setExamSchedule(examSchedule);
        existingCourse.setSemesterSchedule(schedule);
        existingCourse.setUpdatedAt(new Date());

        Course updatedCourse = courseRepository.save(existingCourse);
        CourseDTO courseDTO = convertToDTO(updatedCourse);

        return new ResponseEntity<>(courseDTO, HttpStatus.OK);
    }

    /**
     * Retrieves all courses as a list of CourseDTOs.
     *
     * @return ResponseEntity containing the list of CourseDTOs and HTTP status.
     */
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        List<Course> courses = courseRepository.findAll();
        List<CourseDTO> courseDTOs = courses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new ResponseEntity<>(courseDTOs, HttpStatus.OK);
    }

    /**
     * Retrieves a course by its ID as a CourseDTO.
     *
//     * @param courseId The ID of the course to retrieve.
     * @return ResponseEntity containing the CourseDTO and HTTP status.
     */
//    public ResponseEntity<CourseDTO> getCourseById(Long courseId) {
//        Course course = courseRepository.findById(courseId)
//                .orElseThrow(() -> new RuntimeException("Course not found with id " + courseId));
//        CourseDTO courseDTO = convertToDTO(course);
//        return new ResponseEntity<>(courseDTO, HttpStatus.OK);
//    }

    public List<Course> getCoursesByUserId(Long userId) {
        return courseRepository.findByUserId(userId);
    }

    /**
     * Searches for courses by name and returns a list of matching CourseDTOs.
     *
     * @param name The name or partial name to search for.
     * @return ResponseEntity containing the list of matching CourseDTOs and HTTP status.
     */
    public ResponseEntity<List<CourseDTO>> searchCourseByName(String name) {
        List<Course> courses = courseRepository.findByNameContainingIgnoreCase(name);
        List<CourseDTO> courseDTOs = courses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new ResponseEntity<>(courseDTOs, HttpStatus.OK);
    }

    /**
     * Deletes a course by its ID.
     *
     * @param courseId The ID of the course to delete.
     * @return ResponseEntity containing a success or error message and HTTP status.
     */
    public ResponseEntity<String> deleteCourseById(Long courseId) {
        if (courseRepository.existsById(courseId)) {
            courseRepository.deleteById(courseId);
            return new ResponseEntity<>("Course deleted successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Course not found with id " + courseId, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Converts a Course entity to a CourseDTO.
     *
     * @param course The Course entity to convert.
     * @return The corresponding CourseDTO.
     */
    private CourseDTO convertToDTO(Course course) {
        DepartmentDTO departmentDTO = DepartmentDTO.builder()
                .id(course.getDepartment().getId())
                .name(course.getDepartment().getName())
                .build();

        ExamScheduleDTO examScheduleDTO = ExamScheduleDTO.builder()
                .id(course.getExamSchedule().getId())
                .examDate(course.getExamSchedule().getExamDate())
                .build();

        SemesterScheduleDTO semesterScheduleDTO = SemesterScheduleDTO.builder()
                .id(course.getSemesterSchedule().getId())
                .semesterName(course.getSemesterSchedule().getSemesterName())
                .build();

        return CourseDTO.builder()
                .id(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .year(course.getYear())
                .department(departmentDTO)
                .examSchedule(examScheduleDTO)
                .semesterSchedule(semesterScheduleDTO)
                .userId(course.getUser() != null ? course.getUser().getId() : null) // Include userId
                .build();
    }
}
