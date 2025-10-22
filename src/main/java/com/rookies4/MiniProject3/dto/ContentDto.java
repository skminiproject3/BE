package com.rookies4.MiniProject3.dto;

import lombok.Builder;
import lombok.Getter;

public class ContentDto {

    @Getter
    public static class UploadResponse {
        private Long contentId;
        private String title;
        private String status; // Enum 타입을 String으로 변환하여 전달

        public UploadResponse(Long contentId, String title, String status) {
            this.contentId = contentId;
            this.title = title;
            this.status = status;
        }
    }

    @Getter
    public static class StatusResponse {
        private String status;

        public StatusResponse(String status) {
            this.status = status;
        }
    }

    // 파일 목록 조회를 위한 DTO
    @Getter
    public static class ContentInfo {
        private Long contentId;
        private String title;
        private String fileName;
        private String status;

        @Builder
        public ContentInfo(Long contentId, String title, String fileName, String status) {
            this.contentId = contentId;
            this.title = title;
            this.fileName = fileName;
            this.status = status;
        }
    }
}