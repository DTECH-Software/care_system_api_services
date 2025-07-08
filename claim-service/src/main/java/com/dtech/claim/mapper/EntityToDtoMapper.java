/**
 * User: Himal_J
 * Date: 3/14/2025
 * Time: 11:08 AM
 * <p>
 */

package com.dtech.claim.mapper;

import com.dtech.claim.dto.response.DeathClaimRequestResponseDTO;
import com.dtech.claim.dto.response.InsuranceClaimRequestResponseDTO;
import com.dtech.claim.dto.response.DocumentDownloadResponseDTO;
import com.dtech.claim.enums.PaymentType;
import com.dtech.claim.enums.RelationCategory;
import com.dtech.claim.enums.Workflow;
import com.dtech.claim.model.ApprovalWorkFlow;
import com.dtech.claim.model.InsuranceClaimsRequest;
import com.dtech.claim.model.DeathClaimRequest;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class EntityToDtoMapper {
    private static final ModelMapper modelMapper = new ModelMapper();
    public static InsuranceClaimRequestResponseDTO mapInsuranceClaimHistoryDetails(InsuranceClaimsRequest insuranceClaimsRequest) {
        try {

            log.info("Call to mapClaimHistoryDetails method {} ", insuranceClaimsRequest);
            InsuranceClaimRequestResponseDTO insuranceClaimRequestResponseDto = modelMapper.map(insuranceClaimsRequest, InsuranceClaimRequestResponseDTO.class);
            insuranceClaimRequestResponseDto.setRequestStatusDescription(Workflow.valueOf(insuranceClaimRequestResponseDto.getRequestStatus()).getDescription());
            if(insuranceClaimRequestResponseDto.getClaimsDependents() != null) {
                log.info("Found dependent claims: {}", insuranceClaimRequestResponseDto.getClaimsDependents());
                insuranceClaimRequestResponseDto.getClaimsDependents().setRelationCategoryDescription(RelationCategory.valueOf(insuranceClaimRequestResponseDto.getClaimsDependents().getRelationCategory().name()).getDescription());

            }

            if(insuranceClaimsRequest.getApprovalWorkFlows() != null) {

                List<ApprovalWorkFlow> approvalWorkFlows = insuranceClaimsRequest.getApprovalWorkFlows();

                approvalWorkFlows.stream().filter(approvalWorkFlow -> approvalWorkFlow.getApprovalLevel().equals(insuranceClaimsRequest.getApprovalLevel()))
                        .forEach(val -> {
                            insuranceClaimRequestResponseDto.setRemark(val.getRejectedRemark());
                            insuranceClaimRequestResponseDto.setApprovedDateTime(val.getApprovedDate());
                });

            }
//            List<DocumentDownloadResponseDTO> collect = insuranceClaimsRequest.getInsuranceClaimsDetails().getDocuments().stream().map((document -> {
//                log.info("inside document mapper {} ",document);
//                return new DocumentDownloadResponseDTO(String.valueOf(document.getType()), document.getFileName(), document.getFileType(),document.getDoc());
//            })).collect(Collectors.toList());
   //         insuranceClaimRequestResponseDto.getInsuranceClaimsDetails().setDocuments(collect);
            return insuranceClaimRequestResponseDto;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    public static DeathClaimRequestResponseDTO mapDeathClaimHistoryDetails(DeathClaimRequest deathClaimRequest) {
        try {
            log.info("Call to map death claim history method {} ", deathClaimRequest);
            DeathClaimRequestResponseDTO deathClaimRequestResponseDTO = modelMapper.map(deathClaimRequest, DeathClaimRequestResponseDTO.class);
            deathClaimRequestResponseDTO.setRequestStatusDescription(Workflow.valueOf(deathClaimRequestResponseDTO.getRequestStatus()).getDescription());
            deathClaimRequestResponseDTO.setPaymentTypeDescription(PaymentType.valueOf(deathClaimRequestResponseDTO.getPaymentType()).getDescription());
            deathClaimRequestResponseDTO.getClaimsDependents().setRelationCategoryDescription(RelationCategory.valueOf(deathClaimRequestResponseDTO.getClaimsDependents().getRelationCategory().name()).getDescription());
//            List<DocumentDownloadResponseDTO> collect = deathClaimRequest.getDocuments().stream().map((document -> {
//                log.info("inside document mapper death history {} ",document);
//                return new DocumentDownloadResponseDTO(String.valueOf(document.getType()), document.getFileName(), document.getFileType(),document.getDoc());
//            })).collect(Collectors.toList());
//            deathClaimRequestResponseDTO.setDocuments(collect);
            return deathClaimRequestResponseDTO;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
