package com.examsystem.controller;

import com.examsystem.dto.Dto;
import com.examsystem.model.Student;
import com.examsystem.repository.StudentRepository;
import com.examsystem.service.ExamService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** Student side: log in, see exams, take a timed exam, submit, view released results. */
@RestController
@RequestMapping("/api/portal")
public class PortalController {

    private final StudentRepository studentRepository;
    private final ExamService examService;
    private final PasswordEncoder encoder;

    public PortalController(StudentRepository studentRepository, ExamService examService, PasswordEncoder encoder) {
        this.studentRepository = studentRepository;
        this.examService = examService;
        this.encoder = encoder;
    }

    @PostMapping("/login")
    public Student login(@RequestBody Dto.LoginRequest request) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        String password = request.password() == null ? "" : request.password();
        Student student = studentRepository.findByEmail(email).orElse(null);
        if (student == null || !encoder.matches(password, student.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        }
        return student;
    }

    @GetMapping("/exams")
    public List<Dto.ExamCard> exams(@RequestParam Long studentId) {
        return examService.listExams(studentId);
    }

    @PostMapping("/exams/{examId}/start")
    public Dto.StartResponse start(@PathVariable Long examId, @RequestParam Long studentId) {
        return examService.start(examId, studentId);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public Dto.ResultView submit(@PathVariable Long attemptId, @RequestParam Long studentId,
                                 @RequestBody Dto.SubmitRequest body) {
        Map<Long, String> answers = body.answers();
        return examService.submit(attemptId, studentId, answers);
    }

    @GetMapping("/results")
    public List<Dto.ResultView> results(@RequestParam Long studentId) {
        return examService.results(studentId);
    }
}
