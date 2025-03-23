package com.remitly.main.RemitlyInternship.Service;

import com.remitly.main.RemitlyInternship.Repository.SwiftCodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;


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

    //@PostConstruct - this annotation allow us tu start this method everytime when we start out app
    public void init(){
        try{
            InputStream inputStream = new ClassPathResource("data/Interns_2025_SWIFT_CODES.xlsx").getInputStream();
            swiftCodeParseService.parseExcelFile(inputStream);
            log.info("Successfully parsed excel file");

        }catch(Exception e){
            log.error("Error while parsing excel file", e);
        }
    }

}
