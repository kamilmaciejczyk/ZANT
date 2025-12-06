package com.zant.backend.dto.ewyp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccidentInfoDTO {
    private String accidentDate;
    private String accidentTime;
    private String plannedWorkStartTime;
    private String plannedWorkEndTime;
    private String placeOfAccident;
    private String injuriesDescription;
    private String circumstancesAndCauses;
    private Boolean firstAidGiven;
    private String firstAidFacility;
    private String investigatingAuthority;
    private Boolean accidentDuringMachineOperation;
    private String machineConditionDescription;
    private Boolean machineHasCertificate;
    private Boolean machineInFixedAssetsRegister;
}
