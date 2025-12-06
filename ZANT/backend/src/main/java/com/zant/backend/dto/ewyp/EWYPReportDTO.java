package com.zant.backend.dto.ewyp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EWYPReportDTO {
    private Long id;
    private InjuredPersonDTO injuredPerson;
    private ReporterDTO reporter;
    private AccidentInfoDTO accidentInfo;
    private List<WitnessInfoDTO> witnesses;
    private AttachmentsDTO attachments;
    private DocumentsToDeliverLaterDTO documentsToDeliverLater;
    private String responseDeliveryMethod;
    private SignatureDTO signature;
}
