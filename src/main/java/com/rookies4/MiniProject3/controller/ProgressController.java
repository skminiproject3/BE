package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.dto.ProgressDto;
import com.rookies4.MiniProject3.service.ProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    // =====================================================
    // 사용자별 학습 현황 조회
    // =====================================================
    @GetMapping("/{userId}")
    public ResponseEntity<List<ProgressDto.Response>> getUserProgress(@PathVariable Long userId) {
        log.info("📘 사용자 학습 현황 조회 요청 | userId={}", userId);
        List<ProgressDto.Response> response = progressService.getUserProgress(userId);
        return ResponseEntity.ok(response);
    }

    // =====================================================
    // 콘텐츠별 학습자 현황 조회 (관리자/통계용)
    // =====================================================
    @GetMapping("/content/{contentId}")
    public ResponseEntity<List<ProgressDto.Response>> getProgressByContent(@PathVariable Long contentId) {
        log.info("📗 콘텐츠 학습 현황 조회 요청 | contentId={}", contentId);
        List<ProgressDto.Response> response = progressService.getProgressByContent(contentId);
        return ResponseEntity.ok(response);
    }


}
