package com.examsystem.controller;

import com.examsystem.dto.Dto;
import com.examsystem.service.ExamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin side: see every student's marks for an exam, and reset an attempt so it can be retaken. */
@RestController
@RequestMapping("/api/attempts")
public class AttemptController {

    private final ExamService examService;

    public AttemptController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public List<Dto.AttemptRow> read(@RequestParam Long examId) {
        return examService.resultsForExam(examId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        examService.deleteAttempt(id);
        return ResponseEntity.noContent().build();
    }
}
