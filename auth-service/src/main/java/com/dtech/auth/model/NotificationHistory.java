/**
 * User: Himal_J
 * Date: 4/1/2025
 * Time: 8:21 AM
 * <p>
 */

package com.dtech.auth.model;

import com.dtech.auth.enums.NotificationTitle;
import com.dtech.auth.enums.NotificationsType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "notification_history")
@Data
public class NotificationHistory extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @Column(name = "title",nullable = false,updatable = false)
    @Enumerated(EnumType.STRING)
    private NotificationTitle title;

    @Column(name = "body",nullable = false,updatable = false)
    private String body;

    @Column(name = "type",nullable = false,updatable = false)
    @Enumerated(EnumType.STRING)
    private NotificationsType type;

    @Column(name = "is_read",nullable = false,columnDefinition = "DEFAULT 0")
    private boolean isRead;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "employee", nullable = false, updatable = false,referencedColumnName = "id")
    private ApplicationUser employee;
}
