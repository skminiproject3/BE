package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.ChatLog;
import com.rookies4.MiniProject3.domain.entity.Upload;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.domain.enums.Role;
import com.rookies4.MiniProject3.repository.ChatLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatLogService {

    private final ChatLogRepository chatLogRepository;

    public ChatLog saveChat(User user, Upload upload, Role role, String message, Float responseTime, Integer tokenUsage) {
        ChatLog chatLog = ChatLog.builder()
                .user(user)
                .upload(upload)
                .role(role)
                .message(message)
                .responseTime(responseTime)
                .tokenUsage(tokenUsage)
                .build();
        return chatLogRepository.save(chatLog);
    }

    public List<ChatLog> getUserChats(User user) {
        return chatLogRepository.findByUser(user);
    }

    public List<ChatLog> getUploadChats(Upload upload) {
        return chatLogRepository.findByUpload(upload);
    }
}