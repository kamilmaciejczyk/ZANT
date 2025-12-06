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
    private String hospitalCardCopyFilename;
    
    private Boolean hasProsecutorDecisionCopy;
    private String prosecutorDecisionCopyFilename;
    
    private Boolean hasPowerOfAttorneyCopy;
    private String powerOfAttorneyCopyFilename;
    
    private Boolean hasDeathDocsCopy;
    private String deathDocsCopyFilename;
    
    private Boolean hasOtherDocuments;
    private List<OtherDocumentDTO> otherDocuments;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherDocumentDTO {
        private String documentName;
        private String filename;
    }
}
