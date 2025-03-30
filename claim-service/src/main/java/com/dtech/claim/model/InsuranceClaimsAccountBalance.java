/**
 * User: Himal_J
 * Date: 3/3/2025
 * Time: 9:59 AM
 * <p>
 */

package com.dtech.claim.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "claims_account_balance")
@Data
public class InsuranceClaimsAccountBalance extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "utilize_amount",nullable = false)
    private BigDecimal utilizeAmount;

    @Column(name = "available_balance",nullable = false)
    private BigDecimal availableBalance;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.PERSIST)
    @JoinColumn(name = "employee",nullable = false)
    private ApplicationUser employee;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.PERSIST)
    @JoinColumn(name = "treatment",nullable = false)
    private Treatment treatment;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.PERSIST)
    @JoinColumn(name = "year",nullable = false)
    private InsurancePeriod insurancePeriod;

}
