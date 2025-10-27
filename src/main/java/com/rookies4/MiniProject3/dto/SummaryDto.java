package com.rookies4.MiniProject3.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class SummaryDto {

    // ==========================================================
    // ✅ [1] 단원별 요약 요청 DTO
    // ==========================================================
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterRequest {
        private Integer chapter;  // 요청할 챕터 번호 (예: 1, 2, 3 등)
    }

    // ==========================================================
    // ✅ [2] FastAPI 요약 응답 DTO (전체 또는 챕터별)
    // ==========================================================
    @Getter
    @AllArgsConstructor
    public static class Response {
        private Integer chapter;      // 챕터 번호 (0 = 전체 요약)
        private String summaryText;   // 요약 텍스트
    }

    // ==========================================================
    // ✅ [3] FastAPI로 보낼 전체 요청 DTO (선택)
    // ==========================================================
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FullSummaryRequest {
        private List<String> pdf_paths;   // FastAPI에서 요구하는 PDF 경로 리스트
    }
}
