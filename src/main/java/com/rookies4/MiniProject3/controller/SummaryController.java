package com.rookies4.MiniProject3.controller;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class SummaryController {

    private final PythonServerClient pythonClient;
    private final SummaryService summaryService;
    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper; // ✅ 주입받기 (전역 설정 사용)

    // ==========================================================
    // ✅ [1] 전체 요약 생성
    // ==========================================================
    @PostMapping("/{contentId}/summarize")
    public ResponseEntity<?> summarizeFull(@PathVariable Long contentId) {
        log.info("📘 전체 요약 요청 수신 | contentId={}", contentId);

        try {
            // 0) Content 선검증 (불필요한 외부호출 방지)
            Content content = contentRepository.findById(contentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

            // 1) FastAPI 호출 → 문자열(원문) 수신
            String body;
            try {
                body = pythonClient.summarizeFull(contentId);
            } catch (HttpStatusCodeException ex) {
                // FastAPI가 4xx/5xx면 여기로 옴 — 바디 꺼내서 그대로 전달
                String errBody = ex.getResponseBodyAsString();
                log.error("❌ Summarizer HTTP {} | body={}", ex.getStatusCode(), errBody);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                        "error", "SUMMARIZER_HTTP_ERROR",
                        "message", errBody
                ));
            }

            if (body == null || body.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                        "error", "SUMMARIZER_EMPTY",
                        "message", "Empty response from summarizer"
                ));
            }

            // 2) JSON 파싱 방어
            JsonNode node;
            try {
                node = objectMapper.readTree(body);
            } catch (Exception parseEx) {
                log.error("❌ NON-JSON from summarizer: {}", body);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                        "error", "SUMMARIZER_NON_JSON",
                        "message", body
                ));
            }

            // 3) FastAPI 에러 패턴 처리: {"detail": "..."}
            if (node.hasNonNull("detail")) {
                String detail = node.get("detail").asText();
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                        "error", "SUMMARIZER_FAILED",
                        "message", detail
                ));
            }

            // 4) 정상 필드 확인
            String summaryText = node.path("summaryText").asText(null);
            if (summaryText == null || summaryText.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                        "error", "MISSING_SUMMARY_TEXT",
                        "message", "summaryText is missing in summarizer response"
                ));
            }

            // 5) 저장 (원문 JSON을 raw로 저장하거나 필요한 부분만 저장)
            summaryService.saveFullSummary(content, body);

            log.info("✅ 전체 요약 저장 및 반환 완료 | contentId={}", contentId);
            return ResponseEntity.ok(objectMapper.convertValue(node, Map.class));

        } catch (CustomException e) {
            log.error("❌ 전체 요약 처리 실패 | contentId={} | {}", contentId, e.getErrorCode().getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus()).body(Map.of(
                    "error", e.getErrorCode().getCode(),
                    "message", e.getErrorCode().getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ summarizeFull 예외 | contentId={} | {}", contentId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "SUMMARIZE_FULL_FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    // ==========================================================
    // ✅ [2] 단원별 요약 생성
    // ==========================================================
    @PostMapping("/{contentId}/summaries")
    public ResponseEntity<?> summarizeByChapter(
            @PathVariable Long contentId,
            @RequestBody SummaryDto.ChapterRequest request
    ) {
        log.info("📘 단원별 요약 요청 수신 | contentId={} | chapter={}", contentId, request.getChapter());

        try {
            Content content = contentRepository.findById(contentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

            String body;
            try {
                body = pythonClient.summarizeByChapter(contentId, request);
            } catch (HttpStatusCodeException ex) {
                String errBody = ex.getResponseBodyAsString();
                log.error("❌ Summarizer HTTP {} | body={}", ex.getStatusCode(), errBody);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                        "error", "SUMMARIZER_HTTP_ERROR",
                        "message", errBody
                ));
            }

            if (body == null || body.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                        "error", "SUMMARIZER_EMPTY",
                        "message", "Empty response from summarizer"
                ));
            }

            JsonNode node;
            try {
                node = objectMapper.readTree(body);
            } catch (Exception parseEx) {
                log.error("❌ NON-JSON from summarizer: {}", body);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                        "error", "SUMMARIZER_NON_JSON",
                        "message", body
                ));
            }

            if (node.hasNonNull("detail")) {
                String detail = node.get("detail").asText();
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                        "error", "SUMMARIZER_FAILED",
                        "message", detail
                ));
            }

            int saved = summaryService.saveChapterSummaries(content, body);
            log.info("✅ 단원별 요약 {}개 저장 | contentId={}", saved, contentId);

            return ResponseEntity.ok(objectMapper.convertValue(node, Map.class));

        } catch (CustomException e) {
            return ResponseEntity.status(e.getErrorCode().getStatus()).body(Map.of(
                    "error", e.getErrorCode().getCode(),
                    "message", e.getErrorCode().getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ summarizeByChapter 예외 | contentId={} | {}", contentId, e.getMessage(), e);
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
}
