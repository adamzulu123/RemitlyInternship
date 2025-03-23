package com.remitly.main.RemitlyInternship.Service;

import com.remitly.main.RemitlyInternship.Exception.ExcelParseException;
import com.remitly.main.RemitlyInternship.Model.SwiftCode;
import com.remitly.main.RemitlyInternship.Repository.SwiftCodeRepository;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ExcelParseIntegrationTest {

    @Autowired
    private SwiftCodeParseService swiftCodeParseService;

    @Autowired
    private SwiftCodeRepository swiftCodeRepository;

    @BeforeEach
    void setUp() {
        //Clear the database before each test
        swiftCodeRepository.deleteAll();
    }

    @Test
    @Transactional
    void testParseExcelFile() throws Exception{
        InputStream inputStream = new ClassPathResource("data/Interns_2025_SWIFT_CODES.xlsx").getInputStream();

        //parsing input file
        swiftCodeParseService.parseExcelFile(inputStream);

        List<SwiftCode> allSwiftCodes = swiftCodeRepository.findAll();

        //1. verifying if all data was loaded
        assertFalse(allSwiftCodes.isEmpty(), "Excel file should be parsed and data saved");

        //2. Test headquarters identification
        List<SwiftCode> headquarters = allSwiftCodes.stream()
                .filter(SwiftCode::isHeadquarter)
                .toList();

        assertFalse(headquarters.isEmpty(), "Should have headquarters records");
        assertTrue(headquarters.stream().allMatch(hq -> hq.getSwiftCode().endsWith("XXX")),
                "All headquarters should have codes ending with XXX");

        //3. Test headquarter-branch relation
        for (SwiftCode hq : headquarters) {
            String hqPrefix = hq.getSwiftCode().substring(0, 8);

            for (SwiftCode branch : hq.getBranches()) {


                assertEquals(hq, branch.getHeadquarters(), "Branch should reference correct headquarters");
                assertTrue(branch.getSwiftCode().startsWith(hqPrefix), "Prefix (8 chars) should be the same");
                assertFalse(branch.isHeadquarter(), "Branch should not be marked as headquarters");
            }
        }

        //4. verifying data format like UPPERCASE letters and length of some properties
        assertTrue(allSwiftCodes.stream().allMatch(code -> code.getSwiftCode().length() == 11));

        assertTrue(allSwiftCodes.stream().allMatch(code ->
                        code.getCountryISO2() != null &&
                                code.getCountryISO2().equals(code.getCountryISO2().toUpperCase()) &&
                                code.getCountryISO2().length() == 2),
                "Country ISO2 codes should be 2 characters and uppercase");

        assertTrue(allSwiftCodes.stream().allMatch(code ->
                code.getCountryName().equals(code.getCountryName().toUpperCase()) &&
                        code.getBankName().equals(code.getBankName().toUpperCase())
                ));

        //5. verifying duplicates by parsing the same file again
        long initialCount = swiftCodeRepository.count();
        InputStream inputStream2 = new ClassPathResource("data/Interns_2025_SWIFT_CODES.xlsx").getInputStream();
        swiftCodeParseService.parseExcelFile(inputStream2);
        long finalCount = swiftCodeRepository.count();

        assertEquals(initialCount, finalCount, "Initial count should be the same");
    }


    //TESTING SOME EDGE CASES WITH MISSING ROWS OR INVALID DATA

    @Test
    void testExcelWithoutSheets() throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        //NO DATA

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray());

        ExcelParseException exception = assertThrows(ExcelParseException.class, () -> {
            swiftCodeParseService.parseExcelFile(inputStream);
        });

        assertTrue(exception.getMessage().contains("Excel file does not contain any sheets"));
    }

    @Test
    void testExcelWithMissing() throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("SWIFT CODE");
        headerRow.createCell(1).setCellValue("NAME");
        //missing country NAME and ISO2

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray());

        ExcelParseException exception = assertThrows(ExcelParseException.class, () -> {
            swiftCodeParseService.parseExcelFile(inputStream);
        });

        //we except this because its first needed header (before country NAME).
        assertTrue(exception.getMessage().contains("Required column not found: COUNTRY ISO2 CODE"));

    }

    @Test
    void testNullInputStream() {
        assertThrows(ExcelParseException.class, () -> {
            swiftCodeParseService.parseExcelFile(null);
        });
    }



}
