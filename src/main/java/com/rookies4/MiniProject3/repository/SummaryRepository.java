package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Summary;
import com.rookies4.MiniProject3.domain.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
    // 특정 업로드 문서의 요약 조회
    List<Summary> findByContent(Content content);

    // 콘텐츠 + 챕터 번호로 조회
    Summary findByContentAndChapter(Content content, Integer chapter);
}