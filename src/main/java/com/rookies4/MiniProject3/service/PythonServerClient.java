package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.dto.QuizGradeRequest;
import com.rookies4.MiniProject3.dto.SummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PythonServerClient {

    private final WebClient webClient;

    @Value("${backend.base-url:http://localhost:8080}") // ✅ Spring Boot 서버
    private String backendBaseUrl;

    // ======================================
    // FastAPI: PDF 업로드 + 벡터 생성 + 챕터 감지
    // ======================================
    public Map<String, Object> uploadPdfAndVectorize(Long contentId, String filePath) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.error("❌ 업로드할 파일을 찾을 수 없습니다: {}", filePath);
                resultMap.put("error", "file_not_found");
                return resultMap;
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("files", new FileSystemResource(file));

            log.info("📤 FastAPI 업로드 요청 시작 | contentId={} | path={}", contentId, filePath);

            // FastAPI로 업로드 요청
            Map<String, Object> response = webClient.post()
                    .uri("http://localhost:8000/upload_pdfs/") // FastAPI URL
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                log.error("❌ FastAPI 응답이 비어 있습니다.");
                resultMap.put("error", "empty_response");
                return resultMap;
            }

            log.info("✅ FastAPI 응답 수신 | contentId={} | response={}", contentId, response);
            return response;

        } catch (Exception e) {
            log.error("❌ FastAPI 업로드 및 벡터화 요청 실패 | {}", e.getMessage(), e);
            resultMap.put("error", e.getMessage());
            return resultMap;
        }
    }

    // ======================================
    // 전체 요약 요청
    // ======================================
    public String summarizeFull(Long contentId) {
        log.info("[AI 요약 요청] 전체 요약 요청 → contentId={}", contentId);
        try {
            return webClient.post()
                    .uri("/api/contents/{contentId}/summarize", contentId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("❌ 전체 요약 요청 실패: {}", e.getMessage());
            return "요약 실패";
        }
    }

    // ======================================
    // 단원별 요약 요청
    // ======================================
    public String summarizeByChapter(Long contentId, SummaryDto.ChapterRequest request) {
        log.info("[AI 요약 요청] 단원별 요약 요청 → contentId={} | chapter_request={}",
                contentId, request.getChapter_request());
        try {
            return webClient.post()
                    .uri("/api/contents/{contentId}/summaries", contentId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("❌ 단원별 요약 요청 실패: {}", e.getMessage());
            return "요약 실패";
        }
    }

    // ======================================
    // RAG 질문 요청
    // ======================================
    public String askQuestion(Long contentId, String question, boolean forceWeb) {
        try {
            // JSON Body 구성
            Map<String, Object> body = new HashMap<>();
            body.put("question", question);
            body.put("force_web", forceWeb);

            log.info("🧠 질문 요청 → contentId={} | question='{}' | forceWeb={}", contentId, question, forceWeb);

            return webClient.post()
                    .uri("/api/contents/{contentId}/ask", contentId)
                    .contentType(MediaType.APPLICATION_JSON)  // ✅ JSON 전송
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

        } catch (Exception e) {
            log.error("❌ 질문 요청 실패: {}", e.getMessage(), e);
            return "질문 실패";
        }
    }
    // ======================================
    // 퀴즈 생성 요청
    // ======================================
    public String generateQuiz(Long contentId, int numQuestions, String difficulty) {
        try {
            Map<String, Object> body = Map.of(
                    "num_questions", numQuestions,
                    "difficulty", difficulty
            );

            return webClient.post()
                    .uri("/api/contents/{contentId}/quiz/generate", contentId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("❌ 퀴즈 생성 요청 실패: {}", e.getMessage());
            return "퀴즈 생성 실패";
        }
    }

    // ======================================
    // 퀴즈 채점 (임시)
    // ======================================
    public Map<String, Object> gradeQuiz(List<QuizGradeRequest.Answer> answers) {
        Map<String, Object> result = new HashMap<>();
        result.put("score", 100);
        result.put("total", answers.size());
        return result;
    }
}
