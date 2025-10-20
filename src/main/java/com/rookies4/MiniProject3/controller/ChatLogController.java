package com.rookies4.MiniProject3.controller;


import com.rookies4.MiniProject3.domain.entity.ChatLog;
import com.rookies4.MiniProject3.domain.entity.Upload;
import com.rookies4.MiniProject3.domain.enums.Role;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.service.ChatLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatLogController {

    private final ChatLogService chatLogService;

    @PostMapping("/send")
    public ResponseEntity<ChatLog> sendChat(@RequestParam User user,
                                            @RequestParam Upload upload,
                                            @RequestParam Role role,
                                            @RequestParam String message,
                                            @RequestParam(required = false) Float responseTime,
                                            @RequestParam(required = false) Integer tokenUsage) {
        ChatLog chat = chatLogService.saveChat(user, upload, role, message, responseTime, tokenUsage);
        return ResponseEntity.ok(chat);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChatLog>> getUserChats(@PathVariable User user) {
        return ResponseEntity.ok(chatLogService.getUserChats(user));
    }

    @GetMapping("/upload/{uploadId}")
    public ResponseEntity<List<ChatLog>> getUploadChats(@PathVariable Upload upload) {
        return ResponseEntity.ok(chatLogService.getUploadChats(upload));
    }
}