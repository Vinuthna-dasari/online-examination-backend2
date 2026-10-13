package com.examsystem.repository;

import com.examsystem.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByExamId(Long examId);
    void deleteByExamId(Long examId);
}
