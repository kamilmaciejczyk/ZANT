package com.zant.backend.ai;

import com.zant.backend.config.RequiredField;
import com.zant.backend.model.AccidentReport;
import com.zant.backend.model.AssistantState;
import com.zant.backend.model.PersonData;
import com.zant.backend.model.BusinessData;
import com.zant.backend.model.AccidentData;
import com.zant.backend.model.Witness;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiClient {

    public AiResponse extractInfoFromUserMessage(AssistantState state, String userMessage, List<RequiredField> requiredFields) {
        // TODO: Integrate with Gemini API here. For now, return mock data.
        Map<String, Object> extractedFields = new HashMap<>();
        String summaryForUser = "Dziękuję za wiadomość. Na razie nie wyodrębniono żadnych danych.";
        List<String> followUpQuestions = Collections.singletonList("Jakie jest Twoje imię i nazwisko?");

        // Mock logic to simulate data extraction
        if (userMessage.toLowerCase().contains("jan kowalski")) {
            PersonData victimData = new PersonData();
            victimData.setFirstName("Jan");
            victimData.setLastName("Kowalski");
            extractedFields.put("victimData", victimData);
            summaryForUser = "Wyodrębniono imię i nazwisko poszkodowanego: Jan Kowalski.";
            followUpQuestions = Collections.singletonList("Jaki jest Twój PESEL i adres?");
        } else if (userMessage.toLowerCase().contains("pesel 12345678901")) {
            PersonData victimData = (PersonData) extractedFields.getOrDefault("victimData", new PersonData());
            victimData.setPesel("12345678901");
            extractedFields.put("victimData", victimData);
            summaryForUser = "Wyodrębniono PESEL poszkodowanego.";
            followUpQuestions = Collections.singletonList("Jaki jest Twój adres?");
        } else if (userMessage.toLowerCase().contains("nip 123-456-78-90")) {
            BusinessData businessData = new BusinessData();
            businessData.setNip("123-456-78-90");
            extractedFields.put("businessData", businessData);
            summaryForUser = "Wyodrębniono NIP działalności.";
            followUpQuestions = Collections.singletonList("Jaki jest REGON działalności?");
        } else if (userMessage.toLowerCase().contains("wypadek 2025-12-06 10:00")) {
            AccidentData accidentData = new AccidentData();
            accidentData.setAccidentDateTime(LocalDateTime.of(2025, 12, 6, 10, 0));
            extractedFields.put("accidentData", accidentData);
            summaryForUser = "Wyodrębniono datę i godzinę wypadku.";
            followUpQuestions = Collections.singletonList("Gdzie miał miejsce wypadek?");
        }


        return new AiResponse(extractedFields, summaryForUser, followUpQuestions);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiResponse {
        private Map<String, Object> extractedFields;
        private String summaryForUser;
        private List<String> followUpQuestions;
    }
}
