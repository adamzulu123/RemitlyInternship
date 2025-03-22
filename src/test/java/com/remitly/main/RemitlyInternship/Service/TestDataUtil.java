package com.remitly.main.RemitlyInternship.Service;

import com.remitly.main.RemitlyInternship.DTO.SwiftCodeRequestDTO;
import com.remitly.main.RemitlyInternship.Model.SwiftCode;

import java.util.ArrayList;
import java.util.List;

public final class TestDataUtil {

    public static SwiftCode createSampleHeadquarter() {
        // Pusta lista na oddziały
        return SwiftCode.builder()
                .id(1L)
                .swiftCode("ABCDPLPWXXX")
                .bankName("TEST BANK")
                .address("Main Street 1")
                .countryISO2("PL")
                .countryName("POLAND")
                .isHeadquarter(true)
                .branches(new ArrayList<>()) // Pusta lista na oddziały
                .build();
    }

    public static SwiftCode createSampleBranch() {
        SwiftCode headquarter = createSampleHeadquarter();

        SwiftCode branch = SwiftCode.builder()
                .id(2L)
                .swiftCode("ABCDPLPW123")
                .bankName("TEST BANK")
                .address("Branch Street 1")
                .countryISO2("PL")
                .countryName("POLAND")
                .isHeadquarter(false)
                .headquarters(headquarter)
                .build();

        headquarter.getBranches().add(branch); // Dodanie oddziału do siedziby głównej
        return branch;
    }

    public static List<SwiftCode> createCountrySwiftCodes() {
        SwiftCode headquarter = createSampleHeadquarter();
        SwiftCode branch = createSampleBranch();
        return List.of(headquarter, branch);
    }

    public static SwiftCodeRequestDTO createSampleSwiftCodeRequestDTO() {
        return SwiftCodeRequestDTO.builder()
                .swiftCode("ABCDPLPW456")
                .bankName("TEST BANK")
                .address("New Branch Street 1")
                .countryISO2("PL")
                .countryName("POLAND")
                .isHeadquarter(false)
                .build();
    }
}






