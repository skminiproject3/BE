package com.rookies4.MiniProject3.controller;


import com.rookies4.MiniProject3.domain.entity.ChatLog;
import com.rookies4.MiniProject3.domain.entity.Content;
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
                                            @RequestParam Content content,
                                            @RequestParam Role role,
                                            @RequestParam String message,
                                            @RequestParam(required = false) Float responseTime,
                                            @RequestParam(required = false) Integer tokenUsage) {
        ChatLog chat = chatLogService.saveChat(user, content, role, message, responseTime, tokenUsage);
        return ResponseEntity.ok(chat);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChatLog>> getUserChats(@PathVariable User user) {
        return ResponseEntity.ok(chatLogService.getUserChats(user));
    }

    @GetMapping("/upload/{uploadId}")
    public ResponseEntity<List<ChatLog>> getUploadChats(@PathVariable Content content) {
        return ResponseEntity.ok(chatLogService.getUploadChats(content));
    }
}