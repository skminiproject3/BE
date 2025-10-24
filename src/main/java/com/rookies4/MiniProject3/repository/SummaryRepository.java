package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {

    /**
     * 특정 콘텐츠의 요약 목록 조회
     * chapterNumber 기준으로 정렬 (오름차순)
     */
    List<Summary> findByContent_IdOrderByChapterAsc(Long contentId);

    /**
     * 특정 콘텐츠 + 특정 챕터 번호로 조회
     */
    List<Summary> findByContent_IdAndChapter(Long contentId, Integer chapterNumber);


}
