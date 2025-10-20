package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.domain.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    // 특정 사용자가 특정 퀴즈 풀었는지 확인
    List<QuizAttempt> findByUser(User user);

    // 특정 사용자가 특정 퀴즈 문제를 푼 기록 조회
    List<QuizAttempt> findByUserAndQuiz(User user, Quiz quiz);

    // 특정 사용자 + 특정 업로드(문서) 기준 모든 퀴즈 기록 조회
    List<QuizAttempt> findByUserAndQuiz_Upload_Id(User user, Long uploadId);
}