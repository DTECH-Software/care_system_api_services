package com.dtech.token.model;

import com.dtech.token.enums.Status;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Entity
@Table(name = "web_user")
@Data
public class WebUser implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "login_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status loginStatus;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_role", referencedColumnName = "code")
    private WebUserRole userRole;
}
