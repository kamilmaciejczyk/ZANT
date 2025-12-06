package com.zant.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PdfRequest {

    private String type;
    private AccidentReport data;
}
