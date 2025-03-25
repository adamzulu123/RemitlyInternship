package com.remitly.main.RemitlyInternship.Service;

import com.remitly.main.RemitlyInternship.DTO.*;
import com.remitly.main.RemitlyInternship.Exception.InvalidSwiftCodeException;
import com.remitly.main.RemitlyInternship.Exception.SwiftCodeExistsException;
import com.remitly.main.RemitlyInternship.Exception.SwiftCodeNotFoundException;
import com.remitly.main.RemitlyInternship.Model.SwiftCode;
import com.remitly.main.RemitlyInternship.Repository.SwiftCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwiftCodeService {

    private final SwiftCodeRepository swiftCodeRepository;

    //Retrieving the data based on the swiftCode
    @Transactional(readOnly = true)
    public SwiftCodeDTO getSwiftCode(String swiftCode) {
        log.info("Retrieving swift code for " + swiftCode);
        SwiftCode swiftCodeEntity = swiftCodeRepository.findBySwiftCode(swiftCode)
                .orElseThrow(() -> new SwiftCodeNotFoundException("SWIFT code not found: " + swiftCode));

        //getting all branches if swiftCodeEntity is a headquarter
        return mapToDTO(swiftCodeEntity, swiftCodeEntity.isHeadquarter());
    }

    //Retrieving the data based on the countryISO2
    @Transactional(readOnly = true)
    public CountrySwiftCodesResponseDTO getCountrySwiftCodes(String countryISO2) {
        log.info("Retrieving country swift codes for " + countryISO2);
        String formatedCountryISO2 = countryISO2.toUpperCase();

        List<SwiftCode> swiftCodes = swiftCodeRepository.findByCountryISO2(formatedCountryISO2);
        if (swiftCodes.isEmpty()) throw new SwiftCodeNotFoundException("SWIFT codes not found for country: " + formatedCountryISO2);

        String countryName = swiftCodes.getFirst().getCountryName();
        //mapping all the swiftCodes from the country to our specific DTO
        List<SwiftCodeDTO> swiftCodeDTOs = swiftCodes.stream()
                .map(swiftCode -> mapToDTO(swiftCode, false))
                .toList();

        //returning DTO containing all information
        return CountrySwiftCodesResponseDTO.builder()
                .countryISO2(formatedCountryISO2)
                .countryName(countryName)
                .swiftCodes(swiftCodeDTOs)
                .build();
    }


    //creating new SwiftCode - we assume that is a new one so we don't add duplicates or change existing ones
    @Transactional
    public MessageResponseDTO createSwiftCode(SwiftCodeRequestDTO requestDTO) {
        log.info("Creating swift code for " + requestDTO.getSwiftCode());

        //retrieve all information from the request
        String address = requestDTO.getAddress();
        String bankName = requestDTO.getBankName();
        String countryISO2 = requestDTO.getCountryISO2();
        String countryName = requestDTO.getCountryName();
        boolean isHeadquarter = requestDTO.getIsHeadquarter();

        String swiftCode = requestDTO.getSwiftCode();


        if (swiftCodeRepository.existsBySwiftCode(swiftCode)) {
            throw new SwiftCodeExistsException("Swift code already exists: " + swiftCode);
        }

        SwiftCode swiftCodeEntity = SwiftCode.builder()
                .swiftCode(swiftCode)
                .bankName(bankName)
                .address(address)
                .countryISO2(countryISO2)
                .countryName(countryName)
                .isHeadquarter(isHeadquarter)
                .branches(new ArrayList<>())
                .build();

        //now if it's a branch we have to find its headquarter
        if(!isHeadquarter) {
            //XXX at the end from the description is always headquarter
            if (swiftCode.endsWith("XXX")) {
                throw new InvalidSwiftCodeException("Branch SwiftCode can't end with XXX: " + swiftCode);
            }
            String potentialHeadquarterSwiftCode = swiftCode.substring(0, 8) + "XXX";
            Optional<SwiftCode> headquarter = swiftCodeRepository.findBySwiftCode(potentialHeadquarterSwiftCode);
            //log.info("Found headquarter: {}", headquarter.isPresent());

            //if we find a headquarter we are setting our headquarters value and creating a relation between
            //our headquarter and branch. So branch know its headquarter and vice versa
            headquarter.ifPresent(h -> {
                swiftCodeEntity.setHeadquarters(h);
                h.getBranches().add(swiftCodeEntity);
                //swiftCodeRepository.save(h);
            });
        } else {
            //If user adds branch first which I guess might be possible
            List<SwiftCode> orphanBranches = swiftCodeRepository.findByHeadquarters(null).stream()
                    .filter(branch -> branch.getSwiftCode().startsWith(swiftCode.substring(0, 8)))
                    .toList();

            orphanBranches.forEach(branch -> {
                branch.setHeadquarters(swiftCodeEntity);
                swiftCodeEntity.getBranches().add(branch);
            });

            swiftCodeRepository.saveAll(orphanBranches);
        }

        swiftCodeRepository.save(swiftCodeEntity);
        log.info("Successfully created new SWIFT code: {}", swiftCode);

        return MessageResponseDTO.builder()
                .message("Successfully created new SWIFT code: " + swiftCode)
                .build();
    }


    //deleting existing SwiftCodes
    @Transactional
    public MessageResponseDTO deleteSwiftCode(String swiftCode) {
        log.info("Deleting swift code for " + swiftCode);

        SwiftCode swiftCodeEntity = swiftCodeRepository.findBySwiftCode(swiftCode)
                .orElseThrow(() -> new SwiftCodeNotFoundException("SWIFT code not found: " + swiftCode));

        //if it's a branch then we are removing it from the headquarter list and deleting branch itself
        if (!swiftCodeEntity.isHeadquarter()) {
            if (swiftCodeEntity.getHeadquarters() != null) {
                swiftCodeEntity.getHeadquarters().getBranches().remove(swiftCodeEntity);
            }
        }

        //IT'S NOT SPECIFIED WHAT SHOULD WE DO IN CASE IF WE ARE DELETING HEADQUARTER!!!!!!!!
        //I choose to delete all branches with: cascade = CascadeType.ALL in SwiftCode class.

        /*
        //1) if it's a headquarter we have to delete it and all its branches
        //2) deleting headquarter and all branches stays
        if (swiftCodeEntity.isHeadquarter() && !swiftCodeEntity.getBranches().isEmpty()) {
            swiftCodeEntity.getBranches().forEach(branch ->{
                branch.setHeadquarter(false);
                swiftCodeRepository.delete(branch);
            });
        }
         */

        swiftCodeRepository.delete(swiftCodeEntity);
        log.info("Successfully deleted SWIFT code: {}", swiftCode);

        return MessageResponseDTO.builder()
                .message("Successfully deleted SWIFT code: " + swiftCode)
                .build();
    }


    //mapping SwiftCode model (entity) to a DTO - used by both getSwiftCode and getCountrySwiftCodes
    private SwiftCodeDTO mapToDTO(SwiftCode swiftCode, boolean includeBranches) {
        SwiftCodeDTO swiftCodeDTO = SwiftCodeDTO.builder()
                .address(swiftCode.getAddress())
                .bankName(swiftCode.getBankName())
                .countryISO2(swiftCode.getCountryISO2())
                .countryName(swiftCode.getCountryName())
                .isHeadquarter(swiftCode.isHeadquarter())
                .swiftCode(swiftCode.getSwiftCode())
                .build();

        if(swiftCode.isHeadquarter() && includeBranches) {
            List<SwiftCodeBranchDTO> branchDTOs = getBranchDTOs(swiftCode);
            swiftCodeDTO.setBranches(branchDTOs);
        }
        return swiftCodeDTO;
    }

    //mapping the list of the branches
    private List<SwiftCodeBranchDTO> getBranchDTOs(SwiftCode swiftCode) {
        if (swiftCode.getBranches().isEmpty()) {
            return List.of();
        }

        //we are using mapBranchToDTO for every object in the stream
        return swiftCode.getBranches().stream()
                .map(this::mapBranchToDTO)
                .collect(Collectors.toList()); //and then collect every object to the list
    }

    private SwiftCodeBranchDTO mapBranchToDTO(SwiftCode branch) {
        return SwiftCodeBranchDTO.builder()
                .address(branch.getAddress())
                .bankName(branch.getBankName())
                .countryISO2(branch.getCountryISO2())
                .countryName(branch.getCountryName())
                .isHeadquarter(branch.isHeadquarter())
                .swiftCode(branch.getSwiftCode())
                .build();
    }
}
