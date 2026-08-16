
/**
 * User: Himal_J
 * Date: 2/10/2025
 * Time: 8:06 PM
 * <p>
 */
 
package com.dtech.claim.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "application_otp_sessions")
@Data
public class ApplicationOtpSession extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @Column(name = "otp",nullable = false)
    private String otp;

    @Column(name = "purpose", length = 50)
    private String purpose;

    @Column(name = "application_user_id")
    private Long applicationUserId;

    @Column(name = "context_key")
    private String contextKey;

    @Column(name = "success",nullable = false)
    private boolean success;

    @Column(name = "validated",nullable = false)
    private boolean validated;

    @Column(name = "consumed", nullable = false)
    private boolean consumed;

}
