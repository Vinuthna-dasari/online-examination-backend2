package com.examsystem.controller;

import com.examsystem.model.Student;
import com.examsystem.repository.StudentRepository;
import com.examsystem.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Admin side: manage the students who may sit the exams. */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository repository;
    private final ExamService examService;
    private final PasswordEncoder encoder;

    public StudentController(StudentRepository repository, ExamService examService, PasswordEncoder encoder) {
        this.repository = repository;
        this.examService = examService;
        this.encoder = encoder;
    }

    @PostMapping
    public Student create(@Valid @RequestBody Student body) {
        if (body.getPassword() == null || body.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        body.setId(null);
        body.setEmail(body.getEmail().trim().toLowerCase());
        if (repository.findByEmail(body.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A student with this email already exists");
        }
        body.setPassword(encoder.encode(body.getPassword()));
        return repository.save(body);
    }

    @GetMapping
    public List<Student> readAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Student readOne(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    @PutMapping("/{id}")
    public Student update(@PathVariable Long id, @Valid @RequestBody Student body) {
        Student existing = readOne(id);
        String email = body.getEmail().trim().toLowerCase();
        if (repository.findByEmail(email).filter(s -> !s.getId().equals(id)).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A student with this email already exists");
        }
        existing.setName(body.getName());
        existing.setEmail(email);
        if (body.getPassword() != null && !body.getPassword().isBlank()) {
            existing.setPassword(encoder.encode(body.getPassword()));
        }
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        readOne(id);
        examService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
