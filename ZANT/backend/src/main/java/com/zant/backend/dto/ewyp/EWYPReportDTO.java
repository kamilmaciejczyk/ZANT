package com.zant.backend.dto.ewyp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EWYPReportDTO {
    private UUID id;
    private InjuredPersonDTO injuredPerson;
    private ReporterDTO reporter;
    private AccidentInfoDTO accidentInfo;
    private List<WitnessInfoDTO> witnesses;
    private AttachmentsDTO attachments;
    private DocumentsToDeliverLaterDTO documentsToDeliverLater;
    private String responseDeliveryMethod;
    private SignatureDTO signature;
    private String status;
}
