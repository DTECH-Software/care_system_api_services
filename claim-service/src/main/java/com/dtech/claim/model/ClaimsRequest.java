/**
 * User: Himal_J
 * Date: 3/2/2025
 * Time: 2:09 PM
 * <p>
 */

package com.dtech.claim.model;

import com.dtech.claim.enums.Workflow;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;


@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "claims_request")
@Data
public class ClaimsRequest extends Audit implements Serializable {

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

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "dependent", updatable = false,referencedColumnName = "id")
    private ClaimsDependents claimsDependents;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "employee", nullable = false, updatable = false,referencedColumnName = "id")
    private ApplicationUser employee;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.PERSIST)
    @JoinColumn(name = "insurance_claims_details",updatable = false)
    private InsuranceClaimsDetails insuranceClaimsDetails;

}
