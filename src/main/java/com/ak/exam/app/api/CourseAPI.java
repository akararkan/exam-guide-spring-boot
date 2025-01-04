package com.ak.exam.app.api;

import com.ak.exam.app.dto.CourseDTO;
import com.ak.exam.app.dto.DepartmentDTO;
import com.ak.exam.app.dto.ExamScheduleDTO;
import com.ak.exam.app.dto.SemesterScheduleDTO;
import com.ak.exam.app.model.Course;
import com.ak.exam.app.model.Department;
import com.ak.exam.app.model.ExamSchedule;
import com.ak.exam.app.model.SemesterSchedule;
import com.ak.exam.app.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/course")
@RequiredArgsConstructor
public class CourseAPI {

    private final CourseService courseService;

    /**
     * Create a new Course.
     *
     * @param courseDTO  The course details sent in the request body.
     * @param userId     The ID of the user.
     * @param deptId     The ID of the department.
     * @param examId     The ID of the exam schedule.
     * @param semesterId The ID of the semester schedule.
     * @return ResponseEntity containing the created CourseDTO and HTTP status.
     */
    @PostMapping("/addCourse/{userId}/{deptId}/{examId}/{semesterId}")
    public ResponseEntity<CourseDTO> addCourse(@RequestBody CourseDTO courseDTO,
                                               @PathVariable Long userId,
                                               @PathVariable Long deptId,
                                               @PathVariable Long examId,
                                               @PathVariable Long semesterId) {
        Course course = new Course();
        course.setName(courseDTO.getName());
        course.setDescription(courseDTO.getDescription());
        course.setYear(courseDTO.getYear());

        return courseService.addCourse(course, userId, deptId, examId, semesterId);
    }

    /**
     * Get Course by ID.
     *
//     * @param courseId The ID of the course to retrieve.
     * @return ResponseEntity containing the CourseDTO and HTTP status.
     */
    @GetMapping("/user/{userId}")
    public List<CourseDTO> getCoursesByUserId(@PathVariable Long userId) {
        List<Course> courses = courseService.getCoursesByUserId(userId);
        // Convert Course entities to CourseDTOs
        List<CourseDTO> courseDTOs = courses.stream()
                .map(course -> CourseDTO.builder()
                        .id(course.getId())
                        .name(course.getName())
                        .description(course.getDescription())
                        .year(course.getYear())
                        .department(mapToDepartmentDTO(course.getDepartment())) // Convert Department to DepartmentDTO
                        .examSchedule(mapToExamScheduleDTO(course.getExamSchedule())) // Convert ExamSchedule to ExamScheduleDTO
                        .semesterSchedule(mapToSemesterScheduleDTO(course.getSemesterSchedule())) // Convert SemesterSchedule to SemesterScheduleDTO
                        .build())
                .collect(Collectors.toList());
        return courseDTOs;
    }

    // Utility method to convert Department to DepartmentDTO
    private DepartmentDTO mapToDepartmentDTO(Department department) {
        if (department == null) {
            return null;
        }
        return DepartmentDTO.builder()
                .id(department.getId())
                .name(department.getName())
                .build();
    }

    // Utility method to convert ExamSchedule to ExamScheduleDTO
    private ExamScheduleDTO mapToExamScheduleDTO(ExamSchedule examSchedule) {
        if (examSchedule == null) {
            return null;
        }
        return ExamScheduleDTO.builder()
                .id(examSchedule.getId())
                .examDate(examSchedule.getExamDate())
                .build();
    }

    // Utility method to convert SemesterSchedule to SemesterScheduleDTO
    private SemesterScheduleDTO mapToSemesterScheduleDTO(SemesterSchedule semesterSchedule) {
        if (semesterSchedule == null) {
            return null;
        }
        return SemesterScheduleDTO.builder()
                .id(semesterSchedule.getId())
                .semesterName(semesterSchedule.getSemesterName())
                .startDate(semesterSchedule.getStartDate())
                .endDate(semesterSchedule.getEndDate())
                .build();
    }




    /**
     * Get all Courses.
     *
     * @return ResponseEntity containing the list of CourseDTOs and HTTP status.
     */
    @GetMapping("/getAllCourses")
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        return courseService.getAllCourses();
    }

    /**
     * Update a Course.
     *
     * @param courseId       The ID of the course to update.
     * @param courseDetails  The updated course details sent in the request body.
     * @param userId         The ID of the user.
     * @param deptId         The ID of the new department.
     * @param examId         The ID of the new exam schedule.
     * @param semesterId     The ID of the new semester schedule.
     * @return ResponseEntity containing the updated CourseDTO and HTTP status.
     */
    @PutMapping("/updateCourse/{courseId}/{userId}/{deptId}/{examId}/{semesterId}")
    public ResponseEntity<CourseDTO> updateCourse(@PathVariable Long courseId,
                                                  @RequestBody CourseDTO courseDetails,
                                                  @PathVariable Long userId,
                                                  @PathVariable Long deptId,
                                                  @PathVariable Long examId,
                                                  @PathVariable Long semesterId) {
        Course course = new Course();
        course.setName(courseDetails.getName());
        course.setDescription(courseDetails.getDescription());
        course.setYear(courseDetails.getYear());

        return courseService.updateCourse(courseId, course, userId, deptId, examId, semesterId);
    }

    /**
     * Delete Course by ID.
     *
     * @param courseId The ID of the course to delete.
     * @return ResponseEntity containing a success or error message and HTTP status.
     */
    @DeleteMapping("/deleteCourseById/{courseId}")
    public ResponseEntity<String> deleteCourseById(@PathVariable Long courseId) {
        return courseService.deleteCourseById(courseId);
    }

    /**
     * Search Courses by Name.
     *
     * @param name The name or partial name to search for.
     * @return ResponseEntity containing the list of matching CourseDTOs and HTTP status.
     */
    @GetMapping("/searchCourseByName/{name}")
    public ResponseEntity<List<CourseDTO>> searchCourseByName(@PathVariable String name) {
        return courseService.searchCourseByName(name);
    }
}
