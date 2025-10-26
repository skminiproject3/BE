package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Long> {

    /**
     * ✅ 특정 사용자(User)와 콘텐츠(Content) 조합으로 진행 기록 조회
     * - 한 사용자가 특정 콘텐츠를 얼마나 학습했는지 확인할 때 사용
     */
    Optional<Progress> findByUserAndContent(User user, Content content);

    /**
     * ✅ userId + contentId 기준으로 진행 기록 조회 (userId, contentId 값만 있을 때)
     * - Service 단에서 userId와 contentId만 전달되는 경우 사용
     */
    Optional<Progress> findByUser_IdAndContent_Id(Long userId, Long contentId);

    /**
     * ✅ 특정 사용자(User)의 전체 학습 진행 기록 조회
     * - 사용자 대시보드용 (학습 현황 카드)
     */
    List<Progress> findByUser(User user);

    /**
     * ✅ 특정 콘텐츠(contentId)의 전체 진행 기록 조회
     * - 관리자가 콘텐츠별 학습자 진행 현황을 조회할 때 사용
     */
    List<Progress> findByContent_Id(Long contentId);
}