package com.rookies4.MiniProject3.service;

import com.rookies4.MiniProject3.domain.entity.Content;
import com.rookies4.MiniProject3.domain.entity.Quiz;
import com.rookies4.MiniProject3.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;

    /**
     * 퀴즈 저장 (Difficulty 없음)
     */
    public Quiz saveQuiz(Content content, String question, String correctAnswer, String optionsJson, String explanation) {
        Quiz quiz = Quiz.builder()
                .content(content)
                .question(question)
                .options(optionsJson)
                .correctAnswer(correctAnswer)
                .explanation(explanation)
                .build();

        return quizRepository.save(quiz);
    }

    /**
     * 콘텐츠별 퀴즈 조회
     */
    public List<Quiz> getQuizzesByContent(Content content) {
        return quizRepository.findByContent(content);
    }

    /**
     * 전체 퀴즈 조회 (관리용)
     */
    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }
}
