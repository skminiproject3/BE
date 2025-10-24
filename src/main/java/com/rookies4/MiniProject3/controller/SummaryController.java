package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.dto.SummaryDto;
import com.rookies4.MiniProject3.repository.ContentRepository;
import com.rookies4.MiniProject3.service.PythonServerClient;
import com.rookies4.MiniProject3.service.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class SummaryController {

    private final PythonServerClient pythonClient;
    private final SummaryService summaryService;
    private final ContentRepository contentRepository;

    /**
     * 전체 요약
     * FastAPI → /api/contents/{content_id}/summarize
     */
    @PostMapping("/{contentId}/summarize")
    public ResponseEntity<String> summarizeFull(@PathVariable Long contentId) {
        log.info("📘 전체 요약 요청 수신 | contentId={}", contentId);

        // 1 FastAPI 호출
        String resultJson = pythonClient.summarizeFull(contentId);

        // 2️ DB에서 Content 찾기
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("❌ Content not found: " + contentId));

        // 3️ SummaryService를 통해 저장 (chapter=0)
        summaryService.saveFullSummary(content, resultJson);

        log.info("✅ 전체 요약 저장 완료 | contentId={}", contentId);
        return ResponseEntity.ok(resultJson);
    }

    /**
     * 단원별 요약
     * FastAPI → /api/contents/{content_id}/summaries
     */
    @PostMapping("/{contentId}/summaries")
    public ResponseEntity<String> summarizeByChapter(
            @PathVariable Long contentId,
            @RequestBody SummaryDto.ChapterRequest request
    ) {
        log.info("📘 단원별 요약 요청 수신 | contentId={} | chapter_request={}", contentId, request.getChapter_request());

        // 1️ FastAPI 호출
        String resultJson = pythonClient.summarizeByChapter(contentId, request);

        // 2️ DB에서 Content 조회
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("❌ Content not found: " + contentId));

        // 3 요약 데이터 DB 저장
        int savedCount = summaryService.saveChapterSummaries(content, resultJson);

        log.info("✅ 단원별 요약 {}개 저장 완료 | contentId={}", savedCount, contentId);
        return ResponseEntity.ok(resultJson);
    }
}
