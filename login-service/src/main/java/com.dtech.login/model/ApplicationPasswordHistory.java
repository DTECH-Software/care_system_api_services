/**
 * User: Himal_J
 * Date: 2/6/2025
 * Time: 8:51 AM
 * <p>
 */

package com.dtech.login.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "application_password_history")
@Entity
public class ApplicationPasswordHistory extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false,updatable = false)
    private ApplicationUser applicationUser;

    @Column(name = "password",nullable = false)
    @Lob
    private String password;

}
