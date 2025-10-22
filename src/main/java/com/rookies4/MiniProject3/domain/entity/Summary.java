package com.rookies4.MiniProject3.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "summaries")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK → contents.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(nullable = false)
    private Integer chapter;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summaryText;

    @Column(columnDefinition = "JSON")
    private String keySentences;

    // 빌더 패턴을 위한 생성자
    @Builder
    public Summary(Content content, int chapter, String summaryText, String keySentences) {
        this.content = content;
        this.chapter = chapter;
        this.summaryText = summaryText;
        this.keySentences = keySentences;
    }
}