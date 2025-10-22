package com.rookies4.MiniProject3.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiSummaryItem {

    @JsonProperty("chapter")
    private String chapter;

    @JsonProperty("summaryText")
    private String summaryText;
}
