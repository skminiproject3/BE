package com.rookies4.MiniProject3.dto;

import lombok.Getter;

public class SummaryDto {

    @Getter
    public static class Response {
        private int chapter;
        private String summary_text;

        public Response(int chapter, String summaryText) {
            this.chapter = chapter;
            this.summary_text = summaryText;
        }
    }
}