package com.remitly.main.RemitlyInternship.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwiftCodeRequestDTO {
    private String address; //it can be null because we see few examples where address is null in the excel file
    @NotBlank(message = "Bank name is required")
    private String bankName;
    @NotBlank(message = "countryISO2 is required")
    @Size(min=2, max = 2, message = "countryISO2 must contain exactly 2 characters")
    private String countryISO2;
    @NotBlank(message = "countryName is required")
    private String countryName;
    @NotNull(message = "isHeadquarter information (flag) is required")
    private Boolean isHeadquarter;
    /*
      SwiftCode has to be exactly 11 numbers following the pattern presented below.
      So 6 letters + 2 letters or numbers + 3 letter or numbers
     */
    @NotBlank(message = "swiftCode is required")
    @Size(min = 11, max = 11, message = "SWIFT code must be exactly 11 characters")
    @Pattern(regexp = "^[A-Z]{6}[A-Z0-9]{2}[A-Z0-9]{3}$", message = "SWIFT code format is invalid")
    private String swiftCode;


}
