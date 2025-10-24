package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    // ✅ 해당 콘텐츠의 모든 퀴즈 (전체 회차 다 포함)
    List<Quiz> findByContent(Content content);

    // ✅ 해당 콘텐츠의 특정 배치(회차) 퀴즈만
    List<Quiz> findByContentAndQuizBatch(Content content, Integer quizBatch);

    // ✅ quiz_id 증가용: 가장 큰 quiz_id
    Optional<Quiz> findTopByContentOrderByQuizIdDesc(Content content);

    // ✅ batch 증가용: 가장 최신 세트 번호
    Optional<Quiz> findTopByContentOrderByQuizBatchDesc(Content content);
}
