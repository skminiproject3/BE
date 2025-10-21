package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import java.time.Duration;
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

    // TODO: WebClient 주입
    // WebClient 는 별도의 @Configuration 파일에서 AI 서버의 baseUrl, API Key 헤더 등을 설정해 Bean으로 등록해야 한다.
    private final WebClient aiWebClient;

    /**
     * AI 에게 파일 벡터화 요청 -> ContentService 클래스가 호출
     * @param contentId 백엔드 DB의 콘텐츠 ID
     * @param fileResource AI에게 전달할 실제 파일 리소스
     */
    // TODO: 임시로 만든 processAndVectorize 메서드 완성하기
    public void processAndVectorize(Long contentId, Resource fileResource) {
        log.info("[AI 통신] contentId: {} 파일 벡터화 요청 시작...", contentId);

        // AI 서버가 multipart/form-data 를 받는다고 가정
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", fileResource);
        builder.part("contentId", String.valueOf(contentId)); // AI 서버도 contentID를 알 수 있게 전달

        try {
            aiWebClient.post()
                    .uri("/vectorize") //TODO: AI 서버의 벡터화 경로 수정 (현재는 임시로 /vectorize 로 해놓음)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve() // 응답 받기
                    .bodyToMono(void.class) // AI가 성공만 주면 응답 바디는 필요 없음
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
