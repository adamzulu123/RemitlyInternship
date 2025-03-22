package com.remitly.main.RemitlyInternship.Controller;

import com.remitly.main.RemitlyInternship.DTO.CountrySwiftCodesResponseDTO;
import com.remitly.main.RemitlyInternship.DTO.MessageResponseDTO;
import com.remitly.main.RemitlyInternship.DTO.SwiftCodeDTO;
import com.remitly.main.RemitlyInternship.DTO.SwiftCodeRequestDTO;
import com.remitly.main.RemitlyInternship.Service.SwiftCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/swift-codes")
public class SwiftCodeController {

    private final SwiftCodeService swiftCodeService;

    //Endpoint 1: Retrieve details of a single SWIFT code whether for a headquarters or branches.
    @GetMapping("/{swiftCode}")
    public ResponseEntity<SwiftCodeDTO> getSwiftCode(@PathVariable("swiftCode") String swiftCode) {
        log.info("GET request received for SWIFT CODE: {}", swiftCode);
        SwiftCodeDTO swiftCodeDTO = swiftCodeService.getSwiftCode(swiftCode);
        return ResponseEntity.ok(swiftCodeDTO);
    }

    @GetMapping("/country/{countryISO2code}")
    public ResponseEntity<CountrySwiftCodesResponseDTO> getSwiftCodesByContry(@PathVariable String countryISO2code){
        log.info("Get request received for SWIFT CODE COUNTRY: {}", countryISO2code);
        CountrySwiftCodesResponseDTO countrySwiftCodesResponseDTO = swiftCodeService.getCountrySwiftCodes(countryISO2code);
        return ResponseEntity.ok(countrySwiftCodesResponseDTO);
    }

    @PostMapping
    public ResponseEntity<MessageResponseDTO> createSwiftCode(@Valid @RequestBody SwiftCodeRequestDTO requestDTO) {
        log.info("POST request received for SWIFT CODE: {}", requestDTO.getSwiftCode());
        MessageResponseDTO messageResponseDTO = swiftCodeService.createSwiftCode(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(messageResponseDTO);
    }


}
