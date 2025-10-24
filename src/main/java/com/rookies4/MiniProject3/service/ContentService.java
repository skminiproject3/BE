package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.domain.enums.ContentStatus;
import com.rookies4.MiniProject3.dto.ContentDto;
import com.rookies4.MiniProject3.exception.CustomException;
import com.rookies4.MiniProject3.exception.ErrorCode;
import com.rookies4.MiniProject3.repository.ContentRepository;
import com.rookies4.MiniProject3.repository.ProgressRepository;
import com.rookies4.MiniProject3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

    private final ContentRepository contentRepository;
    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PythonServerClient pythonServerClient;

    // ==========================================================
    //  파일 업로드 + FastAPI 전송 + 챕터 감지 + vectorPath 저장
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

        // 콘텐츠 생성
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
            // ✅ FastAPI 호출: PDF 업로드 + 벡터화 + 챕터 감지
            Map<String, Object> fastApiResponse =
                    pythonServerClient.uploadPdfAndVectorize(content.getId(), storedFilePath);

            int totalChapters = 0;
            String vectorPath = null;

            if (fastApiResponse != null) {
                // total_chapters 파싱
                if (fastApiResponse.get("total_chapters") != null) {
                    try {
                        totalChapters = Integer.parseInt(fastApiResponse.get("total_chapters").toString());
                    } catch (Exception e) {
                        log.warn("⚠️ total_chapters 변환 실패: {}", fastApiResponse.get("total_chapters"));
                    }
                }
                // vector_path 파싱
                if (fastApiResponse.get("vector_path") != null) {
                    vectorPath = fastApiResponse.get("vector_path").toString();
                }
            }

            // ✅ DB 반영
            content.updateTotalChapters(totalChapters);
            content.setVectorPath(vectorPath);
            content.changeStatus(ContentStatus.COMPLETED);
            contentRepository.saveAndFlush(content);

            log.info("✅ 업로드/분석 완료 | contentId={} | total_chapters={} | vectorPath={}",
                    content.getId(), totalChapters, vectorPath);

            // ✅ Progress 자동 생성
            createProgressForUserAndContent(content.getId());

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
    // ✅ Vector Path 업데이트 (FastAPI → Spring)
    // ==========================================================
    @Transactional
    public void updateVectorPath(Long contentId, String vectorPath) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        content.setVectorPath(vectorPath);
        contentRepository.save(content);

        log.info("✅ vectorPath 업데이트 완료 | contentId={} | path={}", contentId, vectorPath);
    }

    // ==========================================================
    // 단일 콘텐츠 조회
    // ==========================================================
    public Content findById(Long id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Content not found with id: " + id));
    }

    // ==========================================================
    // 콘텐츠 전체 목록 조회
    // ==========================================================
    public List<Content> getAllContents() {
        return contentRepository.findAll();
    }

    // ==========================================================
    // 상태 업데이트 (예: 요약 완료 등)
    // ==========================================================
    @Transactional
    public void updateStatus(Long contentId, ContentStatus status) {
        Optional<Content> optional = contentRepository.findById(contentId);
        if (optional.isPresent()) {
            Content content = optional.get();
            content.setStatus(status);
            contentRepository.save(content);
            log.info("🔄 콘텐츠 상태 변경 | contentId={} | status={}", contentId, status);
        } else {
            log.warn("⚠️ updateStatus: 콘텐츠 ID {} 없음", contentId);
        }
    }

    // ==========================================================
    // ✅ Progress 조회 + 없으면 자동 생성
    // ==========================================================
    @Transactional
    public Progress findProgressByContentId(Long contentId) {
        try {
            Optional<Progress> existing = progressRepository.findByContent_Id(contentId);
            if (existing.isPresent()) {
                log.info("✅ 기존 Progress 조회 완료 (id={})", existing.get().getId());
                return existing.get();
            }

            // 없을 경우 새로 생성
            log.warn("⚠️ Progress 없음 → 새로 생성 시도 (contentId={})", contentId);
            return createProgressForUserAndContent(contentId);

        } catch (Exception e) {
            log.error("🚨 Progress 조회 실패 (contentId={}): {}", contentId, e.getMessage());
            return null;
        }
    }

    // ==========================================================
    // ✅ Progress 자동 생성 (quiz_attempts 연동용)
    // ==========================================================
    @Transactional
    public Progress createProgressForUserAndContent(Long contentId) {
        try {
            Content content = contentRepository.findById(contentId)
                    .orElseThrow(() -> new RuntimeException("❌ 콘텐츠를 찾을 수 없습니다. id=" + contentId));

            User user = content.getUser();
            if (user == null) {
                log.error("❌ Progress 생성 실패: 콘텐츠에 연결된 User가 없습니다. (contentId={})", contentId);
                return null;
            }

            Progress progress = Progress.builder()
                    .user(user)
                    .content(content)
                    .completedChapters(0)
                    .averageScore(0f)
                    .build();

            Progress saved = progressRepository.save(progress);
            log.info("🆕 Progress 새로 생성 완료: id={}, user={}, contentId={}",
                    saved.getId(), user.getUsername(), contentId);

            return saved;

        } catch (Exception e) {
            log.error("🚨 Progress 생성 실패 (contentId={}): {}", contentId, e.getMessage());
            return null;
        }
    }
}
