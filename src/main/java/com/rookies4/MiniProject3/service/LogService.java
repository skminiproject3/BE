package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Log;
import com.rookies4.MiniProject3.domain.entity.User;
import com.rookies4.MiniProject3.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;

    public Log saveLog(User user, String action, Long targetId, String description) {
        Log log = Log.builder()
                .user(user)
                .action(action)
                .targetId(targetId)
                .description(description)
                .build();
        return logRepository.save(log);
    }

    public List<Log> getUserLogs(User user) {
        return logRepository.findByUser(user);
    }
}