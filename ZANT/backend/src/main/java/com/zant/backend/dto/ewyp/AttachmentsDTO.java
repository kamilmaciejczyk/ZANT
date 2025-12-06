package com.zant.backend.dto.ewyp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentsDTO {
    private Boolean hasHospitalCardCopy;
    private Boolean hasProsecutorDecisionCopy;
    private Boolean hasDeathDocsCopy;
    private Boolean hasOtherDocuments;
    private List<String> otherDocuments;
}
