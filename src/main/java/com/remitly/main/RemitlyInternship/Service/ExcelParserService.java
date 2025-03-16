package com.remitly.main.RemitlyInternship.Service;

import com.remitly.main.RemitlyInternship.Model.SwiftCode;
import com.remitly.main.RemitlyInternship.Repository.SwiftCodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
Important info about formatting: Country codes and names must always be stored and returned as uppercase strings.

 Important info about Excel file:
 * Codes ending with “XXX” represent a bank's headquarters, otherwise branch.
 * Branch codes are associated with a headquarters if their first 8 characters match.
 * Codes can represent both the branch and the headquarter of the bank.

 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelParserService {

    private final SwiftCodeRepository swiftCodeRepository;

    @PostConstruct
    public void init(){
        try{
            File file = new ClassPathResource("data/Interns_2025_SWIFT_CODES.xlsx").getFile();
            parseExcelFile(new FileInputStream(file));
            log.info("Successfully parsed excel file");

        }catch(Exception e){
            log.error("Error while parsing excel file", e.getMessage());
        }
    }

    private void parseExcelFile(InputStream inputStream) throws IOException {
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
                        .build();

                swiftCodesToSave.add(swift);

                if (isHeadquarter) {
                    headquartersMap.put(swiftCode, swift);
                }
            }

            //Save headquarters first, then branches to ensure proper references
            List<SwiftCode> headquartersToSave = swiftCodesToSave.stream()
                    .filter(SwiftCode::isHeadquarter)
                    .collect(Collectors.toList());
            swiftCodeRepository.saveAll(headquartersToSave);
            log.info("Successfully saved {} headquarters", headquartersToSave.size());

            List<SwiftCode> branchesToSave = swiftCodesToSave.stream()
                    .filter(code -> !code.isHeadquarter())
                    .collect(Collectors.toList());
            swiftCodeRepository.saveAll(branchesToSave);
            log.info("Successfully saved {} branches", branchesToSave.size());

            log.info("Successfully parsed and saved {} total SWIFT codes", swiftCodesToSave.size());

        }catch(Exception e){
            throw new RuntimeException("Error parsing Excel file" + e.getMessage());
        }


    }


}
