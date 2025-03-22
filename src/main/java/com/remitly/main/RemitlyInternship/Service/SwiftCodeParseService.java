package com.remitly.main.RemitlyInternship.Service;

import com.remitly.main.RemitlyInternship.Model.SwiftCode;
import com.remitly.main.RemitlyInternship.Repository.SwiftCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwiftCodeParseService {

    private final SwiftCodeRepository swiftCodeRepository;

    @Transactional
    protected void parseExcelFile(InputStream inputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0); //downloading first sheet

            //cache for heaquarters, it's important to save headquarters first
            Map<String, SwiftCode> headquartersMap = new HashMap<>();
            List<SwiftCode> swiftCodesToSave = new ArrayList<>();

            //iterating through rows cuz every row is a new record which has to be added to the database
            for (Row row : sheet) {

                //skipping header row
                if(row.getRowNum() == 0){
                    continue;
                }

                String countryISO2 = row.getCell(0).getStringCellValue().toUpperCase();
                String swiftCode = row.getCell(1).getStringCellValue();

                Optional<SwiftCode> existingSwiftCode = swiftCodeRepository.findBySwiftCode(swiftCode);
                if(existingSwiftCode.isPresent()){
                    log.warn("This swift code already exists in DB");
                    continue;
                }

                boolean isHeadquarter = swiftCode.endsWith("XXX");
                String bankName = row.getCell(3).getStringCellValue().toUpperCase();
                String address = row.getCell(4).getStringCellValue();
                String countryName = row.getCell(6).getStringCellValue().toUpperCase();

                SwiftCode swift = SwiftCode.builder()
                        .swiftCode(swiftCode)
                        .bankName(bankName)
                        .address(address)
                        .countryISO2(countryISO2)
                        .countryName(countryName)
                        .isHeadquarter(isHeadquarter)
                        .headquarters(null)
                        .branches(new ArrayList<>())
                        .build();

                if (isValid(swift)) {
                    swiftCodesToSave.add(swift);
                } else {
                    log.warn("Skipping invalid SWIFT code: {}", swift.getSwiftCode());
                }

                if (isHeadquarter) {
                    headquartersMap.put(swiftCode, swift);
                }
            }

            for (SwiftCode branch : swiftCodesToSave) {
                if (!branch.isHeadquarter()) {
                    String headquarterCode = branch.getSwiftCode().substring(0, 8) + "XXX";
                    SwiftCode headquarter = headquartersMap.get(headquarterCode);

                    if (headquarter != null) {
                        branch.setHeadquarters(headquarter);
                        headquarter.getBranches().add(branch);
                    }
                }
            }

            swiftCodeRepository.saveAll(swiftCodesToSave);

            log.info("Successfully parsed and saved {} total SWIFT codes", swiftCodesToSave.size());

        }catch(Exception e){
            throw new RuntimeException("Error parsing Excel file" + e.getMessage());
        }

    }

    //in our case excel file is good and this validation is not needed but in real life not always it's that awesome
    private boolean isValid(SwiftCode swiftCode){
        if(swiftCode.getSwiftCode().length() != 11){
            log.warn("Invalid SWIFT code length for {}", swiftCode.getSwiftCode());
            return false;
        }
        if(swiftCode.getBankName() == null || swiftCode.getBankName().isEmpty()){
            log.warn("Invalid bank name for {}", swiftCode.getBankName());
            return false;
        }
        if (swiftCode.getCountryISO2() == null || swiftCode.getCountryISO2().length() != 2) {
            log.warn("Invalid country code for {}", swiftCode.getSwiftCode());
            return false;
        }
        return true;

    }
}
