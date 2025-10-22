package com.rookies4.MiniProject3.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiPdfPathsRequest {
    @JsonProperty("pdf_paths")
    private List<String> pdfPaths;
}
