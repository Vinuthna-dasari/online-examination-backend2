package com.examsystem.controller;

import com.examsystem.model.Question;
import com.examsystem.repository.ExamRepository;
import com.examsystem.repository.QuestionRepository;
import com.examsystem.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Admin side: multiple-choice questions that belong to an exam. */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionRepository repository;
    private final ExamRepository examRepository;
    private final ExamService examService;

    public QuestionController(QuestionRepository repository, ExamRepository examRepository, ExamService examService) {
        this.repository = repository;
        this.examRepository = examRepository;
        this.examService = examService;
    }

    @PostMapping
    public Question create(@Valid @RequestBody Question body) {
        if (!examRepository.existsById(body.getExamId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found");
        }
        body.setId(null);
        return repository.save(body);
    }

    @GetMapping
    public List<Question> read(@RequestParam(required = false) Long examId) {
        return examId == null ? repository.findAll() : repository.findByExamId(examId);
    }

    @GetMapping("/{id}")
    public Question readOne(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
    }

    @PutMapping("/{id}")
    public Question update(@PathVariable Long id, @Valid @RequestBody Question body) {
        Question existing = readOne(id);
        existing.setQuestionText(body.getQuestionText());
        existing.setOptionA(body.getOptionA());
        existing.setOptionB(body.getOptionB());
        existing.setOptionC(body.getOptionC());
        existing.setOptionD(body.getOptionD());
        existing.setCorrectOption(body.getCorrectOption());
        existing.setMarks(body.getMarks());
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        readOne(id);
        examService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}
