package com.rookies4.MiniProject3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies4.MiniProject3.domain.entity.*;
import com.rookies4.MiniProject3.dto.*;
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
    private final ProgressService progressService;
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

            // FastAPI 요청
            List<QuizResponseDto> generated = pythonClient.generateQuiz(
                    contentId, pdfPaths,
                    request.getNumQuestions(),
                    request.getDifficulty()
            );

            if (generated.isEmpty())
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "❌ 퀴즈 생성 실패 — Python 서버 응답 없음"));

            // DB 저장 (새 batch)
            List<Quiz> saved = quizService.saveGeneratedQuizSet(content, generated);
            int batch = saved.isEmpty() ? -1 : saved.get(0).getQuizBatch();

            // 결과 변환
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

    // ==========================================================
// ✅ 3. 회차별 채점 + quiz_attempts 저장 + Progress 갱신 (JWT 사용자 기반)
// ==========================================================
    @PostMapping("/grade")
    public ResponseEntity<?> gradeQuiz(
            @PathVariable Long contentId,
            @RequestParam(value = "batch", required = false) Integer batchParam,
            @RequestBody QuizGradeRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails // ✅ JWT 사용자 정보 주입
    ) {
        try {
            // ✅ 1️⃣ 현재 로그인 사용자 확인
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "❌ 로그인된 사용자 정보가 없습니다."));
            }

            String email = userDetails.getUsername(); // JWT의 subject (email)
            log.info("👤 로그인 사용자 인증 완료 | email={}", email);

            // ✅ 2️⃣ Content 확인
            Content content = contentService.findById(contentId);
            if (content == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ 콘텐츠를 찾을 수 없습니다."));

            int batchToUse = (batchParam != null)
                    ? batchParam
                    : quizService.getLatestBatchForContent(content);

            List<Quiz> quizzes = quizService.getQuizzesByContentAndBatch(content, batchToUse);
            if (quizzes.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ 해당 회차의 퀴즈가 없습니다.", "batch", batchToUse));

            // ✅ 3️⃣ 채점 수행
            Map<String, Object> result = quizService.gradeQuizLocally(quizzes, request.getAnswers());

            // ✅ 4️⃣ Progress 조회 또는 자동 생성
            Progress progress = progressService.findProgressByContentId(contentId);
            if (progress == null) {
                // 콘텐츠 업로드 시 생성 안 되어 있으면 자동 생성
                User user = content.getUser(); // 콘텐츠 작성자 기준
                if (user == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "❌ 콘텐츠에 연결된 사용자 정보가 없습니다."));
                }
                progress = progressService.createProgressIfNotExists(user.getId(), contentId);
                log.info("🆕 Progress 자동 생성 (contentId={}, userId={})", contentId, user.getId());
            }

            // ✅ 5️⃣ QuizAttempt 저장
            quizService.saveQuizAttempt(progress, result, batchToUse);
            log.info("✅ quiz_attempts 저장 완료 (progress_id={}, batch={})", progress.getId(), batchToUse);

            // ✅ 6️⃣ Progress 상태 및 점수 업데이트
            Float score = ((Number) result.get("final_total_score")).floatValue();

            Long userId = progress.getUser() != null
                    ? progress.getUser().getId()
                    : null;

            if (userId == null) {
                // progress에 user가 비어 있다면 JWT 이메일로 조회
                Optional<User> userOpt = content.getUser() != null
                        ? Optional.of(content.getUser())
                        : Optional.empty();
                if (userOpt.isEmpty()) {
                    log.warn("⚠️ Progress와 Content에 user가 연결되어 있지 않습니다. 이메일로 재조회 시도");
                    // 이메일로 유저 조회 (UserRepository를 progressService 내부에서 사용)
                    progress = progressService.createProgressIfNotExists(progress.getUser().getId(), contentId);
                } else {
                    userId = userOpt.get().getId();
                }
            }

            if (userId != null) {
                progressService.updateProgressAfterQuiz(userId, contentId, score);
                log.info("📊 Progress 업데이트 완료 | userId={} | score={}", userId, score);
            } else {
                log.warn("⚠️ 사용자 ID를 찾을 수 없어 Progress 업데이트 생략");
            }

            result.put("batch", batchToUse);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "✅ 채점 및 진행 현황 갱신 완료",
                    "batch", batchToUse,
                    "data", result
            ));

        } catch (Exception e) {
            log.error("🚨 채점 오류", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", "채점 중 오류 발생"));
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

            Progress progress = progressService.findProgressByContentId(contentId);
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
                    "status", "success",
                    "content_id", contentId,
                    "attempt_count", resultList.size(),
                    "attempts", resultList
            ));
        } catch (Exception e) {
            log.error("🚨 채점 결과 조회 오류", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", "채점 결과 조회 오류"));
        }
    }

}
