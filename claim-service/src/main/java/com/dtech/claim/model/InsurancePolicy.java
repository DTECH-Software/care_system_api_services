/**
 * User: Himal_J
 * Date: 3/1/2025
 * Time: 8:53 PM
 * <p>
 */

package com.dtech.claim.model;

import com.dtech.claim.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "insurance_policy")
@Data
public class InsurancePolicy extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "policy_code",nullable = false,updatable = false,unique = true)
    private String policyCode;

    @Column(name = "policy_name",nullable = false)
    private String policyName;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

}
