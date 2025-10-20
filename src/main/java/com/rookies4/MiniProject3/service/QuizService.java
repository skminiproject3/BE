package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.domain.enums.Difficulty;
import com.rookies4.MiniProject3.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;

    //Quiz 저장
    public Quiz saveQuiz(Content content, String question, String answer, String optionsJson, Difficulty difficulty) {
        Quiz quiz = Quiz.builder()
                .content(content)
                .question(question)
                .answer(answer)
                .options(optionsJson)
                .difficulty(difficulty)
                .build();
        return quizRepository.save(quiz);
    }


    public List<Quiz> getQuizzesByUpload(Content content) {
        return quizRepository.findByUpload(content);
    }
}