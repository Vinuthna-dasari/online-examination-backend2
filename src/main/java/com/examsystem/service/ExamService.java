package com.examsystem.service;

import com.examsystem.dto.Dto;
import com.examsystem.model.*;
import com.examsystem.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Cascade deletes, timed attempts, automatic evaluation and result building. */
@Service
public class ExamService {

    // Extra seconds allowed for a submission to travel from the browser to the server.
    private static final long GRACE_SECONDS = 60;

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final StudentRepository studentRepository;

    public ExamService(ExamRepository examRepository, QuestionRepository questionRepository,
                       AttemptRepository attemptRepository, AnswerRepository answerRepository,
                       StudentRepository studentRepository) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.studentRepository = studentRepository;
    }

    // ---------------------------------------------------------------- deletes

    @Transactional
    public void deleteQuestion(Long id) {
        answerRepository.deleteByQuestionId(id);
        questionRepository.deleteById(id);
    }

    @Transactional
    public void deleteAttempt(Long id) {
        answerRepository.deleteByAttemptId(id);
        attemptRepository.deleteById(id);
    }

    @Transactional
    public void deleteExam(Long id) {
        for (Attempt a : attemptRepository.findByExamId(id)) {
            deleteAttempt(a.getId());
        }
        questionRepository.deleteByExamId(id);
        examRepository.deleteById(id);
    }

    @Transactional
    public void deleteStudent(Long id) {
        for (Attempt a : attemptRepository.findByStudentId(id)) {
            deleteAttempt(a.getId());
        }
        studentRepository.deleteById(id);
    }

    // ---------------------------------------------------------------- student side

    @Transactional
    public List<Dto.ExamCard> listExams(Long studentId) {
        requireStudent(studentId);
        LocalDateTime now = LocalDateTime.now();
        List<Dto.ExamCard> cards = new ArrayList<>();
        for (Exam e : examRepository.findByPublishedTrue()) {
            Attempt a = attemptRepository.findByExamIdAndStudentId(e.getId(), studentId).orElse(null);
            if (a != null) {
                closeIfExpired(a, e);
            }
            String window = now.isBefore(e.getStartTime()) ? "UPCOMING"
                    : now.isAfter(e.getEndTime()) ? "CLOSED" : "OPEN";
            String status = a == null ? "NONE" : a.getSubmittedAt() != null ? "SUBMITTED" : "IN_PROGRESS";
            cards.add(new Dto.ExamCard(e.getId(), e.getTitle(), e.getSubject(), e.getDurationMinutes(),
                    e.getStartTime(), e.getEndTime(), window, status));
        }
        cards.sort(Comparator.comparing(Dto.ExamCard::startTime));
        return cards;
    }

    /** Starts the timed attempt, or resumes one that is still running. */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Dto.StartResponse start(Long examId, Long studentId) {
        requireStudent(studentId);
        Exam exam = exam(examId);
        LocalDateTime now = LocalDateTime.now();

        Attempt attempt = attemptRepository.findByExamIdAndStudentId(examId, studentId).orElse(null);
        if (attempt == null) {
            if (!Boolean.TRUE.equals(exam.getPublished())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This exam is not published.");
            }
            if (now.isBefore(exam.getStartTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This exam has not started yet.");
            }
            if (now.isAfter(exam.getEndTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The exam window is closed.");
            }
            if (questionRepository.findByExamId(examId).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This exam has no questions yet.");
            }
            attempt = new Attempt();
            attempt.setExamId(examId);
            attempt.setStudentId(studentId);
            attempt.setStartedAt(now);
            attempt = attemptRepository.save(attempt);
        } else {
            closeIfExpired(attempt, exam);
            if (attempt.getSubmittedAt() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already submitted this exam.");
            }
        }

        long secondsLeft = Duration.between(now, deadline(attempt, exam)).getSeconds();
        if (secondsLeft <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Your time for this exam is over.");
        }

        List<Dto.PortalQuestion> questions = new ArrayList<>();
        for (Question q : questionRepository.findByExamId(examId)) {
            questions.add(new Dto.PortalQuestion(q.getId(), q.getQuestionText(), q.getOptionA(),
                    q.getOptionB(), q.getOptionC(), q.getOptionD(), q.getMarks()));
        }
        return new Dto.StartResponse(attempt.getId(), exam.getTitle(), secondsLeft, questions);
    }

    /** Stores the answers and evaluates the objective questions automatically. */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Dto.ResultView submit(Long attemptId, Long studentId, Map<Long, String> answers) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));
        if (!attempt.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This attempt belongs to another student.");
        }
        if (attempt.getSubmittedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already submitted this exam.");
        }

        Exam exam = exam(attempt.getExamId());
        boolean onTime = !LocalDateTime.now().isAfter(deadline(attempt, exam).plusSeconds(GRACE_SECONDS));
        finalizeAttempt(attempt, exam, onTime ? answers : null);

        if (!onTime) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Time was over, so your answers were not accepted.");
        }
        return resultView(attempt, exam);
    }

    @Transactional
    public List<Dto.ResultView> results(Long studentId) {
        requireStudent(studentId);
        List<Dto.ResultView> out = new ArrayList<>();
        for (Attempt a : attemptRepository.findByStudentId(studentId)) {
            Exam e = examRepository.findById(a.getExamId()).orElse(null);
            if (e == null) {
                continue;
            }
            closeIfExpired(a, e);
            if (a.getSubmittedAt() != null) {
                out.add(resultView(a, e));
            }
        }
        out.sort(Comparator.comparing(Dto.ResultView::submittedAt).reversed());
        return out;
    }

    // ---------------------------------------------------------------- admin side

    @Transactional
    public List<Dto.AttemptRow> resultsForExam(Long examId) {
        Exam exam = exam(examId);
        List<Dto.AttemptRow> rows = new ArrayList<>();
        for (Attempt a : attemptRepository.findByExamId(examId)) {
            closeIfExpired(a, exam);
            Student s = studentRepository.findById(a.getStudentId()).orElse(null);
            rows.add(new Dto.AttemptRow(a.getId(),
                    s == null ? "(deleted student)" : s.getName(),
                    s == null ? "" : s.getEmail(),
                    a.getSubmittedAt(), a.getScore(), a.getTotalMarks()));
        }
        rows.sort(Comparator.comparing(Dto.AttemptRow::studentName, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    // ---------------------------------------------------------------- helpers

    private Exam exam(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
    }

    private void requireStudent(Long studentId) {
        if (studentId == null || !studentRepository.existsById(studentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }
    }

    private LocalDateTime deadline(Attempt attempt, Exam exam) {
        return attempt.getStartedAt().plusMinutes(exam.getDurationMinutes());
    }

    /** An attempt nobody submitted is closed with zero marks once its time (plus grace) has run out. */
    private void closeIfExpired(Attempt attempt, Exam exam) {
        if (attempt.getSubmittedAt() == null
                && LocalDateTime.now().isAfter(deadline(attempt, exam).plusSeconds(GRACE_SECONDS))) {
            finalizeAttempt(attempt, exam, null);
        }
    }

    private void finalizeAttempt(Attempt attempt, Exam exam, Map<Long, String> answers) {
        int total = 0;
        int score = 0;
        for (Question q : questionRepository.findByExamId(exam.getId())) {
            total += q.getMarks();
            String chosen = answers == null ? null : answers.get(q.getId());
            if (chosen != null && !chosen.isBlank()) {
                String option = chosen.trim().toUpperCase();
                Answer answer = new Answer();
                answer.setAttemptId(attempt.getId());
                answer.setQuestionId(q.getId());
                answer.setSelectedOption(option);
                answerRepository.save(answer);
                if (option.equals(q.getCorrectOption())) {
                    score += q.getMarks();
                }
            }
        }
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setScore(score);
        attempt.setTotalMarks(total);
        attemptRepository.save(attempt);
    }

    private Dto.ResultView resultView(Attempt a, Exam e) {
        boolean released = Boolean.TRUE.equals(e.getResultsReleased());
        return new Dto.ResultView(a.getId(), e.getTitle(), a.getSubmittedAt(), released,
                released ? a.getScore() : null, released ? a.getTotalMarks() : null);
    }
}
