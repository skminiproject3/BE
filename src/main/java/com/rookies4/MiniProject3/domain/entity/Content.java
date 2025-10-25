package com.rookies4.MiniProject3.domain.entity;

import com.rookies4.MiniProject3.domain.enums.ContentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "contents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 업로드한 사용자 (FK) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 업로드한 파일명 (ex: lecture1.pdf) */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** 파일이 서버에 저장된 경로 */
    @Column(name = "file_path", nullable = false)
    private String filePath;

    /** 백터 DB 경로 (FastAPI 응답으로 전달됨) */
    @Column(name = "vector_path")
    private String vectorPath;

    /** 파일 제목 (사용자 지정) */
    @Column(name = "title", nullable = false)
    private String title;

    /** FastAPI 분석 결과 감지된 총 챕터 수 */
    @Column(name = "total_chapters")
    private Integer totalChapters;

    /** 콘텐츠 처리 상태 (PROCESSING, COMPLETED, FAILED 등) */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContentStatus status;

    /** 생성 시각 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 수정 시각 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** ====== 라이프사이클 콜백 ====== */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** ====== Progress 연관관계 ====== */
    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Progress> progresses;

    /** ====== 유틸리티 메서드 ====== */
    public void updateTotalChapters(int totalChapters) {
        this.totalChapters = totalChapters;
    }

    public void changeStatus(ContentStatus newStatus) {
        this.status = newStatus;
    }
}
