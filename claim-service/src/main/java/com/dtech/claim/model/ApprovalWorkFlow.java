package com.dtech.claim.model;

import com.dtech.claim.enums.ApprovalLevel;
import com.dtech.claim.enums.Workflow;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "approval_work_flow")
@Data
public class ApprovalWorkFlow extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "approved_level",nullable = false)
    @Enumerated(EnumType.STRING)
    private ApprovalLevel approvalLevel;

    @Column(name = "approved_date", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date approvedDate;

    @Column(name = "approved_user",updatable = false)
    private String approvedUser;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Workflow status;

    @Column(name = "rejected_remak")
    private String rejectedRemark;

    @ManyToMany(mappedBy = "approvalWorkFlows")
    private List<InsuranceClaimsRequest> claimsRequests = new ArrayList<>();

}