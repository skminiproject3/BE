package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.Upload;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.repository.ProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProgressRepository progressRepository;

    public Progress saveOrUpdateProgress(User user, Upload upload, int chapterCompleted, int totalChapters, float quizScore) {
        Optional<Progress> existing = progressRepository.findByUserAndUpload(user, upload);
        Progress progress = existing.orElseGet(() -> Progress.builder().user(user).upload(upload).build());

        progress.setChapterCompleted(chapterCompleted);
        progress.setTotalChapters(totalChapters);
        progress.setQuizScore(quizScore);
        progress.setLastAccessed(java.time.LocalDateTime.now());

        return progressRepository.save(progress);
    }

    public List<Progress> getUserProgress(User user) {
        return progressRepository.findByUser(user);
    }
}