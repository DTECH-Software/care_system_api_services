package com.dtech.auth.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "application_user_device_details")
@Data
public class ApplicationUserDeviceDetails extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "device_id", nullable = false,unique = true)
    private String deviceId;

    @Column(name = "device_model", nullable = false)
    private String deviceModel;

    @Column(name = "device_os", nullable = false)
    private String deviceOS;

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @Column(name = "latitude", nullable = false)
    private String latitude;

    @Column(name = "longitude", nullable = false)
    private String longitude;

}
