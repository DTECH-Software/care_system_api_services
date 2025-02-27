/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 8:35 AM
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
@Table(name = "insurance_period")
@Data
public class InsurancePeriod extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "from",nullable = false)
    @Temporal(TemporalType.DATE)
    private Date form;

    @Column(name = "to",nullable = false)
    @Temporal(TemporalType.DATE)
    private Date to;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

}
