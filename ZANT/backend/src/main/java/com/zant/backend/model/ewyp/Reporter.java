package com.zant.backend.model.ewyp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Reporter {
    
    private Boolean isDifferentFromInjuredPerson;
    
    private String pesel;
    
    private String idDocumentType;
    
    private String idDocumentNumber;
    
    private String firstName;
    
    private String lastName;
    
    private String birthDate;
    
    private String phoneNumber;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "reporter_address_street")),
        @AttributeOverride(name = "houseNumber", column = @Column(name = "reporter_address_house_number")),
        @AttributeOverride(name = "apartmentNumber", column = @Column(name = "reporter_address_apartment_number")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "reporter_address_postal_code")),
        @AttributeOverride(name = "city", column = @Column(name = "reporter_address_city")),
        @AttributeOverride(name = "country", column = @Column(name = "reporter_address_country"))
    })
    private Address address;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "reporter_polish_address_street")),
        @AttributeOverride(name = "houseNumber", column = @Column(name = "reporter_polish_address_house_number")),
        @AttributeOverride(name = "apartmentNumber", column = @Column(name = "reporter_polish_address_apartment_number")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "reporter_polish_address_postal_code")),
        @AttributeOverride(name = "city", column = @Column(name = "reporter_polish_address_city"))
    })
    private PolishAddress lastPolishAddressOrStay;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "mode", column = @Column(name = "reporter_corresp_address_mode")),
        @AttributeOverride(name = "street", column = @Column(name = "reporter_corresp_address_street")),
        @AttributeOverride(name = "houseNumber", column = @Column(name = "reporter_corresp_address_house_number")),
        @AttributeOverride(name = "apartmentNumber", column = @Column(name = "reporter_corresp_address_apartment_number")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "reporter_corresp_address_postal_code")),
        @AttributeOverride(name = "city", column = @Column(name = "reporter_corresp_address_city")),
        @AttributeOverride(name = "country", column = @Column(name = "reporter_corresp_address_country"))
    })
    private CorrespondenceAddress correspondenceAddress;
}
