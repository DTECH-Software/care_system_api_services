/**
 * User: Himal_J
 * Date: 2/18/2025
 * Time: 3:43 PM
 * <p>
 */

package com.dtech.auth.model;

import com.dtech.auth.enums.Status;
import com.fasterxml.jackson.annotation.JsonRawValue;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "onboarding_request")
@Data
public class OnboardingRequest extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "user_custom_details",nullable = false)
    @JsonRawValue
    private String userCustomDetails;

    @Column(name = "request_status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status requestStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "onboarding_request_verified")
    private OnboardingVerifiedMobile onboardingVerifiedMobile;

    @OneToOne(mappedBy = "onboardingRequest")
    private ApplicationUser onboardingRequest;

}
