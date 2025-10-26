package com.rookies4.MiniProject3.dto;

import com.rookies4.MiniProject3.domain.entity.Progress;
import lombok.*;

import java.time.LocalDateTime;

public class ProgressDto {

    // =========================================================
    // ✅ 응답 DTO: 항상 recentScore를 0~100으로 표준화해 전달
    // =========================================================
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long contentId;
        private String title;
        /** 0~100 스케일로 표준화된 최신 점수 (소수점 유지) */
        private Float recentScore;
        private String status;
        private LocalDateTime lastAccessedAt;

        public static Response fromEntity(Progress progress) {
            return Response.builder()
                    .contentId(progress.getContent().getId())
                    .title(progress.getContent().getTitle())
                    .recentScore(normalizePercent(progress.getRecentScore()))
                    .status(progress.getStatus().name())
                    .lastAccessedAt(progress.getLastAccessedAt())
                    .build();
        }

        /** 내부 헬퍼: null 허용, 0~1 입력 시 100배 보정, 0~100로 클램핑 */
        private static Float normalizePercent(Float score) {
            if (score == null) return null; // 점수 자체가 아직 없는 경우는 null 유지
            float v = score;
            if (v <= 1.0f) v = v * 100.0f;  // 0~1 스케일 지원
            if (v < 0f) v = 0f;
            if (v > 100f) v = 100f;
            // 필요 시 소수 한 자리 반올림: 아래 주석 해제
            // v = Math.round(v * 10f) / 10f;
            return v;
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

        /** 새 점수 (0~1 또는 0~100를 받아도 서비스 레이어에서 표준화) */
        private Float newScore;
    }
}
