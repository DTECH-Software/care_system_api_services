/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 2:28 PM
 * <p>
 */

package com.dtech.claim.model;

import com.dtech.claim.enums.DocType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true,exclude = "claimsDependents")
@Entity
@Table(name = "document")
@Data
@ToString(exclude = {"claimsDependents","doc"})
public class Document extends Audit implements Serializable {

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
    private String doc;

    @Column(name = "file_name",nullable = false)
    private String fileName;

    @Column(name = "file_type",nullable = false)
    private String fileType;

    @ManyToMany(mappedBy = "documents")
    private List<ClaimsDependents> claimsDependents = new ArrayList<>();
}
