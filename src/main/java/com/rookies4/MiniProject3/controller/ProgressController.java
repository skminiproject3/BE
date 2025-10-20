package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PostMapping("/update")
    public ResponseEntity<Progress> updateProgress(@RequestParam User user,
                                                   @RequestParam Content content,
                                                   @RequestParam int chapterCompleted,
                                                   @RequestParam int totalChapters,
                                                   @RequestParam float quizScore) {
        Progress progress = progressService.saveOrUpdateProgress(user, content, chapterCompleted, totalChapters, quizScore);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Progress>> getUserProgress(@PathVariable User user) {
        return ResponseEntity.ok(progressService.getUserProgress(user));
    }
}