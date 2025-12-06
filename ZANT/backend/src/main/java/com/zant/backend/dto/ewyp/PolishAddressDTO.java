package com.zant.backend.dto.ewyp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolishAddressDTO {
    private String street;
    private String houseNumber;
    private String apartmentNumber;
    private String postalCode;
    private String city;
}
