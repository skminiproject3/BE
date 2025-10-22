package com.rookies4.MiniProject3.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiUploadResponse {
    private String message;

    @JsonProperty("saved_files")
    private List<String> pdfPaths;
}
