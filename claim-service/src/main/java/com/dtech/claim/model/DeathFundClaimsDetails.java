/**
 * User: Himal_J
 * Date: 3/3/2025
 * Time: 11:17 AM
 * <p>
 */

package com.dtech.claim.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "death_fund_claims_details")
@Data
@ToString(exclude = "documents")
public class DeathFundClaimsDetails extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @Column(name = "death_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date deathDate;

    @Column(name = "hr_informed_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date hrInformedDate;

    @Column(name = "district_code", nullable = false)
    private String districtCode;

    @Column(name = "death_certificate_number", nullable = false)
    private String deathCertificateNumber;

    @ManyToMany
    @JoinTable(
            name = "death_fund_claims_details_document",
            joinColumns = @JoinColumn(name = "death_fund_claims_details_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "document_id", referencedColumnName = "id")
    )
    private List<Document> documents = new ArrayList<>();
}
