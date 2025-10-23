package com.rookies4.MiniProject3.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class SummaryDto {
    @Data
    public static class Response {
        private double  chapter;
        private String summaryText;

        public Response() {}
        public Response(double  chapter, String summaryText) {
            this.chapter = chapter;
            this.summaryText = summaryText;
        }
    }

    @Data
    public static class ChapterSummaryResponse {
        private String message; // FastAPI에서 반환하는 "message"
        private List<Response> summaries; // FastAPI에서 반환하는 "summaries" 리스트
    }
}