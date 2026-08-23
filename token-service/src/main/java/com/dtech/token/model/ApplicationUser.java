/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 8:11 PM
 * <p>
 */

package com.dtech.token.model;

import com.dtech.token.enums.Channel;
import com.dtech.token.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "application_user")
@Data
public class ApplicationUser extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "username",nullable = false,updatable = false,unique = true)
    private String username;

    @Column(name = "password",nullable = false)
    private String password;

    @Column(name = "primary_email",unique = true,nullable = false)
    private String primaryEmail;

    @Column(name = "primary_mobile",unique = true,nullable = false)
    private String primaryMobile;

    @Column(name = "login_status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status loginStatus;

    @Column(name = "is_reset",nullable = false)
    private int isReset;

    @Column(name = "user_key",nullable = false,updatable = false)
    private String userKey;

    @Column(name = "last_logged_channel")
    @Enumerated(EnumType.STRING)
    private Channel lastLoggedChannel;

    @Column(name = "last_password_change_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastPasswordChangeDate;

    @Column(name = "last_logged_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLoggedDate;

    @Column(name = "password_expired_date",nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date passwordExpiredDate;

    @Column(name = "attempt_count",nullable = false)
    private int attemptCount;

}
