package com.remitly.main.RemitlyInternship.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.remitly.main.RemitlyInternship.DTO.CountrySwiftCodesResponseDTO;
import com.remitly.main.RemitlyInternship.DTO.MessageResponseDTO;
import com.remitly.main.RemitlyInternship.DTO.SwiftCodeBranchDTO;
import com.remitly.main.RemitlyInternship.DTO.SwiftCodeDTO;
import com.remitly.main.RemitlyInternship.DTO.SwiftCodeRequestDTO;
import com.remitly.main.RemitlyInternship.Exception.SwiftCodeExistsException;
import com.remitly.main.RemitlyInternship.Exception.SwiftCodeNotFoundException;
import com.remitly.main.RemitlyInternship.Service.SwiftCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SwiftCodeController.class)
public class SwiftCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SwiftCodeService swiftCodeService;

    @Autowired
    private ObjectMapper objectMapper;

    private SwiftCodeDTO headquarters;
    private SwiftCodeDTO branch;
    private SwiftCodeBranchDTO branchDTO;
    private CountrySwiftCodesResponseDTO countryResponse;
    private SwiftCodeRequestDTO createRequest;
    private MessageResponseDTO messageResponse;
    private MessageResponseDTO deleteResponse;

    @BeforeEach
    void setUp() {
        //Setup test data - we can do it like previously with TestDataUtil, but I decided this version is simpler
        headquarters = SwiftCodeDTO.builder()
                .swiftCode("TESTBANKXXX")
                .bankName("Remitly Bank USA")
                .address("123 Banking St, Seattle, WA")
                .countryISO2("US")
                .countryName("United States")
                .isHeadquarter(true)
                .build();

        branchDTO = SwiftCodeBranchDTO.builder()
                .swiftCode("TESTBANK001")
                .bankName("Remitly Bank USA")
                .address("456 Finance Ave, New York, NY")
                .countryISO2("US")
                .countryName("United States")
                .isHeadquarter(false)
                .build();

        List<SwiftCodeBranchDTO> branches = List.of(branchDTO);
        headquarters.setBranches(branches);

        branch = SwiftCodeDTO.builder()
                .swiftCode("TESTBANK001")
                .bankName("Remitly Bank USA")
                .address("456 Finance Ave, New York, NY")
                .countryISO2("US")
                .countryName("United States")
                .isHeadquarter(false)
                .build();

        List<SwiftCodeDTO> swiftCodes = Arrays.asList(headquarters, branch);
        countryResponse = CountrySwiftCodesResponseDTO.builder()
                .countryISO2("US")
                .countryName("United States")
                .swiftCodes(swiftCodes)
                .build();

        createRequest = SwiftCodeRequestDTO.builder()
                .swiftCode("TESTJDJDXXX")
                .bankName("Remitly Bank USA West")
                .address("789 Money St, San Francisco, CA")
                .countryISO2("US")
                .countryName("United States")
                .isHeadquarter(true)
                .build();

        messageResponse = MessageResponseDTO.builder()
                .message("Successfully created new SWIFT code: TESTJDJDXXX")
                .build();

        deleteResponse = MessageResponseDTO.builder()
                .message("Successfully deleted SWIFT code: TESTJDJDXXX")
                .build();
    }

    @Test
    void getSwiftCode_ValidHeadquarter_ReturnsSwiftCodeWithBranches() throws Exception {
        when(swiftCodeService.getSwiftCode("TESTBANKXXX")).thenReturn(headquarters);

        mockMvc.perform(get("/v1/swift-codes/TESTBANKXXX"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.swiftCode").value(headquarters.getSwiftCode()))
                .andExpect(jsonPath("$.bankName").value(headquarters.getBankName()))
                .andExpect(jsonPath("$.isHeadquarter").value(headquarters.isHeadquarter()))
                .andExpect(jsonPath("$.branches[0].swiftCode").value(branch.getSwiftCode()));
    }

    @Test
    void getSwiftCode_InvalidCode_Returns404() throws Exception {
        // Given
        when(swiftCodeService.getSwiftCode("INVALID00XXX"))
                .thenThrow(new SwiftCodeNotFoundException("SWIFT code not found: INVALID00XXX"));

        // When & Then
        mockMvc.perform(get("/v1/swift-codes/INVALID00XXX"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSwiftCodeByCountry_ValidCountry_ReturnsSwiftCodeWithBranches() throws Exception {
        when(swiftCodeService.getCountrySwiftCodes("US")).thenReturn(countryResponse);

        mockMvc.perform(get("/v1/swift-codes/country/US"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.countryISO2").value("US"))
                .andExpect(jsonPath("$.countryName").value(countryResponse.getCountryName()))
                .andExpect(jsonPath("$.swiftCodes[0].swiftCode").value(headquarters.getSwiftCode()))
                .andExpect(jsonPath("$.swiftCodes[1].swiftCode").value(branch.getSwiftCode()));
    }

    @Test
    void getSwiftCodeByCountry_InvalidCountry_Returns404() throws Exception {
        when(swiftCodeService.getCountrySwiftCodes("XX")).thenThrow(new SwiftCodeNotFoundException("SWIFT code not found: XX"));

        mockMvc.perform(get("/v1/swift-codes/country/XX"))
                .andExpect(status().isNotFound());

    }

    @Test
    void createSwiftCode_ValidRequest_Returns201() throws Exception {
        when(swiftCodeService.createSwiftCode(any(SwiftCodeRequestDTO.class))).thenReturn(messageResponse);

        mockMvc.perform(post("/v1/swift-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message")
                        .value("Successfully created new SWIFT code: TESTJDJDXXX"));
    }

    @Test
    void createSwiftCode_InvalidRequest_Returns409() throws Exception {
        when(swiftCodeService.createSwiftCode(any(SwiftCodeRequestDTO.class)))
                .thenThrow(new SwiftCodeExistsException("SwiftCode already exists: TESTJDJDXXX"));

        mockMvc.perform(post("/v1/swift-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteSwiftCode_ValidRequest_ReturnsOk() throws Exception {
        when(swiftCodeService.deleteSwiftCode("TESTJDJDXXX")).thenReturn(messageResponse);

        mockMvc.perform(delete("/v1/swift-codes/TESTJDJDXXX"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(messageResponse.getMessage()));
    }

    @Test
    void deleteSwiftCode_InvalidRequest_Returns404() throws Exception {
        when(swiftCodeService.deleteSwiftCode("INVALIDXXXX")).thenThrow(new SwiftCodeNotFoundException("INVALIDXXXX"));

        mockMvc.perform(delete("/v1/swift-codes/INVALIDXXXX"))
                .andExpect(status().isNotFound());
    }


}



















