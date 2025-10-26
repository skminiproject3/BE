package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {

    /** ✅ 기존 방식 (User 객체 필요) */
    List<Content> findAllByUser(com.rookies4.MiniProject3.domain.entity.User user);

    /** ✅ 더 편리한 방식 (userId로 바로 조회 가능) */
    List<Content> findByUser_Id(Long userId);  // user.id 기준으로 탐색
}
