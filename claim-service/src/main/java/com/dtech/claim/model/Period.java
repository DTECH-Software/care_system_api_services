/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 8:54 AM
 * <p>
 */

package com.dtech.claim.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "period")
@Data
public class Period extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "year",nullable = false,updatable = false)
    private Integer year;
}
