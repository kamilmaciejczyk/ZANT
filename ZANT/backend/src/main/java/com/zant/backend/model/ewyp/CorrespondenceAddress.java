package com.zant.backend.model.ewyp;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class CorrespondenceAddress {
    
    private String mode; // STANDARD_ADDRESS | POSTE_RESTANTE | PO_BOX | PIGEONHOLE
    
    private String street;
    
    private String houseNumber;
    
    private String apartmentNumber;
    
    private String postalCode;
    
    private String city;
    
    private String country;
}
