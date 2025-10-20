package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Summary;
import com.rookies4.MiniProject3.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final SummaryRepository summaryRepository;

    public Summary saveSummary(Content content, String chapterTitle, String summaryText, String keySentences) {
        Summary summary = Summary.builder()
                .content(content)
                .chapterTitle(chapterTitle)
                .summaryText(summaryText)
                .keySentences(keySentences)
                .build();
        return summaryRepository.save(summary);
    }

    public List<Summary> getSummariesByUpload(Content content) {
        return summaryRepository.findByUpload(content);
    }
}