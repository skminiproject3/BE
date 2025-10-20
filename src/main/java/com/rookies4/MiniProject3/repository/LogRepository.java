package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Log;
import com.rookies4.MiniProject3.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogRepository extends JpaRepository<Log, Long> {
    // 사용자별 시스템 이벤트 조회
    List<Log> findByUser(User user);
}