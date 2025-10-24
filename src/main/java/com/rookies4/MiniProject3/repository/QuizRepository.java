package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    // 특정 업로드 문서의 퀴즈 조회
    List<Quiz> findByContent(Content content);

    // ✅ content 내에서 가장 큰 quiz_id 가져오기
    Optional<Quiz> findTopByContentOrderByQuizIdDesc(Content content);
}
