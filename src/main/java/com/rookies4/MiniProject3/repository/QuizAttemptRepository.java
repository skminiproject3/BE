package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByProgress(Progress progress);
}
