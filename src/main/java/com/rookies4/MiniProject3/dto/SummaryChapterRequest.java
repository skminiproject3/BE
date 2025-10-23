package com.rookies4.MiniProject3.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SummaryChapterRequest {
    // 요약 요청할 챕터 번호 (예: "4.1" 또는 "4"). 전체 요약 시 null
    private String chapterRequest; 
}