package com.zant.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class AccidentData {
    private LocalDateTime accidentDateTime;
    private String place;
    private String plannedWorkHours;
    private String activitiesBefore;
    private String circumstancesAndCauses;
    private String injuries;
    private String medicalHelp;
    private String machinesInfo;
    private String bhpInfo;
}
