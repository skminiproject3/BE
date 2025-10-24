package com.rookies4.MiniProject3.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ContentDto {

    // ============================================
    // ✅ 업로드 응답 DTO
    // ============================================
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadResponse {
        private Long contentId;     // 콘텐츠 ID
        private String title;       // 파일 제목
        private String status;      // 상태 (PROCESSING, COMPLETED, FAILED)
        private Integer totalChapters; // 감지된 총 챕터 수 (nullable 가능)
        private String vectorPath;
    }

    // ============================================
    // ✅ 상태 조회 응답 DTO
    // ============================================
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusResponse {
        private String status;
    }
}
