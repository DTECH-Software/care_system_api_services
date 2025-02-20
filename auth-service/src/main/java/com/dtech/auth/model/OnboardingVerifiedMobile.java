/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 9:57 AM
 * <p>
 */

package com.dtech.auth.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "onboadring_verified_mobile")
@Data
public class OnboardingVerifiedMobile extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "nic",nullable = false)
    private String nic;

    @Column(name = "epf_no",nullable = false)
    private int epfNo;

    @Column(name = "mobile",nullable = false,updatable = false)
    private String mobile;

    @Column(name = "verified",nullable = false)
    private boolean verified;

}
