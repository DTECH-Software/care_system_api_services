package com.dtech.auth.model;

import com.dtech.auth.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;


@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "insurance_policy")
@Data
public class InsurancePolicy extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "policy_code",nullable = false,updatable = false,unique = true)
    private String code;

    @Column(name = "policy_name",nullable = false)
    private String description;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

}
