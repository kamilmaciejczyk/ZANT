package com.zant.backend.controller;

import com.zant.backend.ai.AiClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-config")
@RequiredArgsConstructor
@Slf4j
public class AiConfigController {

    private final AiClient aiClient;

    @GetMapping("/provider")
    public ResponseEntity<AiProviderResponse> getCurrentProvider() {
        String currentProvider = aiClient.getAiProvider();
        log.info("Current AI provider: {}", currentProvider);
        return ResponseEntity.ok(new AiProviderResponse(currentProvider));
    }

    @PostMapping("/provider")
    public ResponseEntity<AiProviderResponse> setProvider(@RequestBody AiProviderRequest request) {
        log.info("Changing AI provider to: {}", request.getProvider());
        aiClient.setAiProvider(request.getProvider());
        String newProvider = aiClient.getAiProvider();
        return ResponseEntity.ok(new AiProviderResponse(newProvider));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiProviderRequest {
        private String provider;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiProviderResponse {
        private String provider;
    }
}
