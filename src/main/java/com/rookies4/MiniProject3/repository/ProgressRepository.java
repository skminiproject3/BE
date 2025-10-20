package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Progress;
import com.rookies4.MiniProject3.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProgressRepository extends JpaRepository<Progress, Long> {
    // 사용자와 문서 기준으로 진행 기록 조회
    Optional<Progress> findByUserAndUpload(User user, Upload upload);

    // 특정 사용자의 모든 진행 기록 조회
    List<Progress> findByUser(User user);
}