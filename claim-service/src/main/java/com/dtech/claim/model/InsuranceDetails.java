/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 9:33 AM
 * <p>
 */

package com.dtech.claim.model;

import com.dtech.claim.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "insurance_details")
@Data
public class InsuranceDetails extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "claim_limit",nullable = false)
    private BigDecimal claimLimit;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.PERSIST)
    @JoinColumn(name = "insurance_policy",nullable = false,referencedColumnName = "id")
    private InsurancePolicy insurancePolicy;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.PERSIST)
    @JoinColumn(name = "insurance_period",nullable = false,referencedColumnName = "id")
    private InsurancePeriod insurancePeriod;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.PERSIST)
    @JoinColumn(name = "treatment",nullable = false,referencedColumnName = "id")
    private Treatment treatment;

}
