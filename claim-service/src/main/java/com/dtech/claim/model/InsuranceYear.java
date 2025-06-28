/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 8:35 AM
 * <p>
 */

package com.dtech.claim.model;

import com.dtech.claim.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "insurance_year")
@Data
public class InsuranceYear extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "code",nullable = false,updatable = false,unique = true)
    private String code;

    @Column(name = "description",nullable = false)
    private String description;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "insurance_staff_category_period",nullable = false,referencedColumnName = "id")
    private InsuranceStaffCategoryPeriod insuranceStaffCategoryPeriod;

}
