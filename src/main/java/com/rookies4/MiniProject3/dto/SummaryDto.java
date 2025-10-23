package com.rookies4.MiniProject3.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rookies4.MiniProject3.domain.entity.Summary;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class SummaryDto {

    // 특정 챕터의 요약본 요청 시 사용하는 DTO
    @Getter
    @NoArgsConstructor
    public static class ChapterRequest {
        private int chapter;
    }

    // 요약본 정보를 응답할 때 사용하는 DTO
    @Getter
    public static class Response {
        private int chapter;

        @JsonProperty("summary_text")
        private String summaryText;

        public Response(Summary summary) {
            this.chapter = summary.getChapter();
            this.summaryText = summary.getSummaryText();
        }
    }
}