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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PythonServerClient pythonServerClient;

    // ==========================================================
    //  파일 업로드 + FastAPI 전송 + 챕터 감지까지만 수행
    // ==========================================================
    @Transactional
    public ContentDto.UploadResponse uploadFile(MultipartFile file, String title, Long userId) {
        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.FILE_NOT_ATTACHED);
        }

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 파일 저장
        String originalFileName = file.getOriginalFilename();
        String storedFilePath = fileStorageService.store(file);

        // 콘텐츠 엔티티 생성 및 저장
        Content content = Content.builder()
                .user(user)
                .title(title)
                .fileName(originalFileName)
                .filePath(storedFilePath)
                .status(ContentStatus.PROCESSING)
                .build();

        contentRepository.saveAndFlush(content);
        log.info("📦 Content 생성 완료 | id={} | title={}", content.getId(), title);

        try {
            Map<String, Object> fastApiResponse =
                    pythonServerClient.uploadPdfAndVectorize(content.getId(), storedFilePath);

            int totalChapters = 0;
            String vectorPath = null;

            if (fastApiResponse != null) {
                if (fastApiResponse.get("total_chapters") != null) {
                    totalChapters = Integer.parseInt(fastApiResponse.get("total_chapters").toString());
                }
                if (fastApiResponse.get("vector_path") != null) {
                    vectorPath = fastApiResponse.get("vector_path").toString();
                }
            }

            // ✅ DB 반영
            content.updateTotalChapters(totalChapters);
            content.changeStatus(ContentStatus.COMPLETED);
            content.setVectorPath(vectorPath);
            contentRepository.saveAndFlush(content);

            log.info("✅ 업로드/분석 완료 | contentId={} | total_chapters={} | vectorPath={}",
                    content.getId(), totalChapters, vectorPath);

            // ✅ DTO 반환
            return new ContentDto.UploadResponse(
                    content.getId(),
                    content.getTitle(),
                    content.getStatus().name(),
                    totalChapters,
                    vectorPath
            );

        } catch (Exception e) {
            log.error("❌ FastAPI 업로드 실패 | contentId={} | error={}", content.getId(), e.getMessage());
            content.changeStatus(ContentStatus.FAILED);
            contentRepository.saveAndFlush(content);
            throw new CustomException(ErrorCode.FAILED_TO_PROCESS_CONTENT);
        }
    }

    // ==========================================================
    // 콘텐츠 상태 조회
    // ==========================================================
    public ContentDto.StatusResponse getContentStatus(Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));
        return new ContentDto.StatusResponse(content.getStatus().name());
    }

    // ==========================================================
    // PDF 경로 반환
    // ==========================================================
    public List<String> getPdfPaths(Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));
        return List.of(content.getFilePath());
    }

    // ==========================================================
    // Vector Path 업데이트
    // ==========================================================
    @Transactional
    public void updateVectorPath(Long contentId, String vectorPath) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        content.setVectorPath(vectorPath);
        contentRepository.save(content);

        log.info("✅ vectorPath 업데이트 완료 | contentId={} | path={}", contentId, vectorPath);
    }
}
