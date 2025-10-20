package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.repository.ContentRepository;
import com.rookies4.MiniProject3.repository.ProgressRepository;
import com.rookies4.MiniProject3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class ProgressService {

    private final ProgressRepository progressRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;

    // 전체 학습 현황 조회
    @Transactional(readOnly = true)
    public List<?> getUserProgress(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("[ERROR] 사용자를 찾을 수 없습니다."));

        return progressRepository.findByUser(user).stream()
                .map(progress -> new Object() {
                    public final Long contentId = progress.getContent().getId();
                    public final String title = progress.getContent().getTitle();
                    public final double progress_percent = progress.calculateProgressPercent();
                    public final double average_score = progress.getAverageScore();
                }).toList();
    }

    // 챕터 완료 처리
    public Object completeChapter(Long userId, Long contentId, int chapterNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("[ERROR] 사용자 정보를 찾을 수 없습니다."));
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("[ERROR] 콘텐츠를 찾을 수 없습니다."));

        Progress progress = progressRepository.findByUserAndContent(user, content)
                .orElseThrow(() -> new IllegalArgumentException("[ERROR] 학습 현황을 찾을 수 없습니다."));

        progress.setCompletedChapters(progress.getCompletedChapters() + 1);
        progress.updateLastAccessed();
        progressRepository.save(progress);

        return new Object() {
            public final Long contentId_ = contentId;
            public final int completed_chapters = progress.getCompletedChapters();
            public final int total_chapters = content.getTotalChapters();
            public final double progress_percent = progress.calculateProgressPercent();
        };
    }
}