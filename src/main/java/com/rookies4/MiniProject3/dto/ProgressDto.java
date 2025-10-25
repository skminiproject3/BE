package com.rookies4.MiniProject3.dto;

import com.rookies4.MiniProject3.domain.entity.Progress;
import lombok.*;

import java.time.LocalDateTime;

public class ProgressDto {

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long contentId;
        private String title;
        private Float recentScore;
        private String status;
        private LocalDateTime lastAccessedAt;

        public static Response fromEntity(Progress progress) {
            return Response.builder()
                    .contentId(progress.getContent().getId())
                    .title(progress.getContent().getTitle())
                    .recentScore(progress.getRecentScore())
                    .status(progress.getStatus().name())
                    .lastAccessedAt(progress.getLastAccessedAt())
                    .build();
        }
    }

    // =========================================================
    // ✅ Progress 업데이트 요청용 DTO
    // =========================================================
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        /** 학습 진행을 업데이트할 사용자 ID */
        private Long userId;

        /** 학습 콘텐츠 ID */
        private Long contentId;

        /** 새 점수 (최근 퀴즈 점수) */
        private Float newScore;
    }
}

