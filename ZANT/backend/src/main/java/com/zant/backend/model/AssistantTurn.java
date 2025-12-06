package com.zant.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssistantTurn {

    private String response;
    private List<String> followUpQuestions;
    private List<String> missingFields;
    private double completionProgress;
}
