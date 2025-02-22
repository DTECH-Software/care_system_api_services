/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 8:24 PM
 * <p>
 */

package com.dtech.login.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.util.Date;

@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", updatable = false,nullable = false)
    private Date createdDate;

    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_modified_date",nullable = false)
    private Date lastModifiedDate;

    @CreatedBy
    @Column(name = "created_user",updatable = false, nullable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_user",nullable = false)
    private String lastModifiedBy;

}
