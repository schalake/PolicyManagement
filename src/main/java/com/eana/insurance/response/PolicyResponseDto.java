package com.eana.insurance.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PolicyResponseDto {
    private String policyNumber;
    private String firstName;
    private String lastName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String policyType;
}
