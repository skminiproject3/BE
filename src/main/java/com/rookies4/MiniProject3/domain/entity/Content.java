package com.rookies4.MiniProject3.domain.entity;

import lombok.*;

import com.rookies4.MiniProject3.domain.enums.ContentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "contents")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column
    private Integer totalChapters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentStatus status;

    @CreatedDate
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    // 양방향 연관관계
    @OneToMany(mappedBy = "content")
    private List<Summary> summaries = new ArrayList<>();

    @OneToMany(mappedBy = "content")
    private List<Quiz> quizzes = new ArrayList<>();

    @OneToMany(mappedBy = "content")
    private List<Progress> progresses = new ArrayList<>();

    // 빌더 패턴을 위한 생성자
    @Builder
    public Content(User user, String title, String fileName, String filePath, ContentStatus status) {
        this.user = user;
        this.title = title;
        this.fileName = fileName;
        this.filePath = filePath;
        this.status = status;
    }

    // 상태 변경을 위한 메소드
    public void changeStatus(ContentStatus status) {
        this.status = status;
    }

    // 총 챕터 수 업데이트를 위한 메소드
    public void updateTotalChapters(int totalChapters) {
        this.totalChapters = totalChapters;
    }
}