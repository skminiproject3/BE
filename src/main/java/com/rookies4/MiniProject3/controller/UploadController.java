package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.domain.entity.Upload;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    // 파일 업로드
    @PostMapping("/upload")
    public ResponseEntity<Upload> uploadFile(@RequestParam User user,
                                             @RequestParam MultipartFile file) {
        // 실제 저장 로직은 별도 서비스 또는 util에서 구현
        String filePath = "/upload/" + file.getOriginalFilename();
        Upload upload = uploadService.saveUpload(user, file.getOriginalFilename(),
                filePath, file.getSize(), file.getContentType());
        return ResponseEntity.ok(upload);
    }

    // 사용자 업로드 리스트 조회
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Upload>> getUserUploads(@PathVariable User user) {
        List<Upload> uploads = uploadService.getUserUploads(user);
        return ResponseEntity.ok(uploads);
    }
}