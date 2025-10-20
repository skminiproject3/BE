package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.domain.entity.Upload;
import com.rookies4.MiniProject3.domain.enums.Difficulty;
import com.rookies4.MiniProject3.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;

    public Quiz saveQuiz(Upload upload, String question, String answer, String optionsJson, Difficulty difficulty) {
        Quiz quiz = Quiz.builder()
                .upload(upload)
                .question(question)
                .answer(answer)
                .options(optionsJson)
                .difficulty(difficulty)
                .build();
        return quizRepository.save(quiz);
    }

    public List<Quiz> getQuizzesByUpload(Upload upload) {
        return quizRepository.findByUpload(upload);
    }
}