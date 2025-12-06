package com.zant.backend.mapper;

import com.zant.backend.dto.ewyp.*;
import com.zant.backend.model.ewyp.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EWYPReportMapper {
    
    public EWYPReport toEntity(EWYPReportDTO dto) {
        if (dto == null) return null;
        
        EWYPReport entity = new EWYPReport();
        entity.setInjuredPerson(toInjuredPersonEntity(dto.getInjuredPerson()));
        entity.setReporter(toReporterEntity(dto.getReporter()));
        entity.setAccidentInfo(toAccidentInfoEntity(dto.getAccidentInfo()));
        entity.setWitnesses(toWitnessListEntity(dto.getWitnesses()));
        entity.setAttachments(toAttachmentsEntity(dto.getAttachments()));
        entity.setDocumentsToDeliverLater(toDocumentsToDeliverLaterEntity(dto.getDocumentsToDeliverLater()));
        entity.setResponseDeliveryMethod(dto.getResponseDeliveryMethod());
        entity.setSignature(toSignatureEntity(dto.getSignature()));
        
        return entity;
    }
    
    public EWYPReportDTO toDTO(EWYPReport entity) {
        if (entity == null) return null;
        
        EWYPReportDTO dto = new EWYPReportDTO();
        dto.setId(entity.getId());
        dto.setInjuredPerson(toInjuredPersonDTO(entity.getInjuredPerson()));
        dto.setReporter(toReporterDTO(entity.getReporter()));
        dto.setAccidentInfo(toAccidentInfoDTO(entity.getAccidentInfo()));
        dto.setWitnesses(toWitnessListDTO(entity.getWitnesses()));
        dto.setAttachments(toAttachmentsDTO(entity.getAttachments()));
        dto.setDocumentsToDeliverLater(toDocumentsToDeliverLaterDTO(entity.getDocumentsToDeliverLater()));
        dto.setResponseDeliveryMethod(entity.getResponseDeliveryMethod());
        dto.setSignature(toSignatureDTO(entity.getSignature()));
        
        return dto;
    }
    
    private InjuredPerson toInjuredPersonEntity(InjuredPersonDTO dto) {
        if (dto == null) return null;
        
        InjuredPerson entity = new InjuredPerson();
        entity.setPesel(dto.getPesel());
        entity.setIdDocumentType(dto.getIdDocumentType());
        entity.setIdDocumentNumber(dto.getIdDocumentNumber());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setBirthDate(dto.getBirthDate());
        entity.setBirthPlace(dto.getBirthPlace());
        entity.setPhoneNumber(dto.getPhoneNumber());
        entity.setAddress(toAddressEntity(dto.getAddress()));
        entity.setLastPolishAddressOrStay(toPolishAddressEntity(dto.getLastPolishAddressOrStay()));
        entity.setCorrespondenceAddress(toCorrespondenceAddressEntity(dto.getCorrespondenceAddress()));
        entity.setNonAgriculturalBusinessAddress(toNonAgriculturalBusinessAddressEntity(dto.getNonAgriculturalBusinessAddress()));
        entity.setChildCareAddress(toChildCareAddressEntity(dto.getChildCareAddress()));
        
        return entity;
    }
    
    private InjuredPersonDTO toInjuredPersonDTO(InjuredPerson entity) {
        if (entity == null) return null;
        
        InjuredPersonDTO dto = new InjuredPersonDTO();
        dto.setPesel(entity.getPesel());
        dto.setIdDocumentType(entity.getIdDocumentType());
        dto.setIdDocumentNumber(entity.getIdDocumentNumber());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setBirthDate(entity.getBirthDate());
        dto.setBirthPlace(entity.getBirthPlace());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setAddress(toAddressDTO(entity.getAddress()));
        dto.setLastPolishAddressOrStay(toPolishAddressDTO(entity.getLastPolishAddressOrStay()));
        dto.setCorrespondenceAddress(toCorrespondenceAddressDTO(entity.getCorrespondenceAddress()));
        dto.setNonAgriculturalBusinessAddress(toNonAgriculturalBusinessAddressDTO(entity.getNonAgriculturalBusinessAddress()));
        dto.setChildCareAddress(toChildCareAddressDTO(entity.getChildCareAddress()));
        
        return dto;
    }
    
    private Reporter toReporterEntity(ReporterDTO dto) {
        if (dto == null) return null;
        
        Reporter entity = new Reporter();
        entity.setIsDifferentFromInjuredPerson(dto.getIsDifferentFromInjuredPerson());
        entity.setPesel(dto.getPesel());
        entity.setIdDocumentType(dto.getIdDocumentType());
        entity.setIdDocumentNumber(dto.getIdDocumentNumber());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setBirthDate(dto.getBirthDate());
        entity.setPhoneNumber(dto.getPhoneNumber());
        entity.setAddress(toAddressEntity(dto.getAddress()));
        entity.setLastPolishAddressOrStay(toPolishAddressEntity(dto.getLastPolishAddressOrStay()));
        entity.setCorrespondenceAddress(toCorrespondenceAddressEntity(dto.getCorrespondenceAddress()));
        
        return entity;
    }
    
    private ReporterDTO toReporterDTO(Reporter entity) {
        if (entity == null) return null;
        
        ReporterDTO dto = new ReporterDTO();
        dto.setIsDifferentFromInjuredPerson(entity.getIsDifferentFromInjuredPerson());
        dto.setPesel(entity.getPesel());
        dto.setIdDocumentType(entity.getIdDocumentType());
        dto.setIdDocumentNumber(entity.getIdDocumentNumber());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setBirthDate(entity.getBirthDate());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setAddress(toAddressDTO(entity.getAddress()));
        dto.setLastPolishAddressOrStay(toPolishAddressDTO(entity.getLastPolishAddressOrStay()));
        dto.setCorrespondenceAddress(toCorrespondenceAddressDTO(entity.getCorrespondenceAddress()));
        
        return dto;
    }
    
    private Address toAddressEntity(AddressDTO dto) {
        if (dto == null) return null;
        return new Address(dto.getStreet(), dto.getHouseNumber(), dto.getApartmentNumber(),
                dto.getPostalCode(), dto.getCity(), dto.getCountry());
    }
    
    private AddressDTO toAddressDTO(Address entity) {
        if (entity == null) return null;
        return new AddressDTO(entity.getStreet(), entity.getHouseNumber(), entity.getApartmentNumber(),
                entity.getPostalCode(), entity.getCity(), entity.getCountry());
    }
    
    private PolishAddress toPolishAddressEntity(PolishAddressDTO dto) {
        if (dto == null) return null;
        return new PolishAddress(dto.getStreet(), dto.getHouseNumber(), dto.getApartmentNumber(),
                dto.getPostalCode(), dto.getCity());
    }
    
    private PolishAddressDTO toPolishAddressDTO(PolishAddress entity) {
        if (entity == null) return null;
        return new PolishAddressDTO(entity.getStreet(), entity.getHouseNumber(), entity.getApartmentNumber(),
                entity.getPostalCode(), entity.getCity());
    }
    
    private CorrespondenceAddress toCorrespondenceAddressEntity(CorrespondenceAddressDTO dto) {
        if (dto == null) return null;
        return new CorrespondenceAddress(dto.getMode(), dto.getStreet(), dto.getHouseNumber(),
                dto.getApartmentNumber(), dto.getPostalCode(), dto.getCity(), dto.getCountry());
    }
    
    private CorrespondenceAddressDTO toCorrespondenceAddressDTO(CorrespondenceAddress entity) {
        if (entity == null) return null;
        return new CorrespondenceAddressDTO(entity.getMode(), entity.getStreet(), entity.getHouseNumber(),
                entity.getApartmentNumber(), entity.getPostalCode(), entity.getCity(), entity.getCountry());
    }
    
    private NonAgriculturalBusinessAddress toNonAgriculturalBusinessAddressEntity(NonAgriculturalBusinessAddressDTO dto) {
        if (dto == null) return null;
        return new NonAgriculturalBusinessAddress(dto.getStreet(), dto.getHouseNumber(),
                dto.getApartmentNumber(), dto.getPostalCode(), dto.getCity(), dto.getPhoneNumber());
    }
    
    private NonAgriculturalBusinessAddressDTO toNonAgriculturalBusinessAddressDTO(NonAgriculturalBusinessAddress entity) {
        if (entity == null) return null;
        return new NonAgriculturalBusinessAddressDTO(entity.getStreet(), entity.getHouseNumber(),
                entity.getApartmentNumber(), entity.getPostalCode(), entity.getCity(), entity.getPhoneNumber());
    }
    
    private ChildCareAddress toChildCareAddressEntity(ChildCareAddressDTO dto) {
        if (dto == null) return null;
        return new ChildCareAddress(dto.getStreet(), dto.getHouseNumber(), dto.getApartmentNumber(),
                dto.getPostalCode(), dto.getCity(), dto.getPhoneNumber());
    }
    
    private ChildCareAddressDTO toChildCareAddressDTO(ChildCareAddress entity) {
        if (entity == null) return null;
        return new ChildCareAddressDTO(entity.getStreet(), entity.getHouseNumber(), entity.getApartmentNumber(),
                entity.getPostalCode(), entity.getCity(), entity.getPhoneNumber());
    }
    
    private AccidentInfo toAccidentInfoEntity(AccidentInfoDTO dto) {
        if (dto == null) return null;
        
        AccidentInfo entity = new AccidentInfo();
        entity.setAccidentDate(dto.getAccidentDate());
        entity.setAccidentTime(dto.getAccidentTime());
        entity.setPlannedWorkStartTime(dto.getPlannedWorkStartTime());
        entity.setPlannedWorkEndTime(dto.getPlannedWorkEndTime());
        entity.setPlaceOfAccident(dto.getPlaceOfAccident());
        entity.setInjuriesDescription(dto.getInjuriesDescription());
        entity.setCircumstancesAndCauses(dto.getCircumstancesAndCauses());
        entity.setFirstAidGiven(dto.getFirstAidGiven());
        entity.setFirstAidFacility(dto.getFirstAidFacility());
        entity.setInvestigatingAuthority(dto.getInvestigatingAuthority());
        entity.setAccidentDuringMachineOperation(dto.getAccidentDuringMachineOperation());
        entity.setMachineConditionDescription(dto.getMachineConditionDescription());
        entity.setMachineHasCertificate(dto.getMachineHasCertificate());
        entity.setMachineInFixedAssetsRegister(dto.getMachineInFixedAssetsRegister());
        
        return entity;
    }
    
    private AccidentInfoDTO toAccidentInfoDTO(AccidentInfo entity) {
        if (entity == null) return null;
        
        AccidentInfoDTO dto = new AccidentInfoDTO();
        dto.setAccidentDate(entity.getAccidentDate());
        dto.setAccidentTime(entity.getAccidentTime());
        dto.setPlannedWorkStartTime(entity.getPlannedWorkStartTime());
        dto.setPlannedWorkEndTime(entity.getPlannedWorkEndTime());
        dto.setPlaceOfAccident(entity.getPlaceOfAccident());
        dto.setInjuriesDescription(entity.getInjuriesDescription());
        dto.setCircumstancesAndCauses(entity.getCircumstancesAndCauses());
        dto.setFirstAidGiven(entity.getFirstAidGiven());
        dto.setFirstAidFacility(entity.getFirstAidFacility());
        dto.setInvestigatingAuthority(entity.getInvestigatingAuthority());
        dto.setAccidentDuringMachineOperation(entity.getAccidentDuringMachineOperation());
        dto.setMachineConditionDescription(entity.getMachineConditionDescription());
        dto.setMachineHasCertificate(entity.getMachineHasCertificate());
        dto.setMachineInFixedAssetsRegister(entity.getMachineInFixedAssetsRegister());
        
        return dto;
    }
    
    private List<WitnessInfo> toWitnessListEntity(List<WitnessInfoDTO> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream().map(this::toWitnessEntity).collect(Collectors.toList());
    }
    
    private List<WitnessInfoDTO> toWitnessListDTO(List<WitnessInfo> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream().map(this::toWitnessDTO).collect(Collectors.toList());
    }
    
    private WitnessInfo toWitnessEntity(WitnessInfoDTO dto) {
        if (dto == null) return null;
        
        WitnessInfo entity = new WitnessInfo();
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setStreet(dto.getStreet());
        entity.setHouseNumber(dto.getHouseNumber());
        entity.setApartmentNumber(dto.getApartmentNumber());
        entity.setPostalCode(dto.getPostalCode());
        entity.setCity(dto.getCity());
        entity.setCountry(dto.getCountry());
        
        return entity;
    }
    
    private WitnessInfoDTO toWitnessDTO(WitnessInfo entity) {
        if (entity == null) return null;
        
        WitnessInfoDTO dto = new WitnessInfoDTO();
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setStreet(entity.getStreet());
        dto.setHouseNumber(entity.getHouseNumber());
        dto.setApartmentNumber(entity.getApartmentNumber());
        dto.setPostalCode(entity.getPostalCode());
        dto.setCity(entity.getCity());
        dto.setCountry(entity.getCountry());
        
        return dto;
    }
    
    private Attachments toAttachmentsEntity(AttachmentsDTO dto) {
        if (dto == null) return null;
        
        Attachments entity = new Attachments();
        entity.setHasHospitalCardCopy(dto.getHasHospitalCardCopy());
        entity.setHospitalCardCopyFilename(dto.getHospitalCardCopyFilename());
        entity.setHasProsecutorDecisionCopy(dto.getHasProsecutorDecisionCopy());
        entity.setProsecutorDecisionCopyFilename(dto.getProsecutorDecisionCopyFilename());
        entity.setHasDeathDocsCopy(dto.getHasDeathDocsCopy());
        entity.setDeathDocsCopyFilename(dto.getDeathDocsCopyFilename());
        entity.setHasOtherDocuments(dto.getHasOtherDocuments());
        
        // Map OtherDocumentDTO list to OtherDocument list
        if (dto.getOtherDocuments() != null) {
            List<Attachments.OtherDocument> otherDocuments = dto.getOtherDocuments().stream()
                .map(dtoDoc -> new Attachments.OtherDocument(dtoDoc.getDocumentName(), dtoDoc.getFilename()))
                .collect(Collectors.toList());
            entity.setOtherDocuments(otherDocuments);
        }
        
        return entity;
    }
    
    private AttachmentsDTO toAttachmentsDTO(Attachments entity) {
        if (entity == null) return null;
        
        AttachmentsDTO dto = new AttachmentsDTO();
        dto.setHasHospitalCardCopy(entity.getHasHospitalCardCopy());
        dto.setHospitalCardCopyFilename(entity.getHospitalCardCopyFilename());
        dto.setHasProsecutorDecisionCopy(entity.getHasProsecutorDecisionCopy());
        dto.setProsecutorDecisionCopyFilename(entity.getProsecutorDecisionCopyFilename());
        dto.setHasDeathDocsCopy(entity.getHasDeathDocsCopy());
        dto.setDeathDocsCopyFilename(entity.getDeathDocsCopyFilename());
        dto.setHasOtherDocuments(entity.getHasOtherDocuments());
        
        // Map OtherDocument list to OtherDocumentDTO list
        if (entity.getOtherDocuments() != null) {
            List<AttachmentsDTO.OtherDocumentDTO> otherDocuments = entity.getOtherDocuments().stream()
                .map(entityDoc -> new AttachmentsDTO.OtherDocumentDTO(entityDoc.getDocumentName(), entityDoc.getFilename()))
                .collect(Collectors.toList());
            dto.setOtherDocuments(otherDocuments);
        }
        
        return dto;
    }
    
    private DocumentsToDeliverLater toDocumentsToDeliverLaterEntity(DocumentsToDeliverLaterDTO dto) {
        if (dto == null) return null;
        return new DocumentsToDeliverLater(dto.getToDate(), dto.getDocuments());
    }
    
    private DocumentsToDeliverLaterDTO toDocumentsToDeliverLaterDTO(DocumentsToDeliverLater entity) {
        if (entity == null) return null;
        return new DocumentsToDeliverLaterDTO(entity.getToDate(), entity.getDocuments());
    }
    
    private Signature toSignatureEntity(SignatureDTO dto) {
        if (dto == null) return null;
        return new Signature(dto.getDeclarationDate(), dto.getSignatureName());
    }
    
    private SignatureDTO toSignatureDTO(Signature entity) {
        if (entity == null) return null;
        return new SignatureDTO(entity.getDeclarationDate(), entity.getSignatureName());
    }
}
