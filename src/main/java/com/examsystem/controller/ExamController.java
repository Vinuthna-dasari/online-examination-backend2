package com.examsystem.controller;

import com.examsystem.model.Exam;
import com.examsystem.repository.ExamRepository;
import com.examsystem.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Admin side: create, schedule, publish and delete exams. */
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamRepository repository;
    private final ExamService examService;

    public ExamController(ExamRepository repository, ExamService examService) {
        this.repository = repository;
        this.examService = examService;
    }

    private void checkSchedule(Exam body) {
        if (!body.getEndTime().isAfter(body.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after the start time");
        }
    }

    @PostMapping
    public Exam create(@Valid @RequestBody Exam body) {
        checkSchedule(body);
        body.setId(null);
        return repository.save(body);
    }

    @GetMapping
    public List<Exam> readAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Exam readOne(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
    }

    @PutMapping("/{id}")
    public Exam update(@PathVariable Long id, @Valid @RequestBody Exam body) {
        checkSchedule(body);
        Exam existing = readOne(id);
        existing.setTitle(body.getTitle());
        existing.setSubject(body.getSubject());
        existing.setDurationMinutes(body.getDurationMinutes());
        existing.setStartTime(body.getStartTime());
        existing.setEndTime(body.getEndTime());
        existing.setPublished(Boolean.TRUE.equals(body.getPublished()));
        existing.setResultsReleased(Boolean.TRUE.equals(body.getResultsReleased()));
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        readOne(id);
        examService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }
}
