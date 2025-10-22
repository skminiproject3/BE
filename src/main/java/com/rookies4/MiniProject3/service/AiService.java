package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * AI 서버와의 API 통신을 전담하는 클래스.
 * - ContentService 클래스: processAndVectorize() 메서드 호출
 * - QuizService 클래스: generateQuiz() 메서드 호출
 * - SummaryService 클래스: generateSummary() 메서드 호출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final WebClient aiWebClient;

    /**
     * AI 에게 파일 벡터화 요청 -> ContentService 클래스가 호출
     * @param contentId 백엔드 DB의 콘텐츠 ID
     * @param title 사용자가 첨부한 pdf 제목
     * @param fileResource AI 에게 전달할 실제 파일 리소스
     */
    public void processAndVectorize(Long contentId, String title, Resource fileResource) {
        log.info("[AI 통신] contentId: {}, title: {} 파일 벡터화 요청 시작...", contentId, title);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        // 저장된 파일 이름(UUID)을 가져온다. (null 일 경우 contentId 기반 임시파일 생성)
        String storedFileName = Optional.ofNullable(fileResource.getFilename())
                .orElseGet(() -> contentId + "_unknown.pdf");

        // 'files' 필드: 실제 파일 리소스 (FastAPI 의 List<UploadFile> 필드명)
        builder.part("files", fileResource).filename(storedFileName);

        // 'request' 필드: FastAPI 의 요청 모델에 맞춘 JSON 문자열
        String aiExpectedJson = "{\"pdf_paths\":[\"" + storedFileName + "\"]}";
        builder.part("request", aiExpectedJson)
                .header("Content-Type", "application/json");
        builder.part("title", title);

        try {
            aiWebClient.post()
                    .uri("/upload_pdfs")
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve() // 응답 받기
                    .bodyToMono(void.class) // AI가 반환한 응답 바디를 백엔드에서 사용하지 않으므로 void 처리
                    .timeout(Duration.ofMinutes(10)) // 벡터화는 오래 걸릴 수 있음 -> 타임아웃 길게 설정
                    .block(); // AI 작업이 끝날 때까지 동기식으로 대기

            log.info("[AI 통신] contentId: {} 파일 벡터화 요청 성공!", contentId);
        } catch (WebClientResponseException e) {
            // AI 서버가 4xx, 5xx 에러를 반환한 경우
            log.error("[AI 통신] AI 서버가 벡터화 실패 응답 (Status: {}, Body: {})",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
        } catch (Exception e) {
            // 네트워크 오류 등 기타 오류
            log.error("[AI 통신] 벡터화 요청 중 알 수 없는 오류 발생", e);
            throw new CustomException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    // 퀴즈 생성 요청 메서드(generateQuiz) 만들기 -> QuizService 클래스가 호출

    // 요약 생성 요청 메서드(generateSummary) 만들기 -> SummaryService 클래스가 호출
}
