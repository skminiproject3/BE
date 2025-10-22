package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.enums.ContentStatus;
import com.rookies4.MiniProject3.dto.QuestionDto;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class QuestionService {

    private final WebClient aiWebClient;
    private final ContentRepository contentRepository;

    public QuestionDto.Response askQuestion(Long contentId, QuestionDto.Request request) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        // ⚠️ 사전처리 미완료 시 차단
        if (content.getStatus() != ContentStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PROCESSING_NOT_COMPLETED);
        }

        try {
            // ✅ PDF 파일 경로 확인
            String pdfPath = new File(content.getFilePath()).getAbsolutePath();
            log.info("📄 질문용 PDF 경로: {}", pdfPath);

            Map<String, Object> requestBody = Map.of(
                    "question", request.getQuestion(),
                    "pdf_paths", List.of(pdfPath)
            );

            // ✅ FastAPI로 요청 전송
            Map<String, Object> response = aiWebClient.post()
                    .uri("/question/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMinutes(3))
                    .block();

            log.info("✅ FastAPI 응답 수신: {}", response);

            if (response != null && response.containsKey("answer")) {
                return new QuestionDto.Response(request.getQuestion(), response.get("answer").toString());
            } else {
                throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
            }

        } catch (WebClientResponseException e) {
            log.error("[AI 질문 응답 오류] Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
        } catch (Exception e) {
            log.error("[AI 질문 처리 중 예외 발생]", e);
            throw new CustomException(ErrorCode.AI_SERVER_ERROR);
        }
    }
}
