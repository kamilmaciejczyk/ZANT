package com.zant.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class AccidentReport {

    private PersonData victimData;
    private BusinessData businessData;
    private AccidentData accidentData;
    private List<Witness> witnesses;
    private PersonData attorneyData;
    private List<String> requiredDocuments;
}
