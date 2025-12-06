package com.zant.backend.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequiredField {
    private String code;
    private String section;
    private String label;
    private boolean mandatory;
    private String description;
}
