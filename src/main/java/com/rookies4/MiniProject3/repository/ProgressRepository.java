package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProgressRepository extends JpaRepository<Progress, Long> {

    // ✅ 사용자 + 콘텐츠 기준 진행 기록 조회 (1명 사용자가 1개의 콘텐츠를 얼마나 봤는지)
    Optional<Progress> findByUserAndContent(User user, Content content);

    // ✅ 특정 사용자 전체 진행 기록 조회
    List<Progress> findByUser(User user);

    // ✅ contentId 기준으로 진행 기록 조회 (quiz_attempts 저장용)
    // 기존 findByContentId → JPA 표준 필드명 방식으로 수정
    Optional<Progress> findByContent_Id(Long contentId);
}
