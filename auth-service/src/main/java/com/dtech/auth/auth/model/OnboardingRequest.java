/**
 * User: Himal_J
 * Date: 2/18/2025
 * Time: 3:43 PM
 * <p>
 */

package com.dtech.auth.auth.model;

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

    @Column(name = "request_id",updatable = false,nullable = false,unique = true)
    private String requestId;


}
