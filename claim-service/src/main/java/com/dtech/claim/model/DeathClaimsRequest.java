/**
 * User: Himal_J
 * Date: 3/16/2025
 * Time: 8:35 AM
 * <p>
 */

package com.dtech.claim.model;

import com.dtech.claim.enums.Workflow;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "death_claims_request")
@Data
public class DeathClaimsRequest extends Audit implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @Column(name = "request_id", nullable = false, updatable = false, unique = true)
    private String requestId;

    @Column(name = "request_amount", nullable = false)
    private BigDecimal requestAmount;

    @Column(name = "request_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Workflow requestStatus;

    @Column(name = "remark")
    private String remark;

    @Column(name = "death_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date deathDate;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "dependent", updatable = false,referencedColumnName = "id")
    private ClaimsDependents claimsDependents;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "employee", nullable = false, updatable = false,referencedColumnName = "id")
    private ApplicationUser employee;

    @ManyToMany
    @JoinTable(
            name = "death_claims_details_document",
            joinColumns = @JoinColumn(name = "death_claims_details_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "document_id", referencedColumnName = "id")
    )
    private List<Document> documents = new ArrayList<>();
}
