package com.ak.exam.app.service;
import com.ak.exam.app.model.SemesterSchedule;
import com.ak.exam.app.repo.SemesterScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;
import java.util.Optional;
@Service
@Transactional
@RequiredArgsConstructor
public class SemesterScheduleService {
    private final SemesterScheduleRepository semesterScheduleRepository;
    // 1. Create a new SemesterSchedule
    public ResponseEntity<SemesterSchedule> addSemesterSchedule(SemesterSchedule semesterSchedule) {
        SemesterSchedule newSchedule = SemesterSchedule.builder()
                .semesterName(semesterSchedule.getSemesterName())
                .startDate(semesterSchedule.getStartDate())
                .endDate(semesterSchedule.getEndDate())
                .createdAt(new Date())
                .updatedAt(null)
                .build();

        semesterScheduleRepository.save(newSchedule);
        return new ResponseEntity<>(newSchedule, HttpStatus.CREATED);
    }

    // 2. Get all SemesterSchedules
    public ResponseEntity<List<SemesterSchedule>> getAllSemesterSchedules() {
        List<SemesterSchedule> semesterSchedules = semesterScheduleRepository.findAll();
        if (semesterSchedules.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(semesterSchedules, HttpStatus.OK);
    }

    // 3. Get SemesterSchedule by ID
    public ResponseEntity<SemesterSchedule> getSemesterScheduleById(Long id) {
        Optional<SemesterSchedule> semesterSchedule = semesterScheduleRepository.findById(id);
        return semesterSchedule.map(schedule -> new ResponseEntity<>(schedule, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // 4. Update a SemesterSchedule by ID
    public ResponseEntity<SemesterSchedule> updateSemesterScheduleById(Long id, SemesterSchedule semesterScheduleDetails) {
        Optional<SemesterSchedule> existingSchedule = semesterScheduleRepository.findById(id);

        if (existingSchedule.isPresent()) {
            SemesterSchedule updatedSchedule = existingSchedule.get();
            updatedSchedule.setSemesterName(semesterScheduleDetails.getSemesterName());
            updatedSchedule.setStartDate(semesterScheduleDetails.getStartDate());
            updatedSchedule.setEndDate(semesterScheduleDetails.getEndDate());
            updatedSchedule.setUpdatedAt(new Date());

            semesterScheduleRepository.save(updatedSchedule);
            return new ResponseEntity<>(updatedSchedule, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    // 5. Delete a SemesterSchedule by ID
    public ResponseEntity<HttpStatus> deleteSemesterScheduleById(Long id) {
        try {
            semesterScheduleRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
