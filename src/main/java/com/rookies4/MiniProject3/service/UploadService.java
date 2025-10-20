package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.repository.UploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final UploadRepository uploadRepository;

    // 문서 업로드 기록 저장
    public Content saveUpload(User user, String fileName, String filePath, Long fileSize, String fileType) {
        Content content = content.builder()
                .user(user)
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(fileSize)
                .fileType(fileType)
                .build();
        return uploadRepository.save(content);
    }

    public List<Content> getUserUploads(User user) {
        return uploadRepository.findByUser(user);
    }
}