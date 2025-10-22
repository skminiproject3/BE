package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.domain.enums.ContentStatus;
import com.rookies4.MiniProject3.dto.ContentDto;
import com.rookies4.MiniProject3.dto.ai.AiUploadResponse;
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

        // 파일 저장 (UUID 포함된 파일명)
        String originalFileName = file.getOriginalFilename();
        String storedFileName = fileStorageService.store(file);

        // Content 엔티티 생성 및 DB 저장 (상태: PROCESSING)
        Content content = Content.builder()
                .user(user)
                .title(title)
                .fileName(originalFileName)
                .filePath(storedFileName)
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


        try {
            // 1. 파일 로드 (DB 에서 엔티티 조회)
            Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

            String storedFileName = content.getFilePath();
            Resource fileResource = fileStorageService.loadAsResource(storedFileName);

            // 2. AI 서비스 호출 (AiUploadResponse 응답 받기)
            AiUploadResponse response = aiService.processAndVectorize(contentId, fileResource);

            // 3. AI 서버 경로 추출
            if (response == null || response.getPdfPaths() == null || response.getPdfPaths().isEmpty()) {
                throw new CustomException(ErrorCode.AI_PROCESSING_FAILED);
            }
            String aiServerPath = response.getPdfPaths().get(0);

            // 4. 처리 완료
            updateContentSuccess(contentId, aiServerPath);

            log.info("[Async] 업로드 파일(contentId: {})에 대한 AI 작업 완료!", contentId);

        } catch (Exception e) {
            log.error("[ERROR] 업로드 파일(contentId: {})에 대한 AI 작업 실패", contentId, e);
            updateContentFailed(contentId);
        }
    }

    // AI 처리 성공 시 호출
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateContentSuccess(Long contentId, String aiServerPath) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        content.changeStatus(ContentStatus.COMPLETED); // 상태 변경
        content.setAiServerPath(aiServerPath); // AI 경로 저장

        contentRepository.save(content);
    }

    // AI 처리 실패 시 호출
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateContentFailed(Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        content.changeStatus(ContentStatus.FAILED); // 상태 변경

        contentRepository.save(content);
    }

    // AI 처리 상태 조회 메서드 -> 프론트가 이 API를 주기적으로 호출
    public ContentDto.StatusResponse getContentStatus(Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        return new ContentDto.StatusResponse(content.getStatus().name());
    }
}