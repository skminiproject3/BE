package com.rookies4.MiniProject3.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiChapterSummaryResponse {

    @JsonProperty("message")
    private String message;

    @JsonProperty("summaries")
    private List<AiSummaryItem> summaries;
}
