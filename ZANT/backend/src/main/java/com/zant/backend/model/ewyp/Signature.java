package com.zant.backend.model.ewyp;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Signature {
    
    private String declarationDate;
    
    private String signatureName;
}
