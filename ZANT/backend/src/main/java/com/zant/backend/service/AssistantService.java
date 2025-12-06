package com.zant.backend.service;

import com.zant.backend.ai.AiClient;
import com.zant.backend.ai.AiClient.AiResponse;
import com.zant.backend.model.AccidentReport;
import com.zant.backend.model.AssistantState;
import com.zant.backend.model.AssistantTurn;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AssistantService {

    private final MissingFieldsCalculator missingFieldsCalculator;
    private final AiClient aiClient;

    public AssistantService(MissingFieldsCalculator missingFieldsCalculator, AiClient aiClient) {
        this.missingFieldsCalculator = missingFieldsCalculator;
        this.aiClient = aiClient;
    }

    public AssistantTurn handleMessage(String conversationId, String userMessage) {
        // TODO: Implement assistant logic here
        // 1. pobierz stan rozmowy (mock na razie)
        AssistantState state = new AssistantState();
        state.setConversationId(conversationId);
        state.setAccidentReport(new AccidentReport()); // Initialize an empty report for now

        // 2. wywołaj AiClient.extractInfoFromUserMessage(...)
        AiResponse aiResponse = aiClient.extractInfoFromUserMessage(state, userMessage, missingFieldsCalculator.getRequiredFields());

        // 3. zaktualizuj sloty w AssistantState
        // For now, we'll just update the victimData if it was extracted
        if (aiResponse.getExtractedFields().containsKey("victimData")) {
            state.getAccidentReport().setVictimData((com.zant.backend.model.PersonData) aiResponse.getExtractedFields().get("victimData"));
        }
        if (aiResponse.getExtractedFields().containsKey("businessData")) {
            state.getAccidentReport().setBusinessData((com.zant.backend.model.BusinessData) aiResponse.getExtractedFields().get("businessData"));
        }
        if (aiResponse.getExtractedFields().containsKey("accidentData")) {
            state.getAccidentReport().setAccidentData((com.zant.backend.model.AccidentData) aiResponse.getExtractedFields().get("accidentData"));
        }


        // 4. oblicz brakujące pola
        List<String> missingFields = missingFieldsCalculator.calculateMissingFields(state.getAccidentReport());
        state.setMissingFields(missingFields);
        state.setCompletionProgress((double) (missingFieldsCalculator.getRequiredFields().size() - missingFields.size()) / missingFieldsCalculator.getRequiredFields().size() * 100);

        // 5. wygeneruj follow-up perguntas
        // 6. zwróć AssistantTurn.
        return new AssistantTurn(
                aiResponse.getSummaryForUser(),
                aiResponse.getFollowUpQuestions(),
                missingFields,
                state.getCompletionProgress()
        );
    }
}
