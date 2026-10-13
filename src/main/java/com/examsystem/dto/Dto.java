package com.examsystem.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Small request / response shapes used by the student side and the results view. */
public final class Dto {

    private Dto() {}

    public record LoginRequest(String email, String password) {}

    /** A question as the student sees it: the correct answer is never included. */
    public record PortalQuestion(Long id, String questionText, String optionA, String optionB,
                                 String optionC, String optionD, Integer marks) {}

    public record StartResponse(Long attemptId, String examTitle, long secondsLeft,
                                List<PortalQuestion> questions) {}

    public record SubmitRequest(Map<Long, String> answers) {}

    /** window = UPCOMING / OPEN / CLOSED, attemptStatus = NONE / IN_PROGRESS / SUBMITTED */
    public record ExamCard(Long id, String title, String subject, Integer durationMinutes,
                           LocalDateTime startTime, LocalDateTime endTime,
                           String window, String attemptStatus) {}

    /** score and totalMarks stay null until the admin releases the results. */
    public record ResultView(Long attemptId, String examTitle, LocalDateTime submittedAt,
                             boolean released, Integer score, Integer totalMarks) {}

    public record AttemptRow(Long attemptId, String studentName, String studentEmail,
                             LocalDateTime submittedAt, Integer score, Integer totalMarks) {}
}
