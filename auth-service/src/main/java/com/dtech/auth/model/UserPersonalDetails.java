/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 8:58 AM
 * <p>
 */

package com.dtech.auth.model;

import com.dtech.auth.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_personal_details")
@Data
public class UserPersonalDetails extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "epf_no",nullable = false,updatable = false)
    private int epfNo;

    @Column(name = "initials",nullable = false,length = 30)
    private String initials;

    @Column(name = "first_name",nullable = false,length = 30)
    private String firstName;

    @Column(name = "last_name",nullable = false,length = 30)
    private String lastName;

    @Column(name = "nic",nullable = false,updatable = false)
    private String nic;

    @Column(name = "email",nullable = false)
    private String email;

    @Column(name = "mobile_no",nullable = false)
    private String mobileNo;

    @Column(name = "dob",nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dob;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "user_address",nullable = false,referencedColumnName = "id")
    private UserAddress userAddress;

    @Column(name = "user_status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status userStatus;

}
