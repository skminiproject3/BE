package com.rookies4.MiniProject3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies4.MiniProject3.domain.entity.*;
import com.rookies4.MiniProject3.dto.*;
import com.rookies4.MiniProject3.service.ContentService;
import com.rookies4.MiniProject3.service.PythonServerClient;
import com.rookies4.MiniProject3.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/contents/{contentId}/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final ContentService contentService;
    private final PythonServerClient pythonClient;
    private final QuizService quizService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==========================================================
    // ✅ 1. FastAPI 연동 퀴즈 생성 → 새 batch 자동 생성
    // ==========================================================
    @PostMapping("/generate")
    public ResponseEntity<?> generateQuiz(
            @PathVariable Long contentId,
            @RequestBody QuizRequest request
    ) {
        try {
            Content content = contentService.findById(contentId);
            if (content == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ 콘텐츠를 찾을 수 없습니다."));

            List<String> pdfPaths = contentService.getPdfPaths(contentId);
            if (pdfPaths == null || pdfPaths.isEmpty())
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "❌ PDF 경로가 존재하지 않습니다."));

            // FastAPI에 퀴즈 생성 요청
            List<QuizResponseDto> generated = pythonClient.generateQuiz(
                    contentId, pdfPaths,
                    request.getNumQuestions(),
                    request.getDifficulty()
            );

            if (generated.isEmpty())
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "❌ 퀴즈 생성 실패 — Python 서버 응답 없음"));

            // DB에 새 batch로 저장
            List<Quiz> saved = quizService.saveGeneratedQuizSet(content, generated);
            int batch = saved.isEmpty() ? -1 : saved.get(0).getQuizBatch();

            // 응답에 문제 목록까지 포함
            List<Map<String, Object>> quizList = new ArrayList<>();
            for (Quiz q : saved) {
                List<String> options;
                try {
                    options = objectMapper.readValue(q.getOptions(), List.class);
                } catch (Exception e) {
                    options = List.of();
                }

                Map<String, Object> map = new LinkedHashMap<>();
                map.put("quiz_id", q.getQuizId());
                map.put("quiz_batch", q.getQuizBatch());
                map.put("question", q.getQuestion());
                map.put("options", options);
                map.put("correct_answer", q.getCorrectAnswer());
                map.put("explanation", q.getExplanation());
                quizList.add(map);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "✅ 퀴즈 생성 & 저장 완료",
                    "batch", batch,
                    "generatedCount", quizList.size(),
                    "quizzes", quizList
            ));

        } catch (Exception e) {
            log.error("🚨 퀴즈 생성 오류", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "서버 내부 오류"));
        }
    }

    // ==========================================================
    // ✅ 2. 퀴즈 조회 (?batch=1 → 특정 회차 / 없으면 최신 회차)
    // ==========================================================
    @GetMapping("")
    public ResponseEntity<?> getQuizzes(
            @PathVariable Long contentId,
            @RequestParam(value = "batch", required = false) Integer batchParam
    ) {
        try {
            Content content = contentService.findById(contentId);
            if (content == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ 콘텐츠를 찾을 수 없습니다."));

            List<Quiz> quizzes;
            int batchUsed;

            if (batchParam != null) {
                quizzes = quizService.getQuizzesByContentAndBatch(content, batchParam);
                batchUsed = batchParam;
            } else {
                quizzes = quizService.getLatestBatchQuizzes(content);
                batchUsed = quizService.getLatestBatchForContent(content);
            }

            if (quizzes.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ 해당 회차의 퀴즈가 없습니다.", "batch", batchUsed));

            List<Map<String, Object>> resultList = new ArrayList<>();
            for (Quiz q : quizzes) {
                List<String> options;
                try {
                    options = objectMapper.readValue(q.getOptions(), List.class);
                } catch (Exception e) {
                    options = List.of();
                }

                Map<String, Object> map = new LinkedHashMap<>();
                map.put("quiz_id", q.getQuizId());
                map.put("quiz_batch", q.getQuizBatch());
                map.put("question", q.getQuestion());
                map.put("options", options);
                map.put("correct_answer", q.getCorrectAnswer());
                map.put("explanation", q.getExplanation());
                resultList.add(map);
            }

            return ResponseEntity.ok(Map.of(
                    "content_id", contentId,
                    "quiz_batch", batchUsed,
                    "quiz_count", resultList.size(),
                    "quizzes", resultList
            ));
        } catch (Exception e) {
            log.error("🚨 퀴즈 조회 오류", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "퀴즈 조회 중 오류 발생"));
        }
    }

    // ==========================================================
    // ✅ 3. 회차별 채점 + quiz_attempts 저장
    // ==========================================================
    @PostMapping("/grade")
    public ResponseEntity<?> gradeQuiz(
            @PathVariable Long contentId,
            @RequestParam(value = "batch", required = false) Integer batchParam,
            @RequestBody QuizGradeRequest request
    ) {
        try {
            Content content = contentService.findById(contentId);
            if (content == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ 콘텐츠 없음"));

            int batchToUse = (batchParam != null)
                    ? batchParam
                    : quizService.getLatestBatchForContent(content);

            List<Quiz> quizzes = quizService.getQuizzesByContentAndBatch(content, batchToUse);
            if (quizzes.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ 해당 회차의 퀴즈가 없습니다.", "batch", batchToUse));

            Map<String, Object> result = quizService.gradeQuizLocally(quizzes, request.getAnswers());
            Progress progress = contentService.findProgressByContentId(contentId);

            if (progress != null) {
                quizService.saveQuizAttempt(progress, result, batchToUse);
                log.info("✅ quiz_attempts 저장 완료 (batch={})", batchToUse);
            }

            result.put("batch", batchToUse);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("🚨 채점 오류", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "채점 중 오류 발생"));
        }
    }

    // ==========================================================
    // ✅ 4. 회차별 채점 결과 조회 (?batch=)
    // ==========================================================
    @GetMapping("/attempts")
    public ResponseEntity<?> getQuizAttempts(
            @PathVariable Long contentId,
            @RequestParam(value = "batch", required = false) Integer batchParam
    ) {
        try {
            Content content = contentService.findById(contentId);
            if (content == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ 콘텐츠 없음"));

            Progress progress = contentService.findProgressByContentId(contentId);
            if (progress == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ Progress 없음"));

            List<QuizAttempt> attempts = quizService.getAttemptsByProgress(progress);
            if (batchParam != null) {
                attempts = attempts.stream()
                        .filter(a -> a.getQuizBatch().equals(batchParam))
                        .toList();
            }

            if (attempts.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ 해당 회차의 채점 결과가 없습니다."));

            List<Map<String, Object>> resultList = new ArrayList<>();
            for (QuizAttempt a : attempts) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("attempt_id", a.getId());
                r.put("batch", a.getQuizBatch());
                r.put("score", a.getScore());
                r.put("correct_answers", a.getCorrectAnswers());
                r.put("total_questions", a.getTotalQuestions());
                r.put("created_at", a.getCreatedAt());
                resultList.add(r);
            }

            return ResponseEntity.ok(Map.of(
                    "content_id", contentId,
                    "attempt_count", resultList.size(),
                    "attempts", resultList
            ));
        } catch (Exception e) {
            log.error("🚨 채점 결과 조회 오류", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "채점 결과 조회 오류"));
        }
    }
}

