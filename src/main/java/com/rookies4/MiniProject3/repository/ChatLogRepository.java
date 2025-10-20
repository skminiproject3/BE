package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.ChatLog;
import com.rookies4.MiniProject3.domain.entity.Upload;
import com.rookies4.MiniProject3.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {
    // 사용자별 대화 로그 조회
    List<ChatLog> findByUser(User user);

    // 특정 업로드 문서 관련 대화 로그 조회
    List<ChatLog> findByUpload(Upload upload);
}