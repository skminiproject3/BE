package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Summary;
import com.rookies4.MiniProject3.dto.SummaryDto;
import com.rookies4.MiniProject3.dto.ai.AiChapterSummaryResponse;
import com.rookies4.MiniProject3.dto.ai.AiFullSummaryResponse;
import com.rookies4.MiniProject3.dto.ai.AiSummaryItem;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.repository.ContentRepository;
import com.rookies4.MiniProject3.repository.SummaryRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final SummaryRepository summaryRepository;
    private final ContentRepository contentRepository;
    private final AiService aiService;

    // 전체 요약 생성
    @Transactional
    public SummaryDto.Response createFullSummary(Long contentId) {
        // 콘텐츠 조회 및 AI 처리 상태 확인
        Content content = findContentOrThrow(contentId);
        validateAiProcessingCompleted(content);

        // AI 서버에 요약 요청
        List<String> pdfPaths = List.of(content.getAiServerPath());
        AiFullSummaryResponse aiResponse = aiService.generateFullSummary(pdfPaths);

        if (aiResponse == null || !StringUtils.hasText(aiResponse.getSummaryText())) {
            throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
        }

        // DB 에 요약 캐시
        Summary summary = Summary.builder()
                .content(content)
                .chapter(0) // 0 = 전체 요약
                .summaryText(aiResponse.getSummaryText())
                .build();

        summaryRepository.save(summary);

        return new SummaryDto.Response(summary);
    }

    // 단원별 요약 생성
    @Transactional
    public SummaryDto.Response createChapterSummary(Long contentId, SummaryDto.ChapterRequest requestDto) {
        // 1. 콘텐츠 조회 및 AI 처리 상태 확인
        Content content = findContentOrThrow(contentId);
        validateAiProcessingCompleted(content); // 409 Conflict 처리

        // 2. AI 서버에 요약 요청
        List<String> pdfPaths = List.of(content.getAiServerPath());
        int chapter = requestDto.getChapter();
        AiChapterSummaryResponse aiResponse = aiService.generateChapterSummary(pdfPaths, chapter);

        // AI가 단원 요약 시 리스트(summaries)로 반환하므로 첫 번째 항목을 사용
        AiSummaryItem summaryItem = aiResponse.getSummaries().stream()
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.AI_PROCESSING_FAILED));

        // 3. DB에 요약 "캐시" (저장)
        Summary summary = Summary.builder()
                .content(content)
                .chapter(chapter) // 요청받은 단원 번호
                .summaryText(summaryItem.getSummaryText())
                .build();

        summaryRepository.save(summary);

        return new SummaryDto.Response(summary);
    }

    // 캐시된 요약 목록 조회
    @Transactional(readOnly = true)
    public List<SummaryDto.Response> getCachedSummaries(Long contentId) {
        // 1. contentId 존재 여부 확인 (optional)
        contentRepository.findById(contentId)
                 .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        // 2. DB 에서 캐시된 요약본 조회
        List<Summary> summaries = summaryRepository.findByContentId(contentId);

        return summaries.stream()
                .map(SummaryDto.Response::new)
                .collect(Collectors.toList());
    }

    // --- 공통 메서드 ---
    private Content findContentOrThrow(Long contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND)); // 404
    }

    // API 명세의 409 Conflict 처리
    private void validateAiProcessingCompleted(Content content) {
        if (!StringUtils.hasText(content.getAiServerPath())) {
            throw new CustomException(ErrorCode.PROCESSING_NOT_COMPLETED); // 409
        }
    }
}
