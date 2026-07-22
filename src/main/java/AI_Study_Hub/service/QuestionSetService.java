package AI_Study_Hub.service;

import AI_Study_Hub.dto.response.QuestionSetResponseDTO;
import AI_Study_Hub.entity.Account;
import AI_Study_Hub.entity.Question; // NHỚ IMPORT QUESTION
import AI_Study_Hub.entity.StudyMaterial;
import AI_Study_Hub.repository.AccountRepository;
import AI_Study_Hub.repository.StudyMaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionSetService {

    private final StudyMaterialRepository studyMaterialRepository;
    private final AccountRepository accountRepository;

    private final AI_Study_Hub.repository.QuestionRepository questionRepository;
    private final AI_Study_Hub.repository.QuestionOptionRepository optionRepository;

    // Hàm 1: Cũ của bạn (Giữ nguyên)
    public List<QuestionSetResponseDTO> getMyQuestionSets(String username) {
        Account account = accountRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        List<StudyMaterial> materials = studyMaterialRepository.findByAccount_AccountIdOrderByCreatedAtDesc(account.getAccountId());

        return materials.stream().map(material -> {
            int totalQuestions = 0;
            if (material.getMaterialContext() != null && material.getMaterialContext().getQuestions() != null) {
                totalQuestions = material.getMaterialContext().getQuestions().size();
            }

            return QuestionSetResponseDTO.builder()
                    .id(material.getMaterialId())
                    .title(material.getTitle())
                    .subject(material.getSubject() != null ? material.getSubject().getSubjectName() : "General")
                    .totalQuestions(totalQuestions)
                    .downloads(0)
                    .status("Active")
                    .build();
        }).collect(Collectors.toList());
    }

    // THÊM HÀM 2: Lấy toàn bộ câu hỏi (Tận dụng luôn Service này)
    public List<QuestionSetResponseDTO.QuestionDetailDTO> getAllQuestions(String username) {
        Account account = accountRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        List<StudyMaterial> materials = studyMaterialRepository.findByAccount_AccountIdOrderByCreatedAtDesc(account.getAccountId());
        List<QuestionSetResponseDTO.QuestionDetailDTO> allQuestions = new ArrayList<>();

        for (StudyMaterial material : materials) {
            if (material.getMaterialContext() != null && material.getMaterialContext().getQuestions() != null) {
                for (Question q : material.getMaterialContext().getQuestions()) {
                    allQuestions.add(QuestionSetResponseDTO.QuestionDetailDTO.builder()
                            .questionId(q.getQuestionId())
                            .questionText(q.getQuestionText())
                            .documentId(material.getMaterialId())
                            .documentTitle(material.getTitle())
                            .difficulty("Medium")
                            .build());
                }
            }
        }
        return allQuestions;
    }

    @org.springframework.transaction.annotation.Transactional
    public QuestionSetResponseDTO.QuestionDetailDTO createManualQuestion(Map<String, Object> payload) {
        String content = (String) payload.get("content");
        List<Map<String, Object>> options = (List<Map<String, Object>>) payload.get("options");

        AI_Study_Hub.entity.Question question = AI_Study_Hub.entity.Question.builder()
                .questionText(content)
                .build();
        question = questionRepository.save(question);

        String correctText = "";
        for (Map<String, Object> opt : options) {
            String text = (String) opt.get("text");
            boolean isCorrect = (boolean) opt.get("isCorrect");

            AI_Study_Hub.entity.QuestionOption option = AI_Study_Hub.entity.QuestionOption.builder()
                    .question(question)
                    .optionText(text)
                    .isCorrect(isCorrect)
                    .build();
            optionRepository.save(option);

            if (isCorrect) correctText = text;
        }

        question.setCorrectAnswer(correctText);
        questionRepository.save(question);

        return QuestionSetResponseDTO.QuestionDetailDTO.builder()
                .questionId(question.getQuestionId())
                .questionText(question.getQuestionText())
                .documentTitle("Tự luận / Bổ sung")
                .difficulty((String) payload.get("difficulty"))
                .build();
    }
}