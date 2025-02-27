/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 12:02 PM
 * <p>
 */

package com.dtech.auth.model;

import com.dtech.auth.enums.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "claims_dependents")
@Data
public class ClaimsDependents extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "dependentCategory",nullable = false)
    @Enumerated(EnumType.STRING)
    private DependentCategory dependentCategory;

    @Column(name = "initials",nullable = false)
    private String initials;

    @Column(name = "first_name",nullable = false)
    private String firstName;

    @Column(name = "last_name",nullable = false)
    private String lastName;

    @Column(name = "dob",nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dob;

    @Column(name = "gender",nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "nic")
    private String nic;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "relation_category",nullable = false)
    @Enumerated(EnumType.STRING)
    private RelationCategory relationCategory;

    @OneToMany(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "document",nullable = false)
    private List<ClaimsDependentsVerificationDocument> claimsDependentsVerificationDocuments;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Workflow status;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "application_user",nullable = false)
    private ApplicationUser applicationUser;

}
