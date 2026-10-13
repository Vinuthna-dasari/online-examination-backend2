package com.examsystem.repository;

import com.examsystem.model.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    List<Attempt> findByExamId(Long examId);
    List<Attempt> findByStudentId(Long studentId);
    Optional<Attempt> findByExamIdAndStudentId(Long examId, Long studentId);
}
