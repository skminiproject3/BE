package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Summary;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.domain.enums.ContentStatus;
import com.rookies4.MiniProject3.dto.ContentDto;
import com.rookies4.MiniProject3.dto.SummaryDto;
import com.rookies4.MiniProject3.dto.SummaryDto.Response;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.repository.ContentRepository;
import com.rookies4.MiniProject3.repository.SummaryRepository;
import com.rookies4.MiniProject3.repository.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final SummaryRepository summaryRepository;
    private final FileStorageService fileStorageService;

    // 파일 업로드 및 처리 요청 메서드
    @Transactional
    public ContentDto.UploadResponse uploadAndProcessFile(MultipartFile file, String title, Long userId) {
        // 파일이 비어있는지 검사
        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.FILE_NOT_ATTACHED);
        }

        // 유저가 존재하는지 검사
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 파일 저장
        String originalFileName = file.getOriginalFilename();
        String storedFilePath = fileStorageService.store(file);

        // Content 엔티티 생성 및 DB 저장 (상태: PROCESSING)
        Content content = Content.builder()
                .user(user)
                .title(title)
                .fileName(originalFileName)
                .filePath(storedFilePath)
                .status(ContentStatus.PROCESSING)
                .build();

        Content savedContent = contentRepository.save(content);

        // 비동기로 AI 처리 시작
        processContentAsync(savedContent.getId());

        return new ContentDto.UploadResponse(savedContent.getId(), savedContent.getTitle(),
                savedContent.getStatus().name());
    }

    @Async // 이 메소드는 별도의 스레드에서 비동기적으로 실행됨
    @Transactional
    public void processContentAsync(Long contentId) {
        log.info("[Async] 업로드 파일(contentId: {})에 대한 AI 작업 시작...", contentId);

        // DB에서 AI 처리를 할 Content 엔티티 조회
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        try {
            // TODO: 실제 AI 처리 로직 구현하기 (PDF 파싱, 텍스트 추출, LLM API 호출 등)
            // 아래는 5초가 걸리는 AI 처리 시뮬레이션
            Thread.sleep(5000);

            // AI 처리 결과(요약, 챕터 등)를 DB에 저장
            // --- 시뮬레이션 데이터 생성 ---
            Summary summary1 = Summary.builder().content(content).chapter(1).summaryText("1챕터 요약 내용입니다...").build();
            Summary summary2 = Summary.builder().content(content).chapter(2).summaryText("2챕터 요약 내용입니다...").build();
            summaryRepository.saveAll(Arrays.asList(summary1, summary2));

            content.updateTotalChapters(2); // 분석된 총 챕터 수 업데이트
            // --- 시뮬레이션 종료 ---

            // 처리 완료 후 상태를 COMPLETED 로 변경
            content.changeStatus(ContentStatus.COMPLETED);

            // 상태가 COMPLETED 로 변경된 Content 엔티티를 DB에 저장 (상태 업데이트)
            contentRepository.save(content);

            log.info("[Async] 업로드 파일(contentId: {})에 대한 AI 작업 완료!", contentId);

        } catch (Exception e) {
            log.error("[ERROR] 업로드 파일(contentId: {})에 대한 AI 작업 실패", contentId, e);
            content.changeStatus(ContentStatus.FAILED);
            contentRepository.save(content); // // 상태가 FAILED 로 변경된 Content 엔티티를 DB에 저장 (상태 업데이트)
        }
    }

    public ContentDto.StatusResponse getContentStatus(Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        return new ContentDto.StatusResponse(content.getStatus().name());
    }

    public List<Response> getSummaries(Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        if (content.getStatus() != ContentStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PROCESSING_NOT_COMPLETED);
        }

        List<Summary> summaries = summaryRepository.findByContentIdOrderByChapterAsc(contentId);

        return summaries.stream()
                .map(summary -> new SummaryDto.Response(summary.getChapter(), summary.getSummaryText()))
                .collect(Collectors.toList());
    }
}