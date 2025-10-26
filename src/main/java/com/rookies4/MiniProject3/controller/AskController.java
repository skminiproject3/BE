package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.service.PythonServerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class AskController {

    private final PythonServerClient pythonServerClient;

    /**
     * ✅ 문서 기반 질문 (RAG + 웹검색)
     * FastAPI `/api/contents/{content_id}/ask` 와 연동
     *
     * Request Example:
     * POST /api/contents/5/ask
     * {
     *   "question": "머신러닝이란 무엇인가요?",
     *   "forceWeb": false
     * }
     */
    @PostMapping("/{contentId}/ask")
    public ResponseEntity<?> askQuestion(
            @PathVariable("contentId") Long contentId,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            String question = (String) requestBody.get("question");
            boolean forceWeb = requestBody.get("forceWeb") != null && (Boolean) requestBody.get("forceWeb");

            if (question == null || question.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "질문 내용이 비어 있습니다."
                ));
            }

            log.info("🧠 [질문 요청 수신] contentId={} | question='{}' | forceWeb={}",
                    contentId, question, forceWeb);

            // ✅ FastAPI로 질문 전달
            String response = pythonServerClient.askQuestion(contentId, question, forceWeb);

            log.info("✅ [질문 결과 수신] contentId={} | 응답길이={}자", contentId, response.length());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [질문 요청 실패] contentId={} | error={}", contentId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "질문 처리 중 오류가 발생했습니다.",
                    "message", e.getMessage()
            ));
        }
    }
}
