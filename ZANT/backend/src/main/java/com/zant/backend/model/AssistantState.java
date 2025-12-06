package com.zant.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class AssistantState {

    private String conversationId;
    private AccidentReport accidentReport;
    private List<Map<String, String>> conversationHistory;
    private List<String> missingFields;
    private double completionProgress;
}
