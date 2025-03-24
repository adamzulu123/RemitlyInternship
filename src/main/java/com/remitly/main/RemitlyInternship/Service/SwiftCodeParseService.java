package com.remitly.main.RemitlyInternship.Service;

import com.remitly.main.RemitlyInternship.Exception.ExcelParseException;
import com.remitly.main.RemitlyInternship.Model.SwiftCode;
import com.remitly.main.RemitlyInternship.Repository.SwiftCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
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

    private static final String COUNTRY_ISO2_HEADER = "COUNTRY ISO2 CODE";
    private static final String SWIFT_CODE_HEADER = "SWIFT CODE";
    private static final String BANK_NAME_HEADER = "NAME";
    private static final String ADDRESS_HEADER = "ADDRESS";
    private static final String COUNTRY_NAME_HEADER = "COUNTRY NAME";

    private final SwiftCodeRepository swiftCodeRepository;

    @Transactional
    protected void parseExcelFile(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new ExcelParseException("Input stream cannot be null");
        }

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new ExcelParseException("Excel file does not contain any sheets");
            }

            Sheet sheet = workbook.getSheetAt(0); //downloading first sheet

            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new ExcelParseException("Excel file is empty");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new ExcelParseException("Missing headers row");
            }

            //verification of all necessary columns
            Map<String, Integer> columnIndexMap = mapColumnIndices(headerRow);
            verifyRequiredColumns(columnIndexMap);

            //cache for heaquarters, it's important to save headquarters first
            Map<String, SwiftCode> headquartersMap = new HashMap<>();
            List<SwiftCode> swiftCodesToSave = new ArrayList<>();

            //Add a set to track unique swift codes to prevent duplicates in the same Excel file
            Set<String> uniqueSwiftCodes = new HashSet<>();

            //iterating through rows cuz every row is a new record which has to be added to the database
            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);

                if (row == null) {
                    log.warn("Empty row at index: {}, skipping", i);
                    continue;
                }

                //skipping header row
                if(row.getRowNum() == 0){
                    continue;
                }

                try {
                    String countryISO2 = getCellValueSafely(row, columnIndexMap.get(COUNTRY_ISO2_HEADER));
                    if (countryISO2 != null) {
                        countryISO2 = countryISO2.toUpperCase();
                    } else {
                        log.warn("Missing country ISO2 at row: {}, skipping", i);
                        continue;
                    }

                    String swiftCode = getCellValueSafely(row, columnIndexMap.get(SWIFT_CODE_HEADER));
                    if (swiftCode == null) {
                        log.warn("Missing SWIFT code at row: {}, skipping", i);
                        continue;
                    }

                    //Skip if this swift code was already processed in this file
                    if (!uniqueSwiftCodes.add(swiftCode)) {
                        log.warn("Duplicate SWIFT code found in file: {}, skipping", swiftCode);
                        continue;
                    }

                    //this should also ensure that no same swiftcodes are saved to the database (more important)
                    Optional<SwiftCode> existingSwiftCode = swiftCodeRepository.findBySwiftCode(swiftCode);
                    if (existingSwiftCode.isPresent()) {
                        log.warn("This swift code already exists in DB: {}", swiftCode);
                        continue;
                    }

                    boolean isHeadquarter = swiftCode.endsWith("XXX");

                    String bankName = getCellValueSafely(row, columnIndexMap.get(BANK_NAME_HEADER));
                    if (bankName != null) {
                        bankName = bankName.toUpperCase();
                    } else {
                        log.warn("Missing bank name at row: {}, skipping", i);
                        continue;
                    }

                    String address = getCellValueSafely(row, columnIndexMap.get(ADDRESS_HEADER), "");

                    String countryName = getCellValueSafely(row, columnIndexMap.get(COUNTRY_NAME_HEADER));
                    if (countryName != null) {
                        countryName = countryName.toUpperCase();
                    } else {
                        log.warn("Missing country name at row: {}, skipping", i);
                        continue;
                    }

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
                } catch (Exception e) {
                    log.warn("Error processing row {}: {}", i, e.getMessage());
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

        } catch (ExcelParseException e) {
            log.error("Excel parsing error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error parsing Excel file", e);
            throw new ExcelParseException("Error parsing Excel file: " + e.getMessage(), e);
        }

    }


    //HELPER methods


    //creating a map which align columns (headers) with indexes in the Excel File
    //this prevents a situation when the order of the columns in the Excel File change
    private Map<String, Integer> mapColumnIndices(Row headerRow) {
        Map<String, Integer> columnIndexMap = new HashMap<>();

        for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String headerValue = cell.getStringCellValue();
                if (headerValue != null && !headerValue.trim().isEmpty()) {
                    columnIndexMap.put(headerValue.trim(), i);
                }
            }
        }

        return columnIndexMap;
    }

    //checking if all necessary columns exits in the Excel file
    private void verifyRequiredColumns(Map<String, Integer> columnIndexMap) {
        List<String> requiredColumns = Arrays.asList(
                COUNTRY_ISO2_HEADER,
                SWIFT_CODE_HEADER,
                BANK_NAME_HEADER,
                COUNTRY_NAME_HEADER
        );

        for (String requiredColumn : requiredColumns) {
            if (!columnIndexMap.containsKey(requiredColumn)) {
                throw new ExcelParseException("Required column not found: " + requiredColumn);
            }
        }
    }

    //safely retrieving value from the excel cell
    private String getCellValueSafely(Row row, Integer columnIndex) {
        return getCellValueSafely(row, columnIndex, null);
    }

    private String getCellValueSafely(Row row, Integer columnIndex, String defaultValue) {
        if (columnIndex == null || row == null) {
            return defaultValue;
        }

        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return defaultValue;
        }

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue().toString();
                    } else {
                        return String.valueOf(cell.getNumericCellValue());
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    return cell.getCellFormula();
                default:
                    return defaultValue;
            }
        } catch (Exception e) {
            log.warn("Error reading cell value at column {}: {}", columnIndex, e.getMessage());
            return defaultValue;
        }
    }

    //in our case excel file is good and this validation is not needed but in real life not always it's that awesome
    private boolean isValid(SwiftCode swiftCode){
        String swiftCodeValue = swiftCode.getSwiftCode();
        if (swiftCode.getSwiftCode() == null || swiftCodeValue.length() != 11) {
            log.warn("Invalid SWIFT code length for {}", swiftCode.getSwiftCode());
            return false;
        }
        if (swiftCode.getBankName() == null || swiftCode.getBankName().isEmpty()) {
            log.warn("Invalid bank name for SWIFT code {}", swiftCode.getSwiftCode());
            return false;
        }
        if (swiftCode.getCountryISO2() == null || swiftCode.getCountryISO2().length() != 2) {
            log.warn("Invalid country code for SWIFT code {}", swiftCode.getSwiftCode());
            return false;
        }
        if (swiftCode.getCountryName() == null || swiftCode.getCountryName().isEmpty()) {
            log.warn("Invalid country name for SWIFT code {}", swiftCode.getSwiftCode());
            return false;
        }
        return true;

    }
}
