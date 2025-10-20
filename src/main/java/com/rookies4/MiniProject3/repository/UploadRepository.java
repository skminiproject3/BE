package com.rookies4.MiniProject3.repository;

import com.rookies4.MiniProject3.domain.entity.Upload;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.domain.entity.Upload;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UploadRepository extends JpaRepository<Upload, Long> {
    // 특정 사용자의 업로드 목록 조회
    List<Upload> findByUser(User user);
}