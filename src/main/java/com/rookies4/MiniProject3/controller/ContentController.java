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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final PythonServerClient pythonClient;
    private final QuizService quizService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =====================================
    // 🧮 퀴즈 채점 요청 + quiz_attempts 저장
    // =====================================
    @PostMapping("/{contentId}/quiz/grade")
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

            // ✅ 로컬 채점 수행
            Map<String, Object> result = quizService.gradeQuizLocally(quizzes, request.getAnswers());

            // ✅ Progress 찾기 (현재는 임시로 첫 번째 progress 사용)
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
