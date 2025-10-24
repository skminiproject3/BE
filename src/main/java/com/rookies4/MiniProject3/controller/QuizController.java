package com.rookies4.MiniProject3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.dto.*;
import com.rookies4.MiniProject3.service.ContentService;
import com.rookies4.MiniProject3.service.PythonServerClient;
import com.rookies4.MiniProject3.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // =====================================
    // 🧠 퀴즈 자동 생성 (FastAPI 연동)
    // =====================================
    @PostMapping("/generate")
    public ResponseEntity<?> generateQuiz(
            @PathVariable Long contentId,
            @RequestBody QuizRequest request) {
        try {
            Content content = contentService.findById(contentId);
            if (content == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ 콘텐츠를 찾을 수 없습니다."));
            }

            List<String> pdfPaths = contentService.getPdfPaths(contentId);
            if (pdfPaths == null || pdfPaths.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "❌ PDF 경로가 존재하지 않습니다."));
            }

            List<QuizResponseDto> generated = pythonClient.generateQuiz(
                    pdfPaths,
                    request.getNumQuestions(),
                    request.getDifficulty()
            );

            if (generated.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "❌ 퀴즈 생성 실패 — Python 서버 응답 없음"));
            }

            List<Map<String, Object>> savedQuizList = new ArrayList<>();
            for (QuizResponseDto dto : generated) {
                String optionsJson = objectMapper.writeValueAsString(dto.getOptions());
                Quiz savedQuiz = quizService.saveQuiz(content, dto.getQuestion(), dto.getCorrectAnswer(), optionsJson, dto.getExplanation());

                Map<String, Object> quizData = new LinkedHashMap<>();
                quizData.put("quiz_id", savedQuiz.getQuizId()); // ✅ quiz_id 추가
                quizData.put("question", dto.getQuestion());
                quizData.put("options", dto.getOptions());
                quizData.put("correctAnswer", dto.getCorrectAnswer());
                quizData.put("explanation", dto.getExplanation());
                savedQuizList.add(quizData);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "✅ 퀴즈 생성 완료",
                    "generatedCount", savedQuizList.size(),
                    "difficulty", request.getDifficulty(),
                    "quizzes", savedQuizList
            ));

        } catch (Exception e) {
            log.error("🚨 퀴즈 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("errorCode", "INTERNAL_SERVER_ERROR", "message", "[ERROR] 서버 내부에 오류가 발생했습니다."));
        }
    }

    // =====================================
    // 🧮 퀴즈 채점 + quiz_attempts 저장
    // =====================================
    @PostMapping("/grade")
    public ResponseEntity<Map<String, Object>> gradeQuiz(
            @PathVariable Long contentId,
            @RequestBody QuizGradeRequest request) {

        try {
            Content content = contentService.findById(contentId);
            List<Quiz> quizzes = quizService.getQuizzesByContent(content);

            if (quizzes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ 해당 콘텐츠의 퀴즈가 없습니다."));
            }

            // ✅ 로컬 채점 수행 (quiz_id 포함됨)
            Map<String, Object> result = quizService.gradeQuizLocally(quizzes, request.getAnswers());

            Progress progress = contentService.findProgressByContentId(contentId);
            if (progress != null) {
                quizService.saveQuizAttempt(progress, result);
                log.info("✅ quiz_attempts 저장 완료 (progress_id={})", progress.getId());
            } else {
                log.warn("⚠️ Progress 정보 없음 → quiz_attempts 저장 생략");
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("🚨 로컬 채점 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "채점 중 오류 발생"));
        }
    }
}
