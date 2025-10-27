package com.rookies4.MiniProject3.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Summary;
import com.rookies4.MiniProject3.dto.SummaryDto;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SummaryService {

    private final SummaryRepository summaryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==========================================================
    // ✅ [1] 전체 요약 저장 (FastAPI: {"content_id": 1, "summaryText": "..."})
    // ==========================================================
    public void saveFullSummary(Content content, String resultJson) {
        try {
            JsonNode root = objectMapper.readTree(resultJson);

            // ✅ FastAPI는 "summaryText" 필드를 반환
            String summaryText = null;
            if (root.has("summaryText")) {
                summaryText = root.path("summaryText").asText(null);
            } else if (root.has("summary")) { // 호환용
                summaryText = root.path("summary").asText(null);
            }

            if (summaryText == null || summaryText.isBlank()) {
                log.error("⚠️ FastAPI 응답에 summaryText 필드가 없습니다: {}", resultJson);
                throw new CustomException(ErrorCode.FAILED_TO_PROCESS_CONTENT);
            }

            // ✅ key_sentences는 FastAPI에서 미전달 → 빈 리스트로 처리
            String keySentences = root.has("key_sentences")
                    ? root.path("key_sentences").toString()
                    : "[]";

            Summary summary = Summary.builder()
                    .content(content)
                    .chapter(0) // 전체 요약은 chapter=0
                    .summaryText(summaryText)
                    .keySentences(keySentences)
                    .build();

            summaryRepository.save(summary);
            log.info("✅ 전체 요약 저장 완료 | contentId={}", content.getId());

        } catch (DataAccessException e) {
            log.error("❌ DB 오류: 전체 요약 저장 실패 - {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.DATABASE_ERROR);

        } catch (CustomException e) {
            throw e;

        } catch (Exception e) {
            log.error("❌ 전체 요약 파싱 실패 | resultJson={}", resultJson, e);
            throw new CustomException(ErrorCode.FAILED_TO_PROCESS_CONTENT);
        }
    }

    // ==========================================================
    // ✅ [2] 단원별 요약 저장 (FastAPI: {"summaries": [ { "chapter": "1.1", "summaryText": "..." } ]})
    // ==========================================================
    public int saveChapterSummaries(Content content, String resultJson) {
        try {
            JsonNode root = objectMapper.readTree(resultJson);
            JsonNode summariesNode = root.path("summaries");

            if (summariesNode.isMissingNode() || !summariesNode.isArray()) {
                log.error("⚠️ FastAPI 응답 구조 오류: summaries 배열 없음 | JSON={}", resultJson);
                throw new CustomException(ErrorCode.FAILED_TO_PROCESS_CONTENT);
            }

            List<Summary> summaries = new ArrayList<>();

            for (JsonNode node : summariesNode) {
                String chapterValue = node.path("chapter").asText("0");
                String summaryText = node.path("summaryText").asText("");
                String keySentences = node.has("key_sentences")
                        ? node.path("key_sentences").toString()
                        : "[]";

                Summary summary = Summary.builder()
                        .content(content)
                        .chapter(parseChapterNumber(chapterValue))
                        .summaryText(summaryText)
                        .keySentences(keySentences)
                        .build();

                summaries.add(summary);
            }

            summaryRepository.saveAll(summaries);
            log.info("✅ {}개 단원 요약 저장 완료 | contentId={}", summaries.size(), content.getId());
            return summaries.size();

        } catch (DataAccessException e) {
            log.error("❌ DB 오류: 단원별 요약 저장 실패 - {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.DATABASE_ERROR);

        } catch (CustomException e) {
            throw e;

        } catch (Exception e) {
            log.error("❌ 단원별 요약 처리 실패 | JSON={}", resultJson, e);
            throw new CustomException(ErrorCode.FAILED_TO_PROCESS_CONTENT);
        }
    }

    // ✅ 챕터 문자열("1.1") → 숫자 변환
    private int parseChapterNumber(String chapterValue) {
        try {
            if (chapterValue.contains(".")) {
                return Integer.parseInt(chapterValue.split("\\.")[0]);
            }
            return Integer.parseInt(chapterValue);
        } catch (Exception e) {
            return 0;
        }
    }

    // ==========================================================
    // ✅ [3] 전체 요약 조회
    // ==========================================================
    @Transactional(readOnly = true)
    public SummaryDto.Response getFullSummaryByContentId(Long contentId) {
        try {
            Summary summary = summaryRepository.findByContentIdAndChapter(contentId, 0)
                    .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

            return new SummaryDto.Response(
                    summary.getChapter(),
                    summary.getSummaryText()
            );

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ 전체 요약 조회 실패 | contentId={} | error={}", contentId, e.getMessage(), e);
            throw new CustomException(ErrorCode.DATABASE_ERROR);
        }
    }
}
