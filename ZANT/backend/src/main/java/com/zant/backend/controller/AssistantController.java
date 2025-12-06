package com.zant.backend.controller;

import com.zant.backend.model.AssistantTurn;
import com.zant.backend.service.AssistantService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/{conversationId}/message")
    public AssistantTurn handleMessage(@PathVariable String conversationId, @RequestBody String userMessage) {
        return assistantService.handleMessage(conversationId, userMessage);
    }
}
