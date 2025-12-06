package com.zant.backend.service;

import com.zant.backend.config.RequiredField;
import com.zant.backend.model.AccidentReport;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class MissingFieldsCalculator {

    private final List<RequiredField> requiredFields = Arrays.asList(
            new RequiredField("victimData.firstName", "PERSON_DATA", "Imię poszkodowanego", true, "Podaj imię poszkodowanego"),
            new RequiredField("victimData.lastName", "PERSON_DATA", "Nazwisko poszkodowanego", true, "Podaj nazwisko poszkodowanego"),
            new RequiredField("victimData.pesel", "PERSON_DATA", "PESEL poszkodowanego", true, "Podaj PESEL poszkodowanego"),
            new RequiredField("victimData.address", "PERSON_DATA", "Adres poszkodowanego", true, "Podaj adres poszkodowanego"),
            new RequiredField("businessData.nip", "BUSINESS_DATA", "NIP działalności", true, "Podaj NIP działalności gospodarczej"),
            new RequiredField("businessData.regon", "BUSINESS_DATA", "REGON działalności", false, "Podaj REGON działalności gospodarczej"),
            new RequiredField("accidentData.accidentDateTime", "ACCIDENT_DATA", "Data i godzina wypadku", true, "Podaj datę i godzinę wypadku"),
            new RequiredField("accidentData.place", "ACCIDENT_DATA", "Miejsce wypadku", true, "Podaj miejsce wypadku"),
            new RequiredField("accidentData.plannedWorkHours", "ACCIDENT_DATA", "Planowane godziny pracy", true, "Podaj planowane godziny pracy"),
            new RequiredField("accidentData.activitiesBefore", "ACCIDENT_DATA", "Czynności wykonywane przed wypadkiem", true, "Opisz czynności wykonywane przed wypadkiem"),
            new RequiredField("accidentData.circumstancesAndCauses", "ACCIDENT_DATA", "Okoliczności i przyczyny wypadku", true, "Opisz okoliczności i przyczyny wypadku"),
            new RequiredField("accidentData.injuries", "ACCIDENT_DATA", "Urazy", true, "Opisz doznane urazy"),
            new RequiredField("accidentData.medicalHelp", "ACCIDENT_DATA", "Udzielona pomoc medyczna", false, "Opisz udzieloną pomoc medyczną"),
            new RequiredField("witnesses", "WITNESSES", "Świadkowie", false, "Podaj dane świadków (imię, nazwisko, adres)"),
            new RequiredField("attorneyData.firstName", "ATTORNEY_DATA", "Imię pełnomocnika", false, "Podaj imię pełnomocnika"),
            new RequiredField("attorneyData.lastName", "ATTORNEY_DATA", "Nazwisko pełnomocnika", false, "Podaj nazwisko pełnomocnika"),
            new RequiredField("attorneyData.address", "ATTORNEY_DATA", "Adres pełnomocnika", false, "Podaj adres pełnomocnika"),
            new RequiredField("requiredDocuments", "DOCUMENTS", "Wymagane dokumenty", true, "Lista wymaganych dokumentów")
    );

    public List<RequiredField> getRequiredFields() {
        return requiredFields;
    }

    public List<String> calculateMissingFields(AccidentReport report) {
        List<String> missing = new ArrayList<>();
        for (RequiredField field : requiredFields) {
            if (field.isMandatory() && isFieldMissing(report, field.getCode())) {
                missing.add(field.getLabel());
            }
        }
        return missing;
    }

    private boolean isFieldMissing(AccidentReport report, String fieldCode) {
        // This is a simplified check. A more robust solution would use reflection or a mapping.
        // For now, we'll check some top-level fields and assume nested objects are populated if the parent is not null.
        switch (fieldCode) {
            case "victimData.firstName": return report.getVictimData() == null || report.getVictimData().getFirstName() == null || report.getVictimData().getFirstName().isEmpty();
            case "victimData.lastName": return report.getVictimData() == null || report.getVictimData().getLastName() == null || report.getVictimData().getLastName().isEmpty();
            case "victimData.pesel": return report.getVictimData() == null || report.getVictimData().getPesel() == null || report.getVictimData().getPesel().isEmpty();
            case "victimData.address": return report.getVictimData() == null || report.getVictimData().getAddress() == null || report.getVictimData().getAddress().isEmpty();
            case "businessData.nip": return report.getBusinessData() == null || report.getBusinessData().getNip() == null || report.getBusinessData().getNip().isEmpty();
            case "businessData.regon": return report.getBusinessData() == null || report.getBusinessData().getRegon() == null || report.getBusinessData().getRegon().isEmpty();
            case "accidentData.accidentDateTime": return report.getAccidentData() == null || report.getAccidentData().getAccidentDateTime() == null;
            case "accidentData.place": return report.getAccidentData() == null || report.getAccidentData().getPlace() == null || report.getAccidentData().getPlace().isEmpty();
            case "accidentData.plannedWorkHours": return report.getAccidentData() == null || report.getAccidentData().getPlannedWorkHours() == null || report.getAccidentData().getPlannedWorkHours().isEmpty();
            case "accidentData.activitiesBefore": return report.getAccidentData() == null || report.getAccidentData().getActivitiesBefore() == null || report.getAccidentData().getActivitiesBefore().isEmpty();
            case "accidentData.circumstancesAndCauses": return report.getAccidentData() == null || report.getAccidentData().getCircumstancesAndCauses() == null || report.getAccidentData().getCircumstancesAndCauses().isEmpty();
            case "accidentData.injuries": return report.getAccidentData() == null || report.getAccidentData().getInjuries() == null || report.getAccidentData().getInjuries().isEmpty();
            case "accidentData.medicalHelp": return report.getAccidentData() == null || report.getAccidentData().getMedicalHelp() == null || report.getAccidentData().getMedicalHelp().isEmpty();
            case "witnesses": return report.getWitnesses() == null || report.getWitnesses().isEmpty();
            case "attorneyData.firstName": return report.getAttorneyData() == null || report.getAttorneyData().getFirstName() == null || report.getAttorneyData().getFirstName().isEmpty();
            case "attorneyData.lastName": return report.getAttorneyData() == null || report.getAttorneyData().getLastName() == null || report.getAttorneyData().getLastName().isEmpty();
            case "attorneyData.address": return report.getAttorneyData() == null || report.getAttorneyData().getAddress() == null || report.getAttorneyData().getAddress().isEmpty();
            case "requiredDocuments": return report.getRequiredDocuments() == null || report.getRequiredDocuments().isEmpty();
            default: return true; // Unknown field, consider it missing
        }
    }
}
