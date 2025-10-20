package com.rookies4.MiniProject3.controller;

import com.rookies4.MiniProject3.domain.entity.Log;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @PostMapping("/create")
    public ResponseEntity<Log> createLog(@RequestParam User user,
                                         @RequestParam String action,
                                         @RequestParam(required = false) Long targetId,
                                         @RequestParam(required = false) String description) {
        Log log = logService.saveLog(user, action, targetId, description);
        return ResponseEntity.ok(log);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Log>> getUserLogs(@PathVariable User user) {
        return ResponseEntity.ok(logService.getUserLogs(user));
    }
}