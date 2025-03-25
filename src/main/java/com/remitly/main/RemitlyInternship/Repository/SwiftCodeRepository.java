package com.remitly.main.RemitlyInternship.Repository;

import com.remitly.main.RemitlyInternship.Model.SwiftCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SwiftCodeRepository extends JpaRepository<SwiftCode, Long> {
    Optional<SwiftCode> findBySwiftCode(String swiftCode);
    List<SwiftCode> findByCountryISO2(String countryISO2);
    //method to find branches based on the bank's headquarter.
    List<SwiftCode> findByHeadquarters(SwiftCode headquarters);
    boolean existsBySwiftCode(String swiftCode);

    //List<SwiftCode> findBySwiftCodeStartingWithAndIsHeadquarterFalse(String headquarterPrefix);

}
