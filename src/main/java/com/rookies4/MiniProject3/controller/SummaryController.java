package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.dto.SummaryChapterRequestDto;
import com.rookies4.MiniProject3.dto.SummaryResponseDto;
import com.rookies4.MiniProject3.service.SummaryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents/{contentId}/summaries")
public class SummaryController {

    private final SummaryService summaryService;

    // 전체 요약 생성
    @PostMapping("/full")
    public ResponseEntity<SummaryResponseDto> generateFullSummary(
            @PathVariable Long contentId
    ) {
        SummaryResponseDto response = summaryService.createFullSummary(contentId);
        return ResponseEntity.ok(response);
    }

    // 단원별 요약 생성
    @PostMapping("/chapter")
    public ResponseEntity<SummaryResponseDto> generateChapterSummary(
            @PathVariable Long contentId,
            @RequestBody SummaryChapterRequestDto requestDto
    ) {
        SummaryResponseDto response = summaryService.createChapterSummary(contentId, requestDto);
        return ResponseEntity.ok(response);
    }

    // 캐시된 요약 목록 조회
    @GetMapping
    public ResponseEntity<List<SummaryResponseDto>> getCachedSummaries(
            @PathVariable Long contentId
    ) {
        List<SummaryResponseDto> responseList = summaryService.getCachedSummaries(contentId);
        return ResponseEntity.ok(responseList);
    }
}
