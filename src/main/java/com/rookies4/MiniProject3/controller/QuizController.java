package com.rookies4.MiniProject3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.domain.entity.QuizAttempt;
import com.rookies4.MiniProject3.dto.QuizGradeRequest;
import com.rookies4.MiniProject3.dto.QuizRequest;
import com.rookies4.MiniProject3.dto.QuizResponseDto;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.service.ContentService;
import com.rookies4.MiniProject3.service.ProgressService;
import com.rookies4.MiniProject3.service.PythonServerClient;
import com.rookies4.MiniProject3.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/contents/{contentId}/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final ContentService contentService;
    private final PythonServerClient pythonClient;
    private final ProgressService progressService;
    private final QuizService quizService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ (1) 퀴즈 생성
    @PostMapping("/generate")
    public ResponseEntity<?> generateQuiz(
            @PathVariable Long contentId,
            @RequestBody QuizRequest request
    ) {
        try {
            Content content = contentService.findById(contentId);
            if (content == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "❌ 콘텐츠를 찾을 수 없습니다."));
            }

            List<String> pdfPaths = contentService.getPdfPaths(contentId);
            if (pdfPaths == null || pdfPaths.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "error", "message", "❌ PDF 경로가 존재하지 않습니다."));
            }

            // FastAPI 호출
            List<QuizResponseDto> generated = pythonClient.generateQuiz(
                    contentId, pdfPaths, request.getNumQuestions(), request.getDifficulty()
            );
            if (generated.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "error", "message", "❌ 퀴즈 생성 실패 (python 서버 응답 없음)"));
            }

            // DB 저장
            List<Quiz> saved = quizService.saveGeneratedQuizSet(content, generated);
            int batch = saved.isEmpty() ? -1 : saved.get(0).getQuizBatch();

            // 응답 변환
            List<Map<String, Object>> quizList = new ArrayList<>();
            for (Quiz q : saved) {
                List<String> options = objectMapper.readValue(q.getOptions(), List.class);
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
                    "status", "success",
                    "message", "✅ 퀴즈 생성 및 저장 완료",
                    "batch", batch,
                    "generatedCount", quizList.size(),
                    "quizzes", quizList
            ));

        } catch (Exception e) {
            log.error("🚨 퀴즈 생성 오류", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", "서버 내부 오류"));
        }
    }

    // ✅ (2) 퀴즈 조회 (batch 지정 가능)
    @GetMapping("")
    public ResponseEntity<?> getQuizzes(
            @PathVariable Long contentId,
            @RequestParam(value = "batch", required = false) Integer batchParam
    ) {
        try {
            Content content = contentService.findById(contentId);
            if (content == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "❌ 콘텐츠를 찾을 수 없습니다."));

            List<Quiz> quizzes;
            int batchUsed;
            if (batchParam != null) {
                quizzes = quizService.getQuizzesByContentAndBatch(content, batchParam);
                batchUsed = batchParam;
            } else {
                batchUsed = quizService.getLatestBatchForContent(content);
                quizzes = quizService.getLatestBatchQuizzes(content);
            }

            if (quizzes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "❌ 해당 회차의 퀴즈가 없습니다.", "batch", batchUsed));
            }

            List<Map<String, Object>> resultList = new ArrayList<>();
            for (Quiz q : quizzes) {
                List<String> options = objectMapper.readValue(q.getOptions(), List.class);
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
                    "status", "success",
                    "content_id", contentId,
                    "quiz_batch", batchUsed,
                    "quiz_count", resultList.size(),
                    "quizzes", resultList
            ));
        } catch (Exception e) {
            log.error("🚨 퀴즈 조회 오류", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", "퀴즈 조회 중 오류 발생"));
        }
    }

    // ✅ (3) 채점 + 저장 + Progress 갱신  ← attempt_id 포함 응답으로 변경
    @PostMapping("/grade")
    public ResponseEntity<?> gradeQuiz(
            @PathVariable Long contentId,
            @RequestParam(value = "batch", required = false) Integer batchParam,
            @RequestBody QuizGradeRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails
    ) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "error", "message", "❌ 로그인 필요"));
            }

            Content content = contentService.findById(contentId);
            if (content == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "❌ 콘텐츠 없음"));

            int batchToUse = (batchParam != null) ? batchParam : quizService.getLatestBatchForContent(content);
            List<Quiz> quizzes = quizService.getQuizzesByContentAndBatch(content, batchToUse);
            if (quizzes.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "❌ 해당 회차 퀴즈 없음", "batch", batchToUse));

            // ✅ 채점
            Map<String, Object> graded = quizService.gradeQuizLocally(quizzes, request.getAnswers());

            // ✅ Progress 조회 or 생성
            Progress progress = progressService.findProgressByContentId(contentId);
            if (progress == null) {
                if (content.getUser() == null)
                    return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "❌ 콘텐츠 user 정보 없음"));

                progress = progressService.createProgressIfNotExists(content.getUser().getId(), contentId);
            }

            // ✅ QuizAttempt 저장 (attempt_id 확보)
            QuizAttempt attempt = quizService.saveQuizAttempt(progress, graded, batchToUse);

            // ✅ Progress 점수 갱신
            Float score = ((Number) graded.get("final_total_score")).floatValue();
            Long userId = (progress.getUser() != null) ? progress.getUser().getId() : content.getUser().getId();
            progressService.updateProgressAfterQuiz(userId, contentId, score);

            // ✅ 응답: attempt_id + 요약 + 문항 목록(응답으로만)
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "success");
            body.put("message", "✅ 채점 및 저장 완료");
            body.put("attempt_id", attempt.getId());
            body.put("content_id", contentId);
            body.put("batch", batchToUse);
            body.put("final_total_score", graded.get("final_total_score"));
            body.put("correct_count", graded.get("correct_count"));
            body.put("total_questions", graded.get("total_questions"));
            body.put("results", graded.get("results")); // ← 프론트 결과 페이지가 바로 사용 (재조회 불필요)

            return ResponseEntity.ok(body);

        } catch (Exception e) {
            log.error("🚨 채점 오류", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", "채점 중 오류 발생"));
        }
    }

    // ✅ (4) 시도 기록 조회 (정렬 보장 추가)
    @GetMapping("/attempts")
    public ResponseEntity<?> getQuizAttempts(
            @PathVariable Long contentId,
            @RequestParam(value = "batch", required = false) Integer batchParam
    ) {
        try {
            Content content = contentService.findById(contentId);
            if (content == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "❌ 콘텐츠 없음"));

            Progress progress = progressService.findProgressByContentId(contentId);
            if (progress == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "❌ Progress 없음"));

            List<QuizAttempt> attempts = quizService.getAttemptsByProgress(progress);
            if (batchParam != null) {
                attempts = attempts.stream()
                        .filter(a -> a.getQuizBatch().equals(batchParam))
                        .toList();
            }

            // 최신 우선 정렬 (created_at DESC, id DESC)
            attempts = attempts.stream()
                    .sorted((a, b) -> {
                        int t = (b.getCreatedAt() != null && a.getCreatedAt() != null)
                                ? b.getCreatedAt().compareTo(a.getCreatedAt())
                                : 0;
                        if (t != 0) return t;
                        return Long.compare(
                                Optional.ofNullable(b.getId()).orElse(0L),
                                Optional.ofNullable(a.getId()).orElse(0L)
                        );
                    })
                    .toList();

            if (attempts.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "❌ 해당 회차 결과 없음"));

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
                    "status", "success",
                    "attempt_count", resultList.size(),
                    "attempts", resultList
            ));
        } catch (Exception e) {
            log.error("🚨 시도 기록 조회 오류", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", "조회 중 오류 발생"));
        }
    }

    // ✅ (5) 대시보드용: 특정 attempt 요약 단건 조회 (정답률 안정 표시용)
    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<?> getAttemptSummary(
            @PathVariable Long contentId,  // contentId는 경로상 존재하지만 여기선 검증만(옵션)
            @PathVariable Long attemptId
    ) {
        try {
            Optional<QuizAttempt> opt = quizService.getAttemptById(attemptId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "❌ 해당 시도를 찾을 수 없습니다."));
            }
            QuizAttempt a = opt.get();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("attempt_id", a.getId());
            body.put("content_id", contentId);
            body.put("quiz_batch", a.getQuizBatch());
            body.put("score", a.getScore());                 // 0~100 (%)
            body.put("correct_answers", a.getCorrectAnswers());
            body.put("total_questions", a.getTotalQuestions());
            body.put("created_at", a.getCreatedAt());

            return ResponseEntity.ok(body);
        } catch (CustomException ce) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", ce.getMessage()));
        } catch (Exception e) {
            log.error("🚨 attempt 요약 조회 오류", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", "시도 조회 중 오류 발생"));
        }
    }
}
