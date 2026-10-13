package com.examsystem.repository;

import com.examsystem.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByPublishedTrue();
}
