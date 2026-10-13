package com.examsystem.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exams")
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    private String subject;

    @NotNull
    @Min(1)
    private Integer durationMinutes;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    private Boolean published = false;

    private Boolean resultsReleased = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    public Boolean getResultsReleased() {
        return resultsReleased;
    }

    public void setResultsReleased(Boolean resultsReleased) {
        this.resultsReleased = resultsReleased;
    }
}
