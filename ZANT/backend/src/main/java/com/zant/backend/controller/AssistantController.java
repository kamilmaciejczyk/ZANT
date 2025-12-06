package com.zant.backend.controller;

import com.zant.backend.model.AssistantTurn;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    @PostMapping("/{conversationId}/message")
    public AssistantTurn handleMessage(@PathVariable String conversationId, @RequestBody String userMessage) {
        // TODO: Implement assistant logic here
        return new AssistantTurn("Hello from Assistant!", null, null, 0.0);
    }
}
