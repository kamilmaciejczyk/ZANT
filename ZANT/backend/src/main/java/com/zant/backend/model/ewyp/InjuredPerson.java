package com.zant.backend.model.ewyp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class InjuredPerson {
    
    private String pesel;
    
    private String idDocumentType;
    
    private String idDocumentNumber;
    
    private String firstName;
    
    private String lastName;
    
    private String birthDate;
    
    private String birthPlace;
    
    private String phoneNumber;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "injured_address_street")),
        @AttributeOverride(name = "houseNumber", column = @Column(name = "injured_address_house_number")),
        @AttributeOverride(name = "apartmentNumber", column = @Column(name = "injured_address_apartment_number")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "injured_address_postal_code")),
        @AttributeOverride(name = "city", column = @Column(name = "injured_address_city")),
        @AttributeOverride(name = "country", column = @Column(name = "injured_address_country"))
    })
    private Address address;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "injured_polish_address_street")),
        @AttributeOverride(name = "houseNumber", column = @Column(name = "injured_polish_address_house_number")),
        @AttributeOverride(name = "apartmentNumber", column = @Column(name = "injured_polish_address_apartment_number")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "injured_polish_address_postal_code")),
        @AttributeOverride(name = "city", column = @Column(name = "injured_polish_address_city"))
    })
    private PolishAddress lastPolishAddressOrStay;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "mode", column = @Column(name = "injured_corresp_address_mode")),
        @AttributeOverride(name = "street", column = @Column(name = "injured_corresp_address_street")),
        @AttributeOverride(name = "houseNumber", column = @Column(name = "injured_corresp_address_house_number")),
        @AttributeOverride(name = "apartmentNumber", column = @Column(name = "injured_corresp_address_apartment_number")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "injured_corresp_address_postal_code")),
        @AttributeOverride(name = "city", column = @Column(name = "injured_corresp_address_city")),
        @AttributeOverride(name = "country", column = @Column(name = "injured_corresp_address_country"))
    })
    private CorrespondenceAddress correspondenceAddress;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "injured_business_address_street")),
        @AttributeOverride(name = "houseNumber", column = @Column(name = "injured_business_address_house_number")),
        @AttributeOverride(name = "apartmentNumber", column = @Column(name = "injured_business_address_apartment_number")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "injured_business_address_postal_code")),
        @AttributeOverride(name = "city", column = @Column(name = "injured_business_address_city")),
        @AttributeOverride(name = "phoneNumber", column = @Column(name = "injured_business_address_phone"))
    })
    private NonAgriculturalBusinessAddress nonAgriculturalBusinessAddress;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "injured_childcare_address_street")),
        @AttributeOverride(name = "houseNumber", column = @Column(name = "injured_childcare_address_house_number")),
        @AttributeOverride(name = "apartmentNumber", column = @Column(name = "injured_childcare_address_apartment_number")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "injured_childcare_address_postal_code")),
        @AttributeOverride(name = "city", column = @Column(name = "injured_childcare_address_city")),
        @AttributeOverride(name = "phoneNumber", column = @Column(name = "injured_childcare_address_phone"))
    })
    private ChildCareAddress childCareAddress;
}
