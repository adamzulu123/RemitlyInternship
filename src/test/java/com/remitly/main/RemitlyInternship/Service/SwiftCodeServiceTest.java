package com.remitly.main.RemitlyInternship.Service;

import com.remitly.main.RemitlyInternship.DTO.CountrySwiftCodesResponseDTO;
import com.remitly.main.RemitlyInternship.DTO.SwiftCodeDTO;
import com.remitly.main.RemitlyInternship.DTO.SwiftCodeRequestDTO;
import com.remitly.main.RemitlyInternship.Exception.SwiftCodeNotFoundException;
import com.remitly.main.RemitlyInternship.Model.SwiftCode;
import com.remitly.main.RemitlyInternship.Repository.SwiftCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SwiftCodeServiceTest {

    //Mock is an object which simulates / imitates the behaviour of the real object
    //helps us to test our application in isolation
    @Mock
    private SwiftCodeRepository swiftCodeRepository;
    @InjectMocks
    private SwiftCodeService swiftCodeService; //inject all mocks into it swiftcodeService instance

    private SwiftCode headquarter;
    private SwiftCode branch;
    private List<SwiftCode> countrySwiftCodes;
    private SwiftCodeRequestDTO requestDTO;

    @BeforeEach
    public void setUp() {
        headquarter = TestDataUtil.createSampleHeadquarter();
        branch = TestDataUtil.createSampleBranch();
        countrySwiftCodes = TestDataUtil.createCountrySwiftCodes();
        requestDTO = TestDataUtil.createSampleSwiftCodeRequestDTO();
    }

    @Test
    void testGetSwiftCode_Success() {
        String givenSwiftCode = headquarter.getSwiftCode();
        //simulation without database
        when(swiftCodeRepository.findBySwiftCode(givenSwiftCode)).thenReturn(Optional.of(headquarter));

        SwiftCodeDTO result = swiftCodeService.getSwiftCode(givenSwiftCode);

        assertNotNull(result);
        assertThat(result.getSwiftCode()).isEqualTo(givenSwiftCode);
        assertThat(result.getBankName()).isEqualTo(headquarter.getBankName());
        assertThat(result.getCountryISO2()).isEqualTo(headquarter.getCountryISO2());
    }

    @Test
    void testGetSwiftCodeBranch_Success() {
        String givenSwiftCode = branch.getSwiftCode();
        when(swiftCodeRepository.findBySwiftCode(givenSwiftCode)).thenReturn(Optional.of(branch));

        SwiftCodeDTO result = swiftCodeService.getSwiftCode(givenSwiftCode);
        assertNotNull(result);
        assertThat(result.getSwiftCode()).isEqualTo(givenSwiftCode);
        assertThat(result.getBankName()).isEqualTo(branch.getBankName());
        assertThat(result.getCountryISO2()).isEqualTo(branch.getCountryISO2());
    }

    @Test
    void getSwiftCode_NotFound() {
        when(swiftCodeRepository.findBySwiftCode("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(SwiftCodeNotFoundException.class, () ->
                swiftCodeService.getSwiftCode("NONEXISTENT")
        );
    }

    @Test
    void getSwiftCodeByCountry_Success() {
        String countryISO2 = "PL";
        when(swiftCodeRepository.findByCountryISO2(countryISO2)).thenReturn(countrySwiftCodes);

        CountrySwiftCodesResponseDTO result = swiftCodeService.getCountrySwiftCodes(countryISO2);

        assertNotNull(result);
        assertThat(result.getCountryISO2()).isEqualTo("PL");
        assertThat(result.getCountryName()).isEqualTo("POLAND");
        assertThat(result.getSwiftCodes()).hasSize(2);
    }

    @Test
    void getSwiftCodeByCountry_NotFound() {
        when(swiftCodeRepository.findByCountryISO2("XX")).thenReturn(List.of());

        assertThrows(SwiftCodeNotFoundException.class, () ->
                swiftCodeService.getCountrySwiftCodes("XX")
        );
    }

    //todo: continue creating unit tests and later integration tests




}
