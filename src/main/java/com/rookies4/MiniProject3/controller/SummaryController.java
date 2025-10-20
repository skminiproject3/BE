package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.domain.entity.Summary;
import com.rookies4.MiniProject3.domain.entity.Upload;
import com.rookies4.MiniProject3.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/summaries")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @PostMapping("/create")
    public ResponseEntity<Summary> createSummary(@RequestParam Upload upload,
                                                 @RequestParam String chapterTitle,
                                                 @RequestParam String summaryText,
                                                 @RequestParam(required = false) String keySentences) {
        Summary summary = summaryService.saveSummary(upload, chapterTitle, summaryText, keySentences);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/upload/{uploadId}")
    public ResponseEntity<List<Summary>> getSummaries(@PathVariable Upload upload) {
        List<Summary> summaries = summaryService.getSummariesByUpload(upload);
        return ResponseEntity.ok(summaries);
    }
}