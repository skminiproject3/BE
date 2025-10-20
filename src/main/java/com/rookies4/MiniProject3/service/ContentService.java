package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.repository.ContentRepository;
import com.rookies4.MiniProject3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ContentService {

    private final ContentRepository contentRepository;
    private final UserRepository userRepository;

    private static final String UPLOAD_DIR = "uploads/";

    // PDF 파일 업로드 및 AI 처리 요청
    public Content uploadFile(Long userId, MultipartFile file, String title) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("[ERROR] 처리할 파일을 첨부해주세요.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("[ERROR] 사용자 정보를 찾을 수 없습니다."));

        try {
            // 파일 저장
            Path path = Paths.get(UPLOAD_DIR + file.getOriginalFilename());
            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            Content content = Content.builder()
                    .user(user)
                    .title(title)
                    .fileName(file.getOriginalFilename())
                    .filePath(path.toString())
                    .status("PROCESSING")
                    .build();

            // AI 비동기 처리 로직은 추후 비동기 Task로 대체
            return contentRepository.save(content);

        } catch (Exception e) {
            throw new RuntimeException("[ERROR] 파일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    // 콘텐츠 상태 조회
    @Transactional(readOnly = true)
    public String getContentStatus(Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("[ERROR] 콘텐츠를 찾을 수 없습니다."));
        return content.getStatus();
    }

    // 챕터별 요약 목록 조회
    @Transactional(readOnly = true)
    public List<?> getSummaries(Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("[ERROR] 콘텐츠를 찾을 수 없습니다."));

        if (!"COMPLETED".equals(content.getStatus())) {
            throw new IllegalStateException("[ERROR] 콘텐츠 처리가 아직 완료되지 않았습니다.");
        }

        return content.getSummaries()
                .stream()
                .map(summary -> new Object() {
                    public final int chapter = summary.getChapter();
                    public final String summary_text = summary.getSummaryText();
                }).toList();
    }
}