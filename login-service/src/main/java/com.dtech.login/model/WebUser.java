package com.dtech.login.model;

import com.dtech.login.enums.Status;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "web_user")
@Data
public class WebUser implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    @Lob
    private String password;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "mobile", nullable = false)
    private String mobile;

    @Column(name = "login_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status loginStatus;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "is_reset", nullable = false)
    private boolean isReset;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "user_key", nullable = false)
    @Lob
    private String userKey;

    @Column(name = "last_password_change_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastPasswordChangeDate;

    @Column(name = "last_logged_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLoggedDate;

    @Column(name = "password_expired_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date passwordExpiredDate;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_role", referencedColumnName = "code")
    private WebUserRole userRole;
}
