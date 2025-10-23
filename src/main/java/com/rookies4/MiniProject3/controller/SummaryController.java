package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.dto.SummaryDto;
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
    public ResponseEntity<SummaryDto.Response> generateFullSummary(
            @PathVariable Long contentId
    ) {
        SummaryDto.Response response = summaryService.createFullSummary(contentId);
        return ResponseEntity.ok(response);
    }

    // 단원별 요약 생성
    @PostMapping("/chapter")
    public ResponseEntity<SummaryDto.Response> generateChapterSummary(
            @PathVariable Long contentId,
            @RequestBody SummaryDto.ChapterRequest requestDto
    ) {
        SummaryDto.Response response = summaryService.createChapterSummary(contentId, requestDto);
        return ResponseEntity.ok(response);
    }

    // 캐시된 요약 목록 조회
    @GetMapping
    public ResponseEntity<List<SummaryDto.Response>> getCachedSummaries(
            @PathVariable Long contentId
    ) {
        List<SummaryDto.Response> responseList = summaryService.getCachedSummaries(contentId);
        return ResponseEntity.ok(responseList);
    }
}
