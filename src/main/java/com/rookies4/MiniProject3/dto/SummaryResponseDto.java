package com.rookies4.MiniProject3.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rookies4.MiniProject3.domain.entity.Summary;
import lombok.Getter;

@Getter
public class SummaryResponseDto {

    private int chapter;

    @JsonProperty("summary_text")
    private String summaryText;

    public SummaryResponseDto(Summary summary) {
        this.chapter = summary.getChapter();
        this.summaryText = summary.getSummaryText();
    }
}
