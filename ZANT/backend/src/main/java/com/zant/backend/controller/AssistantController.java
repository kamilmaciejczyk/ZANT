package com.zant.backend.controller;

import com.zant.backend.ai.AiClient;
import com.zant.backend.model.AssistantTurn;
import com.zant.backend.service.AssistantService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;
    private final AiClient aiClient;

    public AssistantController(AssistantService assistantService, AiClient aiClient) {
        this.assistantService = assistantService;
        this.aiClient = aiClient;
    }

    @PostMapping("/{conversationId}/message")
    public AssistantTurn handleMessage(@PathVariable String conversationId, @RequestBody String userMessage) {
        return assistantService.handleMessage(conversationId, userMessage);
    }

    @PostMapping("/circumstances")
    public AiClient.CircumstancesAssistantResponse generateCircumstancesQuestions(@RequestBody Map<String, String> request) {
        String accidentDescription = request.get("accidentDescription");
        if (accidentDescription == null || accidentDescription.trim().isEmpty()) {
            // Return empty response if no description provided
            return new AiClient.CircumstancesAssistantResponse(0, java.util.Collections.emptyList(), "Brak opisu zdarzenia!");
        }
        return aiClient.generateCircumstancesQuestions(accidentDescription);
    }
}
