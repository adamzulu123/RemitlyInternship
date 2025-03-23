package com.remitly.main.RemitlyInternship.Service;

import com.remitly.main.RemitlyInternship.DTO.*;
import com.remitly.main.RemitlyInternship.Exception.SwiftCodeExistsException;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    private SwiftCode branch2;
    private List<SwiftCode> countrySwiftCodes;
    private SwiftCodeRequestDTO requestDTO;

    @BeforeEach
    public void setUp() {
        headquarter = TestDataUtil.createSampleHeadquarter();
        branch = TestDataUtil.createSampleBranch();
        branch2 = TestDataUtil.createSampleBranch2();
        countrySwiftCodes = TestDataUtil.createCountrySwiftCodes();
        requestDTO = TestDataUtil.createSampleSwiftCodeRequestDTO();
    }


    //basic getting tests
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
    void testGetSwiftCodeHeadquarter_Success() {
        branch.setHeadquarters(headquarter);
        branch2.setHeadquarters(headquarter);
        headquarter.getBranches().add(branch);
        headquarter.getBranches().add(branch2);

        when(swiftCodeRepository.findBySwiftCode(headquarter.getSwiftCode())).thenReturn(Optional.of(headquarter));

        SwiftCodeDTO result = swiftCodeService.getSwiftCode(headquarter.getSwiftCode());

        assertNotNull(result);
        assertThat(result.getSwiftCode()).isEqualTo(headquarter.getSwiftCode());
        assertThat(result.isHeadquarter()).isTrue();

        //checking branches
        assertThat(result.getBranches().size()).isEqualTo(2);
        List<String> branchCodes = result.getBranches().stream()
                .map(SwiftCodeBranchDTO::getSwiftCode)
                .collect(Collectors.toList());

        assertThat(branchCodes).contains(branch.getSwiftCode());
        assertThat(branchCodes).contains(branch2.getSwiftCode());
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

    //getting swiftcodes by country
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


    //creating new SwiftCode
    @Test
    void testCreateSwiftCode_Success() {
        when(swiftCodeRepository.existsBySwiftCode(requestDTO.getSwiftCode())).thenReturn(false);
        //because our sample create requestDTO is a branch
        when(swiftCodeRepository.findBySwiftCode("ABCDPLPWXXX")).thenReturn(Optional.of(headquarter));
        //simulation of saving any SwiftCode to the database
        when(swiftCodeRepository.save(any(SwiftCode.class))).thenAnswer(i -> i.getArgument(0));

        MessageResponseDTO result = swiftCodeService.createSwiftCode(requestDTO);

        assertNotNull(result);
        assertThat(result.getMessage()).contains("Successfully created new SWIFT code: ");
        assertThat(result.getMessage()).contains(requestDTO.getSwiftCode());
        verify(swiftCodeRepository, times(1)).save(any(SwiftCode.class));
    }

    @Test
    void testCreateSwiftCode_AlreadyExists() {
        when(swiftCodeRepository.existsBySwiftCode(requestDTO.getSwiftCode())).thenReturn(true);

        assertThrows(SwiftCodeExistsException.class, () ->
            swiftCodeService.createSwiftCode(requestDTO)
        );
    }


    //delete tests
    @Test
    void testDeleteSwiftCode_NotFound() {
        when(swiftCodeRepository.findBySwiftCode("AADADADAXXX")).thenReturn(Optional.empty());

        assertThrows(SwiftCodeNotFoundException.class, () ->
                swiftCodeService.deleteSwiftCode("AADADADAXXX")
        );
        verify(swiftCodeRepository, never()).delete(any(SwiftCode.class));
    }

    @Test
    void testDeleteSwiftCode_Success() {
        when(swiftCodeRepository.findBySwiftCode("ABCDPLPW123")).thenReturn(Optional.of(branch));
        doNothing().when(swiftCodeRepository).delete(branch);

        MessageResponseDTO result = swiftCodeService.deleteSwiftCode("ABCDPLPW123");
        assertNotNull(result);
        assertThat(result.getMessage()).contains("Successfully deleted SWIFT code: ");
        assertThat(result.getMessage()).contains(branch.getSwiftCode());
        verify(swiftCodeRepository, times(1)).delete(branch);
    }

    @Test
    void testDeleteHeadquarterWithBranches(){
        when(swiftCodeRepository.findBySwiftCode("ABCDPLPWXXX")).thenReturn(Optional.of(headquarter));
        doNothing().when(swiftCodeRepository).delete(headquarter);

        MessageResponseDTO result = swiftCodeService.deleteSwiftCode("ABCDPLPWXXX");
        assertNotNull(result);
        assertThat(result.getMessage()).contains("Successfully deleted SWIFT code: ");
        assertThat(result.getMessage()).contains(headquarter.getSwiftCode());

        verify(swiftCodeRepository, times(1)).delete(headquarter);
    }






}
