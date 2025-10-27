package com.rookies4.MiniProject3.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * 프론트에서 다양한 형태로 오는 채점 요청을 모두 흡수하기 위한 DTO.
 * - 배열 스키마: answers / responses / submissions
 * - 맵 스키마: answers_map_letter / answers_map_text / answers_map_raw
 * - 개별 항목: 문자(A/B), 인덱스(0/1 base), 원문("A. 56비트"), 텍스트("56비트"), 레거시 키들까지 수용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizGradeRequest {

    // (선택) 콘텐츠/배치 정보 – 프론트가 함께 보냄
    @JsonAlias({"contentId", "contentID"})
    private Long content_id;

    private Integer batch;

    // 표준 배열 스키마
    private List<Answer> answers;

    // 프론트 호환(이름만 다른 경우)
    private List<Answer> responses;
    private List<Answer> submissions;

    // 맵 스키마(서버가 맵 기반으로만 읽는 구현을 위해 함께 수용)
    private Map<Long, String> answers_map_letter;   // { quiz_id: "A" }
    private Map<Long, String> answers_map_text;     // { quiz_id: "56비트" }
    private Map<Long, String> answers_map_raw;      // { quiz_id: "A. 56비트" }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Answer {
        // 기본 식별자(여러 이름을 수용)
        @JsonAlias({"id", "question_id"})
        private Long quiz_id;

        // 과거 호환용(문항 본문)
        private String question;

        // 원문 사용자 답: "B. 56비트"
        // (프론트에서 user_answer/raw를 여기에 넣어 보냄)
        @JsonAlias({"option", "selected_option"})
        private String user_answer;

        // 표시용 텍스트: "56비트" (접두 제거본)
        @JsonAlias({"user_answer_text", "selected_option_text"})
        private String answer_text;

        // 문자형 선택: "A"/"B"/...
        @JsonAlias({"selected", "choice", "selected_option_letter", "user_answer_letter"})
        private String answer;

        // 인덱스(0-base/1-base 모두 수용)
        @JsonAlias({"user_answer_index"})
        private Integer answer_index;          // 0-base 권장

        @JsonAlias({"selected_index", "option_index"})
        private Integer answer_index_1based;   // 1-base 호환

        // 필요 시 프론트가 보내는 기타 필드(무시 가능)
        private String raw;    // 클라이언트가 따로 보낼 수도 있는 원본
        private String text;   // 클라이언트가 따로 보낼 수도 있는 표시용
    }
}
