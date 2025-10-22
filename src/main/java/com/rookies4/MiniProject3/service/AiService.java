package com.rookies4.MiniProject3.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.enums.ContentStatus;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final WebClient aiWebClient;
    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * ✅ PDF 요약 요청 (FastAPI → /summarize/full)
     */
    public void processAndVectorize(Long contentId, Resource fileResource) {
        log.info("[AI 통신] contentId: {} → FastAPI 요약 요청 시작", contentId);

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        try {
            File file = fileResource.getFile();
            String absolutePath = file.getAbsolutePath();

            Map<String, Object> requestBody = Map.of("pdf_paths", List.of(absolutePath));

            // ✅ 무조건 문자열로 응답받기
            String rawResponse = aiWebClient.post()
                    .uri("/summarize/full")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMinutes(5))
                    .block();

            log.info("✅ FastAPI 요약 응답(raw): {}", rawResponse);
            if (rawResponse == null || rawResponse.isBlank()) {
                throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
            }

            Map<String, Object> response = objectMapper.readValue(rawResponse, Map.class);

            if (response.containsKey("summary")) {
                String summary = response.get("summary").toString();
                content.changeStatus(ContentStatus.COMPLETED);
                saveSummaryToDB(content, summary);
            } else {
                log.error("[AI 통신 실패] summary 키 없음: {}", response);
                content.changeStatus(ContentStatus.FAILED);
                contentRepository.save(content);
            }

        } catch (WebClientResponseException e) {
            log.error("[AI 통신 오류] Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            content.changeStatus(ContentStatus.FAILED);
            contentRepository.save(content);
            throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);

        } catch (Exception e) {
            log.error("[AI 통신 예외]", e);
            content.changeStatus(ContentStatus.FAILED);
            contentRepository.save(content);
            throw new CustomException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    /**
     * ✅ 퀴즈 생성 요청 (FastAPI → /quiz/generate)
     */
    public Map<String, Object> generateQuizFromAI(Long contentId, int count, String difficulty) {
        log.info("[AI 퀴즈 요청] contentId: {}, count: {}, difficulty: {}", contentId, count, difficulty);

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        String pdfPath = content.getFilePath();
        File file = new File(pdfPath);
        if (!file.isAbsolute()) {
            pdfPath = new File(uploadDir, pdfPath).getAbsolutePath();
        }

        File finalFile = new File(pdfPath);
        if (!finalFile.exists()) {
            log.error("❌ FastAPI로 보낼 파일이 존재하지 않습니다: {}", pdfPath);
            throw new CustomException(ErrorCode.FILE_NOT_ATTACHED);
        }

        log.info("📄 FastAPI로 전달할 파일 경로: {}", pdfPath);

        Map<String, Object> requestBody = Map.of(
                "pdf_paths", List.of(pdfPath),
                "num_questions", count,
                "difficulty", difficulty
        );

        try {
            // ✅ 문자열로 먼저 응답받기
            String rawResponse = aiWebClient.post()
                    .uri("/quiz/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMinutes(5))
                    .block();

            log.info("✅ FastAPI 퀴즈 응답(raw): {}", rawResponse);

            if (rawResponse == null || rawResponse.isBlank()) {
                log.error("❌ FastAPI에서 빈 응답을 수신했습니다.");
                throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
            }

            Map<String, Object> response = objectMapper.readValue(rawResponse, Map.class);
            return response;

        } catch (WebClientResponseException e) {
            log.error("[AI 퀴즈 통신 실패] Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);

        } catch (Exception e) {
            log.error("[AI 퀴즈 요청 실패]", e);
            throw new CustomException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    /**
     * ✅ 요약문 저장 (임시)
     */
    private void saveSummaryToDB(Content content, String summaryText) {
        try {
            log.info("📘 요약문 저장 완료 (contentId: {}): {}",
                    content.getId(),
                    summaryText.substring(0, Math.min(200, summaryText.length())));
            contentRepository.save(content);
        } catch (Exception e) {
            log.error("❌ 요약문 저장 중 오류 발생", e);
        }
    }
}
