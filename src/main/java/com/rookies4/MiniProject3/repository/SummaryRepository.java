package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Summary;
import com.rookies4.MiniProject3.domain.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SummaryRepository extends JpaRepository<Summary, Long> {

    List<Summary> findByContentIdOrderByChapterAsc(Long contentId);
}