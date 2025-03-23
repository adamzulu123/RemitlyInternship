package com.remitly.main.RemitlyInternship.Exception;


import com.remitly.main.RemitlyInternship.DTO.MessageResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

//handling rest exceptions
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SwiftCodeNotFoundException.class)
    public ResponseEntity<MessageResponseDTO> handleSwiftCodeNotFoundException (SwiftCodeNotFoundException ex) {
        log.warn("SwiftCode not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new MessageResponseDTO(ex.getMessage()));

    }

    @ExceptionHandler(SwiftCodeExistsException.class)
    public ResponseEntity<MessageResponseDTO> handleSwiftCodeExistsException (SwiftCodeExistsException ex) {
        log.warn("SwiftCode already exists: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new MessageResponseDTO(ex.getMessage()));
    }

    //handling exceptions in POST: create swiftCode endpoint
    //error we get during @Valid with SwiftCodeRequestDTO, so if user don't provide bankName or
    //countryISO2 in the input JSON, we handle exceptions with occur because of it.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation exception: {}", ex.getMessage());

        //BindingResult is an interface in Spring that holds the results of object validation,
        //providing access to validation errors when the data in an HTTP request doesn't match the defined constraints.
        Map<String, String> errors = new HashMap<>();
        //for each validation error, we retrieve field name, and message
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        //returning the answer with all error and their messages which occurred during validation
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponseDTO> handleUnknownException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponseDTO("An unexpected error occurred. Please try again later."));
    }

    @ExceptionHandler(ExcelParseException.class)
    public ResponseEntity<MessageResponseDTO> handleExcelParseException(ExcelParseException ex) {
        log.error("Excel parse error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponseDTO("Error parsing Excel file: " + ex.getMessage()));
    }
}
