
/**
 * User: Himal_J
 * Date: 2/10/2025
 * Time: 8:06 PM
 * <p>
 */
 
package com.dtech.login.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

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

    @Column(name = "success",nullable = false)
    private int success;

}
