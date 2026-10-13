package com.examsystem.repository;

import com.examsystem.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    void deleteByAttemptId(Long attemptId);
    void deleteByQuestionId(Long questionId);
}
