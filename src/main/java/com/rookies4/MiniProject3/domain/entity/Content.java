package com.rookies4.MiniProject3.domain.entity;

import lombok.*;
import com.rookies4.MiniProject3.domain.enums.ContentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Column(length = 500)
    private String vectorPath;   // 벡터 DB 경로 추가

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

    // 빌더 생성자
    @Builder
    public Content(User user, String title, String fileName, String filePath, ContentStatus status) {
        this.user = user;
        this.title = title;
        this.fileName = fileName;
        this.filePath = filePath;
        this.status = status;
    }

    // 상태 변경
    public void changeStatus(ContentStatus status) {
        this.status = status;
    }

    // 챕터 수 변경
    public void updateTotalChapters(int totalChapters) {
        this.totalChapters = totalChapters;
    }

    // 벡터 경로 업데이트
    public void updateVectorPath(String vectorPath) {
        this.vectorPath = vectorPath;
    }
}
