package com.remitly.main.RemitlyInternship.Service;

import com.remitly.main.RemitlyInternship.Model.SwiftCode;
import com.remitly.main.RemitlyInternship.Repository.SwiftCodeRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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

    public void parseExcelFile(MultipartFile file) throws IOException {
        try(InputStream inputStream = file.getInputStream();
            Workbook workbook = new XSSFWorkbook(inputStream)){

            Sheet sheet = workbook.getSheetAt(0); //downloading first sheet

            //iterating through rows cuz every row is a new record which has to be added to the database
            for (Row row : sheet) {
                String countryISO2 = row.getCell(0).getStringCellValue().toUpperCase();
                String swiftCode = row.getCell(1).getStringCellValue();
                boolean isHeadquarter = swiftCode.endsWith("XXX");
                String bankName = row.getCell(3).getStringCellValue().toUpperCase();
                String address = row.getCell(4).getStringCellValue();
                String countryName = row.getCell(6).getStringCellValue().toUpperCase();

                SwiftCode headquarters = null;
                //finding a headquarter for a bank branch
                if(!isHeadquarter) {
                    String hqSwiftCode = swiftCode.substring(0, 8) + "XXX";
                    headquarters = swiftCodeRepository.findBySwiftCode(hqSwiftCode)
                            .orElse(null);

                }

                SwiftCode swift = SwiftCode.builder()
                        .swiftCode(swiftCode)
                        .bankName(bankName)
                        .address(address)
                        .countryISO2(countryISO2)
                        .countryName(countryName)
                        .isHeadquarter(isHeadquarter)
                        .headquarters(headquarters) //null ---> it's the headquarter
                        .build();

                swiftCodeRepository.save(swift);
            }

            log.info("Successfully parsed excel file");

        }catch(Exception e){
            throw new RuntimeException("Error parsing Excel file" + e.getMessage());
        }


    }


}
