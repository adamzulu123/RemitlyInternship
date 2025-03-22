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
import org.springframework.transaction.annotation.Transactional;
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
    private final SwiftCodeParseService swiftCodeParseService;

    //@PostConstruct
    public void init(){
        try{
            File file = new ClassPathResource("data/Interns_2025_SWIFT_CODES.xlsx").getFile();
            swiftCodeParseService.parseExcelFile(new FileInputStream(file));
            log.info("Successfully parsed excel file");

        }catch(Exception e){
            log.error("Error while parsing excel file", e);
        }
    }

}
