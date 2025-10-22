package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.dto.ai.AiChapterSummaryRequest;
import com.rookies4.MiniProject3.dto.ai.AiChapterSummaryResponse;
import com.rookies4.MiniProject3.dto.ai.AiFullSummaryResponse;
import com.rookies4.MiniProject3.dto.ai.AiPdfPathsRequest;
import com.rookies4.MiniProject3.dto.ai.AiUploadResponse;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

// AI 서버와의 API 통신을 전담하는 클래스.
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final WebClient aiWebClient;
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(10); // AI 파일처리 타임아웃
    private static final Duration SUMMARY_TIMEOUT = Duration.ofMinutes(5); // AI 요약 타임아웃

    /**
     * AI 에게 파일 벡터화 요청 -> ContentService 클래스가 호출
     * @param contentId 백엔드 DB의 콘텐츠 ID
     * @param fileResource AI 에게 전달할 실제 파일 리소스
     */
    public AiUploadResponse processAndVectorize(Long contentId, Resource fileResource) {
        log.info("[AI 통신] contentId: {} 파일 벡터화 요청 시작...", contentId);

        try {
            AiUploadResponse response = aiWebClient.post()
                    .uri("/upload_pdfs/")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData("files", fileResource))
                    .retrieve()
                    .bodyToMono(AiUploadResponse.class) // AI의 응답(저장된 경로)을 받음
                    .timeout(REQUEST_TIMEOUT)
                    .block(); // 동기 대기

            if (response == null || response.getPdfPaths() == null || response.getPdfPaths().isEmpty()) {
                throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
            }

            log.info("[AI 통신] contentId: {} 파일 벡터화 요청 성공! AI 서버 경로: {}", contentId, response.getPdfPaths().get(0));
            return response;

        } catch (WebClientResponseException e) {
            log.error("[AI 통신] AI 서버가 벡터화 실패 응답 (Status: {}, Body: {})",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
        } catch (Exception e) {
            log.error("[AI 통신] 벡터화 요청 중 알 수 없는 오류 발생", e);
            throw new CustomException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    // AI 서버에 "전체 요약" 생성 요청
    public AiFullSummaryResponse generateFullSummary(List<String> pdfPaths) {
        log.info("[AI 통신] 전체 요약 요청 시작. Paths: {}", pdfPaths);
        AiPdfPathsRequest request = new AiPdfPathsRequest(pdfPaths);

        try {
            return aiWebClient.post()
                    .uri("/summarize/full")
                    .body(BodyInserters.fromValue(request))
                    .retrieve()
                    .bodyToMono(AiFullSummaryResponse.class)
                    .timeout(SUMMARY_TIMEOUT)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("[AI 통신] AI 서버가 전체 요약 실패 응답 (Status: {}, Body: {})",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
        } catch (Exception e) {
            log.error("[AI 통신] 전체 요약 요청 중 알 수 없는 오류 발생", e);
            throw new CustomException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    // AI 서버에 "단원별 요약" 생성 요청
    public AiChapterSummaryResponse generateChapterSummary(List<String> pdfPaths, int chapter) {
        log.info("[AI 통신] 단원별 요약 요청 시작. Paths: {}, Chapter: {}", pdfPaths, chapter);

        AiChapterSummaryRequest request = new AiChapterSummaryRequest(pdfPaths, String.valueOf(chapter));

        try {
            return aiWebClient.post()
                    .uri("/summarize/chapter")
                    .body(BodyInserters.fromValue(request))
                    .retrieve()
                    .bodyToMono(AiChapterSummaryResponse.class)
                    .timeout(SUMMARY_TIMEOUT)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("[AI 통신] AI 서버가 단원별 요약 실패 응답 (Status: {}, Body: {})",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
        } catch (Exception e) {
            log.error("[AI 통신] 전체 요약 요청 중 알 수 없는 오류 발생", e);
            throw new CustomException(ErrorCode.AI_SERVER_ERROR);
        }
    }
}
