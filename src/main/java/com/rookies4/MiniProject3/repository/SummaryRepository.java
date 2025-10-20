package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Summary;
import com.rookies4.MiniProject3.domain.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
    // 특정 업로드 문서의 요약 조회
    List<Summary> findByUpload(Content content);
}