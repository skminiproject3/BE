package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProgressRepository extends JpaRepository<Progress, Long> {

    // ✅ 사용자 + 콘텐츠 기준 진행 기록 조회
    Optional<Progress> findByUserAndContent(User user, Content content);

    // ✅ 특정 사용자 전체 진행 기록 조회
    List<Progress> findByUser(User user);

    // ✅ contentId 기준으로 진행 기록 조회 (quiz_attempts 저장용)
    Optional<Progress> findByContentId(Long contentId);
}
