package com.zant.backend.dto.ewyp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InjuredPersonDTO {
    private String pesel;
    private String idDocumentType;
    private String idDocumentNumber;
    private String firstName;
    private String lastName;
    private String birthDate;
    private String birthPlace;
    private String phoneNumber;
    private AddressDTO address;
    private PolishAddressDTO lastPolishAddressOrStay;
    private CorrespondenceAddressDTO correspondenceAddress;
    private NonAgriculturalBusinessAddressDTO nonAgriculturalBusinessAddress;
    private ChildCareAddressDTO childCareAddress;
}
