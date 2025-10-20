package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // 필요 시 이메일 존재 여부 확인
    boolean existsByEmail(String email);
}