package com.rookies4.MiniProject3.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies4.MiniProject3.domain.entity.Summary;
import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SummaryService {

    private final SummaryRepository summaryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 전체 요약 저장 (chapter = 0)
     */
    public void saveFullSummary(Content content, String fullSummaryJson) {
        try {
            JsonNode root = objectMapper.readTree(fullSummaryJson);
            String summaryText = root.has("summaryText")
                    ? root.get("summaryText").asText()
                    : fullSummaryJson;

            Summary summary = Summary.builder()
                    .content(content)
                    .chapter(0)
                    .summaryText(summaryText)
                    .keySentences(null)
                    .build();

            summaryRepository.save(summary);
            log.info("✅ 전체 요약 DB 저장 완료 | contentId={}", content.getId());
        } catch (Exception e) {
            log.error("❌ 전체 요약 파싱 실패 | contentId={} | error={}", content.getId(), e.getMessage());
        }
    }

    /**
     * 단원별 요약 저장
     */
    public int saveChapterSummaries(Content content, String chapterSummaryJson) {
        int savedCount = 0;
        try {
            JsonNode root = objectMapper.readTree(chapterSummaryJson);
            if (!root.has("summaries")) {
                log.warn("⚠️ 요약 데이터 없음 | contentId={}", content.getId());
                return 0;
            }

            List<Summary> summaries = new ArrayList<>();
            for (JsonNode item : root.get("summaries")) {
                String chapterStr = item.has("chapter") ? item.get("chapter").asText() : "0";
                String summaryText = item.has("summaryText") ? item.get("summaryText").asText() : "";

                int chapterNum = 0;
                try {
                    chapterNum = Integer.parseInt(chapterStr.replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {}

                summaries.add(Summary.builder()
                        .content(content)
                        .chapter(chapterNum)
                        .summaryText(summaryText)
                        .keySentences(null)
                        .build());
            }

            summaryRepository.saveAll(summaries);
            savedCount = summaries.size();
            log.info("✅ 단원 요약 {}개 저장 완료 | contentId={}", savedCount, content.getId());
        } catch (Exception e) {
            log.error("❌ 단원 요약 저장 실패 | contentId={} | error={}", content.getId(), e.getMessage());
        }
        return savedCount;
    }
}
