package com.zant.backend.model.ewyp;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class AccidentInfo {
    
    private String accidentDate;
    
    private String accidentTime;
    
    private String plannedWorkStartTime;
    
    private String plannedWorkEndTime;
    
    @Column(columnDefinition = "TEXT")
    private String placeOfAccident;
    
    @Column(columnDefinition = "TEXT")
    private String injuriesDescription;
    
    @Column(columnDefinition = "TEXT")
    private String circumstancesAndCauses;
    
    private Boolean firstAidGiven;
    
    private String firstAidFacility;
    
    private String investigatingAuthority;
    
    private Boolean accidentDuringMachineOperation;
    
    @Column(columnDefinition = "TEXT")
    private String machineConditionDescription;
    
    private Boolean machineHasCertificate;
    
    private Boolean machineInFixedAssetsRegister;
}
