package com.remitly.main.RemitlyInternship.Controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.remitly.main.RemitlyInternship.DTO.SwiftCodeRequestDTO;
import com.remitly.main.RemitlyInternship.Model.SwiftCode;
import com.remitly.main.RemitlyInternship.Repository.SwiftCodeRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.HashSet;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SwiftCodeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SwiftCodeRepository swiftCodeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private SwiftCode headquarters;
    private SwiftCode branch;
    private SwiftCodeRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        // Clear the database
        swiftCodeRepository.deleteAll();

        // Create test data directly in the database
        headquarters = SwiftCode.builder()
                .swiftCode("REMITLYXXX")
                .bankName("REMITLY BANK USA")
                .address("123 BANKING ST, SEATTLE, WA")
                .countryISO2("US")
                .countryName("UNITED STATES")
                .isHeadquarter(true)
                .branches(new ArrayList<>())
                .build();

        branch = SwiftCode.builder()
                .swiftCode("REMITLY001")
                .bankName("REMITLY BANK USA")
                .address("456 FINANCE AVE, NEW YORK, NY")
                .countryISO2("US")
                .countryName("UNITED STATES")
                .isHeadquarter(false)
                .build();

        //Set up the relationship
        branch.setHeadquarters(headquarters);
        headquarters.getBranches().add(branch);

        headquarters = swiftCodeRepository.save(headquarters);
        branch = swiftCodeRepository.save(branch);

        createRequest = SwiftCodeRequestDTO.builder()
                .swiftCode("NEWBANKKXXX")
                .bankName("NEW TEST BANK")
                .address("789 MONEY ST, SAN FRANCISCO, CA")
                .countryISO2("US")
                .countryName("UNITED STATES")
                .isHeadquarter(true)
                .build();
    }


    @Test
    void getSwiftCode_ExistingHeadquarters_ReturnsSwiftCodeWithBranches() throws Exception {
        mockMvc.perform(get("/v1/swift-codes/REMITLYXXX"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.swiftCode").value("REMITLYXXX"))
                .andExpect(jsonPath("$.bankName").value("REMITLY BANK USA"))
                .andExpect(jsonPath("$.isHeadquarter").value(true))
                .andExpect(jsonPath("$.branches", hasSize(1)))
                .andExpect(jsonPath("$.branches[0].swiftCode").value("REMITLY001"));
    }

    @Test
    void getSwiftCode_NonExistingCode_Returns404() throws Exception {
        mockMvc.perform(get("/v1/swift-codes/NONEXIST"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSwiftCodesByCountry_ExistingCountry_ReturnsAllSwiftCodesForCountry() throws Exception {
        mockMvc.perform(get("/v1/swift-codes/country/US"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.countryISO2").value("US"))
                .andExpect(jsonPath("$.countryName").value("UNITED STATES"))
                .andExpect(jsonPath("$.swiftCodes", hasSize(2)))
                .andExpect(jsonPath("$.swiftCodes[*].swiftCode", containsInAnyOrder("REMITLYXXX", "REMITLY001")));
    }

    @Test
    void getSwiftCodesByCountry_NonExistingCountry_Returns404() throws Exception {
        mockMvc.perform(get("/v1/swift-codes/country/XX"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createSwiftCode_ValidNewCode_Success() throws Exception {
        mockMvc.perform(post("/v1/swift-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("Successfully created new SWIFT code: NEWBANKKXXX")));

        //verifying if it was actually saved to the database
        assertTrue(swiftCodeRepository.findBySwiftCode("NEWBANKKXXX").isPresent());
    }

    @Test
    void createSwiftCode_InvalidRequest_Returns409Conflict() throws Exception {
        mockMvc.perform(post("/v1/swift-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/swift-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict());
    }

    //test for some requestDTO validation
    @Test
    void createSwiftCode_InvalidCountryAndSwiftCode_Returns400BadRequest() throws Exception {
        SwiftCodeRequestDTO test = SwiftCodeRequestDTO.builder()
                .swiftCode("SHORT") //less then 11
                .bankName("TEST BANK")
                .address("123 TEST ST")
                .countryISO2("USSS") //invalid country ISO2
                //missing country field also
                .isHeadquarter(true)
                .build();

        mockMvc.perform(post("/v1/swift-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(test)))
                .andExpect(status().isBadRequest());

    }

    @Test
    void deleteSwiftCode_ExistingCode_ReturnsSuccessMessage() throws Exception {
        mockMvc.perform(delete("/v1/swift-codes/REMITLYXXX"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("Successfully deleted SWIFT code: REMITLYXXX")));

        // Verify it was actually removed from the database
        assertFalse(swiftCodeRepository.findBySwiftCode("REMITLYXXX").isPresent());
    }

    @Test
    void deleteSwiftCode_NonExistingCode_Returns404() throws Exception {
        mockMvc.perform(delete("/v1/swift-codes/NONEXIST"))
                .andExpect(status().isNotFound());
    }






}
