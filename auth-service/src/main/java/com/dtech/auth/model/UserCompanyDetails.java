/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 8:16 AM
 * <p>
 */

package com.dtech.auth.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_company_details")
@Data
public class UserCompanyDetails extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "company_type",nullable = false,referencedColumnName = "id")
    private CompanyTypes companyType;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "staff_category",nullable = false,referencedColumnName = "id")
    private StaffCategories staffCategories;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "staff_type",nullable = false,referencedColumnName = "id")
    private StaffTypes staffTypes;

    @Column(name = "permanent_date",nullable = false,updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date permanentDate;

    @Column(name = "terminate_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date terminateDate;

    @OneToOne(mappedBy = "userCompanyDetails")
    private UserPersonalDetails userCompanyDetails;

}
