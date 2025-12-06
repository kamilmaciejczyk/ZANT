package com.zant.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zant.backend.ai.AiClient;
import com.zant.backend.ai.AiClient.AiResponse;
import com.zant.backend.model.AccidentData;
import com.zant.backend.model.AccidentReport;
import com.zant.backend.model.AssistantState;
import com.zant.backend.model.AssistantTurn;
import com.zant.backend.model.BusinessData;
import com.zant.backend.model.PersonData;
import com.zant.backend.repository.AssistantStateRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssistantService {

    private final MissingFieldsCalculator missingFieldsCalculator;
    private final AiClient aiClient;
    private final AssistantStateRepository assistantStateRepository;
    private final ObjectMapper objectMapper;

    public AssistantService(MissingFieldsCalculator missingFieldsCalculator, AiClient aiClient, AssistantStateRepository assistantStateRepository, ObjectMapper objectMapper) {
        this.missingFieldsCalculator = missingFieldsCalculator;
        this.aiClient = aiClient;
        this.assistantStateRepository = assistantStateRepository;
        this.objectMapper = objectMapper;
    }

    public AssistantTurn handleMessage(String conversationId, String userMessage) {
        // 1. Pobierz stan rozmowy z bazy danych lub utwórz nowy
        AssistantState state = assistantStateRepository.findById(conversationId)
                .orElseGet(() -> {
                    AssistantState newState = new AssistantState();
                    newState.setConversationId(conversationId);
                    newState.setAccidentReport(new AccidentReport());
                    newState.setConversationHistory(new ArrayList<>());
                    newState.setMissingFields(new ArrayList<>());
                    newState.setCompletionProgress(0.0);
                    return newState;
                });

        // 2. Dodaj wiadomość użytkownika do historii rozmowy
        Map<String, String> userMessageEntry = new HashMap<>();
        userMessageEntry.put("role", "user");
        userMessageEntry.put("content", userMessage);
        state.getConversationHistory().add(userMessageEntry);

        // 3. Wywołaj AiClient.extractInfoFromUserMessage(...)
        AiResponse aiResponse = aiClient.extractInfoFromUserMessage(state, userMessage, missingFieldsCalculator.getRequiredFields());

        // 4. Zaktualizuj sloty w AssistantState
        if (aiResponse.getExtractedFields().containsKey("victimData")) {
            Object victimDataObj = aiResponse.getExtractedFields().get("victimData");
            PersonData victimData = objectMapper.convertValue(victimDataObj, PersonData.class);
            state.getAccidentReport().setVictimData(victimData);
        }
        if (aiResponse.getExtractedFields().containsKey("businessData")) {
            Object businessDataObj = aiResponse.getExtractedFields().get("businessData");
            BusinessData businessData = objectMapper.convertValue(businessDataObj, BusinessData.class);
            state.getAccidentReport().setBusinessData(businessData);
        }
        if (aiResponse.getExtractedFields().containsKey("accidentData")) {
            Object accidentDataObj = aiResponse.getExtractedFields().get("accidentData");
            AccidentData accidentData = objectMapper.convertValue(accidentDataObj, AccidentData.class);
            state.getAccidentReport().setAccidentData(accidentData);
        }

        // 5. Oblicz brakujące pola
        List<String> missingFields = missingFieldsCalculator.calculateMissingFields(state.getAccidentReport());
        state.setMissingFields(missingFields);
        state.setCompletionProgress((double) (missingFieldsCalculator.getRequiredFields().size() - missingFields.size()) / missingFieldsCalculator.getRequiredFields().size() * 100);

        // 6. Dodaj odpowiedź asystenta do historii rozmowy
        Map<String, String> assistantMessageEntry = new HashMap<>();
        assistantMessageEntry.put("role", "assistant");
        assistantMessageEntry.put("content", aiResponse.getSummaryForUser());
        state.getConversationHistory().add(assistantMessageEntry);

        // 7. Zapisz zaktualizowany stan do bazy danych
        assistantStateRepository.save(state);

        // 8. Zwróć AssistantTurn
        return new AssistantTurn(
                aiResponse.getSummaryForUser(),
                aiResponse.getFollowUpQuestions(),
                missingFields,
                state.getCompletionProgress()
        );
    }
}
