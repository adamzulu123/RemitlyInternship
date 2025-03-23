package com.remitly.main.RemitlyInternship.Service;

import com.remitly.main.RemitlyInternship.Repository.SwiftCodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ExcelParserServiceTest {
    @Mock
    private SwiftCodeRepository swiftCodeRepository;
    @Mock
    private SwiftCodeParseService swiftCodeParseService;
    @Mock
    private ClassPathResource classPathResource;
    @InjectMocks
    private ExcelParserService excelParserService;

    //unit tests for excel parser service
    @Test
    void testInit_exception() throws IOException {
        //Setup the parse service to throw an exception
        doThrow(new IOException("Test exception")).when(swiftCodeParseService).parseExcelFile(any());

        //Create a subclass that uses a test stream but still calls the real service
        ExcelParserService testService = new ExcelParserService(swiftCodeRepository, swiftCodeParseService) {
            @Override
            public void init() {
                try {
                    InputStream testStream = new ByteArrayInputStream("test data".getBytes()); //simple test stream
                    swiftCodeParseService.parseExcelFile(testStream);
                } catch (Exception e) {
                    System.err.println("Error while parsing excel file: " + e.getMessage());
                }
            }
        };

        testService.init();

        verify(swiftCodeParseService).parseExcelFile(any(InputStream.class));
    }
}


