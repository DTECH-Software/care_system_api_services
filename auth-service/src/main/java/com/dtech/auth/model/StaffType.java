/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 9:04 AM
 * <p>
 */

package com.dtech.auth.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "staff_type")
@Data
public class StaffType extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "code",nullable = false,updatable = false,unique = true)
    private String code;

    @Column(name = "description",nullable = false)
    private String description;
}
