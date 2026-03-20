package com.dtech.login.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "application_user_biometrics")
@Data
public class ApplicationUserBiometric extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    private ApplicationUser applicationUser;

    @Column(name = "unique_code", length = 2048)
    private String uniqueCode;

    @Column(name = "app_id", length = 255)
    private String appId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_used_date")
    private Date lastUsedDate;
}
