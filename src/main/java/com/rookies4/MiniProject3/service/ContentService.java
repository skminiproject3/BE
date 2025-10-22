package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.domain.enums.ContentStatus;
import com.rookies4.MiniProject3.dto.ContentDto;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.repository.ContentRepository;
import com.rookies4.MiniProject3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AiService aiService;

    // 파일 업로드 및 사전처리 요청 메서드
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

    // AI 처리 메서드
    @Async
    public void processContentAsync(Long contentId) {
        log.info("[Async] 업로드 파일(contentId: {})에 대한 AI 작업 시작...", contentId);

        // DB 에서 AI 처리를 할 Content 엔티티 조회
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        try {
            // 1. 파일 로드
            String storedFileName = content.getFilePath();
            Resource fileResource = fileStorageService.loadAsResource(storedFileName);

            // 2. AI 서비스 호출 (파일 전달 & 벡터화 요청)
            aiService.processAndVectorize(contentId, fileResource);

            // 3. 처리 완료 후 상태를 COMPLETED 로 변경
            updateContentStatus(contentId, ContentStatus.COMPLETED);

            log.info("[Async] 업로드 파일(contentId: {})에 대한 AI 작업 완료!", contentId);

        } catch (Exception e) {
            log.error("[ERROR] 업로드 파일(contentId: {})에 대한 AI 작업 실패", contentId, e);

            updateContentStatus(contentId, ContentStatus.FAILED);
        }
    }

    // AI 처리 상태 변경 메서드
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateContentStatus(Long contentId, ContentStatus status) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        content.changeStatus(status);
        contentRepository.save(content); // 상태 변경(COMPLETED 또는 FAILED) 저장
    }

    // AI 처리 상태 조회 메서드 -> 프론트가 이 API를 주기적으로 호출
    public ContentDto.StatusResponse getContentStatus(Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        return new ContentDto.StatusResponse(content.getStatus().name());
    }
}