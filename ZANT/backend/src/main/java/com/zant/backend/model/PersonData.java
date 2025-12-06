package com.zant.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PersonData {
    private String firstName;
    private String lastName;
    private String pesel;
    private String address;
    private String seriesAndNumberId;
}
