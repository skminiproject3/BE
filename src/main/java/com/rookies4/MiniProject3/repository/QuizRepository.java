package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.domain.entity.Upload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    // 특정 업로드 문서의 퀴즈 조회
    List<Quiz> findByUpload(Upload upload);
}