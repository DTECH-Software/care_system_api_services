package com.dtech.token.model;


import com.dtech.token.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "application_user_sessions")
@Data
public class ApplicationUserSession extends Audit{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false,referencedColumnName = "id")
    private ApplicationUser applicationUser;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

}
