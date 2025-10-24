package com.rookies4.MiniProject3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.dto.SummaryDto;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.repository.ContentRepository;
import com.rookies4.MiniProject3.service.PythonServerClient;
import com.rookies4.MiniProject3.service.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class SummaryController {

    private final PythonServerClient pythonClient;
    private final SummaryService summaryService;
    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==========================================================
    // ✅ [1] 전체 요약 생성 (FastAPI → DB 저장 → JSON 반환)
    // ==========================================================
    @PostMapping("/{contentId}/summarize")
    public ResponseEntity<?> summarizeFull(@PathVariable Long contentId) {
        log.info("📘 전체 요약 요청 수신 | contentId={}", contentId);

        try {
            // 1️⃣ FastAPI 호출
            String resultJson = pythonClient.summarizeFull(contentId);
            Map<String, Object> responseMap = objectMapper.readValue(resultJson, Map.class);

            // 2️⃣ 콘텐츠 확인
            Content content = findContentOrThrow(contentId);

            // 3️⃣ 요약 DB 저장
            summaryService.saveFullSummary(content, resultJson);

            log.info("✅ 전체 요약 저장 및 반환 완료 | contentId={}", contentId);
            return ResponseEntity.ok(responseMap);

        } catch (CustomException e) {
            log.error("❌ 전체 요약 처리 실패 | contentId={} | error={}",
                    contentId, e.getErrorCode().getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(Map.of(
                            "error", e.getErrorCode().getCode(),
                            "message", e.getErrorCode().getMessage()
                    ));

        } catch (Exception e) {
            log.error("❌ 전체 요약 처리 중 예기치 못한 오류 | contentId={} | error={}",
                    contentId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "SUMMARIZE_FULL_FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    // ==========================================================
    // ✅ [2] 단원별 요약 생성 (FastAPI → DB 저장 → JSON 반환)
    // ==========================================================
    @PostMapping("/{contentId}/summaries")
    public ResponseEntity<?> summarizeByChapter(
            @PathVariable Long contentId,
            @RequestBody SummaryDto.ChapterRequest request
    ) {
        log.info("📘 단원별 요약 요청 수신 | contentId={} | chapter={}", contentId, request.getChapter());

        try {
            // 1️⃣ FastAPI 호출
            String resultJson = pythonClient.summarizeByChapter(contentId, request);
            Map<String, Object> responseMap = objectMapper.readValue(resultJson, Map.class);

            // 2️⃣ 콘텐츠 확인
            Content content = findContentOrThrow(contentId);

            // 3️⃣ DB 저장
            int savedCount = summaryService.saveChapterSummaries(content, resultJson);

            log.info("✅ 단원별 요약 {}개 저장 및 반환 완료 | contentId={}", savedCount, contentId);
            return ResponseEntity.ok(responseMap);

        } catch (CustomException e) {
            log.error("❌ 단원별 요약 실패 | contentId={} | error={}",
                    contentId, e.getErrorCode().getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(Map.of(
                            "error", e.getErrorCode().getCode(),
                            "message", e.getErrorCode().getMessage()
                    ));

        } catch (Exception e) {
            log.error("❌ 단원별 요약 처리 중 예기치 못한 오류 | contentId={} | error={}",
                    contentId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "SUMMARIZE_BY_CHAPTER_FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    // ==========================================================
    // ✅ [3] 전체 요약 조회
    // ==========================================================
    @GetMapping("/{contentId}/summarize")
    public ResponseEntity<SummaryDto.Response> getFullSummary(@PathVariable Long contentId) {
        log.info("📗 전체 요약 조회 요청 | contentId={}", contentId);

        SummaryDto.Response summary = summaryService.getFullSummaryByContentId(contentId);
        log.info("✅ 전체 요약 조회 완료 | contentId={}", contentId);

        return ResponseEntity.ok(summary);
    }

    // ==========================================================
    // ✅ [4] 공통 유틸: Content 조회
    // ==========================================================
    private Content findContentOrThrow(Long contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));
    }
}
