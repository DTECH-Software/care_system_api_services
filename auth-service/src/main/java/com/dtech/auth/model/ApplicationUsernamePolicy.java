/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 8:22 AM
 * <p>
 */

package com.dtech.auth.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "application_username_policy")
@Data
public class ApplicationUsernamePolicy extends Audit implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "min_upper_case", nullable = false,length = 2)
    private int minUpperCase;

    @Column(name = "min_lower_case", nullable = false,length = 2)
    private int minLowerCase;

    @Column(name = "min_numbers", nullable = false,length = 2)
    private int minNumbers;

    @Column(name = "min_special_characters", nullable = false,length = 2)
    private int minSpecialCharacters;

    @Column(name = "min_length", nullable = false,length = 2)
    private int minLength;

    @Column(name = "max_length", nullable = false,length = 2)
    private int maxLength;

}
