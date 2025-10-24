package com.rookies4.MiniProject3.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

public class SummaryDto {

    // 단원별 요약 요청용
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterRequest {
        private List<String> pdf_paths;     // FastAPI에서 요구하는 pdf_paths
        private String chapter_request;     // 특정 챕터 지정 (예: "2.1")
    }

    // 요약 응답용
    @Getter
    @AllArgsConstructor
    public static class Response {
        private String chapter;
        private String summaryText;
    }
}
