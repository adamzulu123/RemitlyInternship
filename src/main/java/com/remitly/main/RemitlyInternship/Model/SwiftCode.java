package com.remitly.main.RemitlyInternship.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
//indexes are created on the 'swiftCode' and 'countryISO2'
//columns to optimize search queries and improve query performance for filtering by these fields.
@Table(name = "swift_codes", indexes = {
    @Index(name="idx_swift_code", columnList = "swiftCode", unique = true),
    @Index(name="idx_country_iso2", columnList = "countryISO2")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwiftCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String swiftCode;

    private String bankName;
    private String address;
    private String countryISO2;
    private String countryName;
    private boolean isHeadquarter; //flag to represent if the record is headquarter

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "headquarters_id")
    private SwiftCode headquarters;

    //mappedBy = "headquarters" --> means that one headquarters can have multiple related branches.
    //cascade = CascadeType.ALL --> if we delete headquarter we delete all branches
    @OneToMany(mappedBy = "headquarters", cascade = CascadeType.ALL)
    private List<SwiftCode> branches = new ArrayList<>();
}
