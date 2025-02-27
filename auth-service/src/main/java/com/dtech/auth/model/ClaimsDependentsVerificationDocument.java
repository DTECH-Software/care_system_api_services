/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 2:28 PM
 * <p>
 */

package com.dtech.auth.model;

import com.dtech.auth.enums.DocType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "document")
@Data
public class ClaimsDependentsVerificationDocument extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "type",nullable = false)
    @Enumerated(EnumType.STRING)
    private DocType type;

    @Column(name = "doc",nullable = false)
    @Lob
    private byte[] doc;

}
