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
import com.dtech.claim.model.InsuranceStaffCategoryPeriod;
import com.dtech.claim.model.InsuranceClaimsRequest;
import com.dtech.claim.model.DeathClaimRequest;
import com.dtech.claim.model.StaffCategories;
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
            populateClaimStaffCategory(insuranceClaimRequestResponseDto, insuranceClaimsRequest);
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
            populateClaimStaffCategory(deathClaimRequestResponseDTO, deathClaimRequest);

            if(deathClaimRequestResponseDTO.getClaimsDependents() != null){
                deathClaimRequestResponseDTO.getClaimsDependents().setRelationCategoryDescription(RelationCategory.valueOf(deathClaimRequestResponseDTO.getClaimsDependents().getRelationCategory().name()).getDescription());

            }

            if(deathClaimRequest.getApprovalWorkFlows() != null) {

                List<ApprovalWorkFlow> approvalWorkFlows = deathClaimRequest.getApprovalWorkFlows();

                approvalWorkFlows.stream().filter(approvalWorkFlow -> approvalWorkFlow.getApprovalLevel().equals(deathClaimRequest.getApprovalLevel()))
                        .forEach(val -> {
                            deathClaimRequestResponseDTO.setRemark(val.getRejectedRemark());
                            deathClaimRequestResponseDTO.setApprovedDateTime(val.getApprovedDate());
                        });

            }

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

    private static void populateClaimStaffCategory(InsuranceClaimRequestResponseDTO dto, InsuranceClaimsRequest claim) {
        StaffCategories staffCategory = resolveClaimStaffCategory(claim);
        if (staffCategory == null) {
            return;
        }
        dto.setStaffCategoryCode(staffCategory.getCode());
        dto.setStaffCategoryDescription(staffCategory.getDescription());
    }

    private static StaffCategories resolveClaimStaffCategory(InsuranceClaimsRequest claim) {
        InsuranceStaffCategoryPeriod period = null;
        if (claim.getInsuranceDetailsLimit() != null) {
            period = claim.getInsuranceDetailsLimit().getInsuranceStaffCategoryPeriod();
        }
        if (period == null && claim.getInsuranceClaimsDetails() != null) {
            period = claim.getInsuranceClaimsDetails().getInsuranceStaffCategoryPeriod();
        }
        return period != null ? period.getStaffCategories() : null;
    }

    private static void populateClaimStaffCategory(DeathClaimRequestResponseDTO dto, DeathClaimRequest claim) {
        String code = extractStaffCategoryFromDeathRequestId(claim.getRequestId());
        if (code != null) {
            dto.setStaffCategoryCode(code);
            dto.setStaffCategoryDescription(resolveKnownStaffDescription(code));
        }
    }

    private static String extractStaffCategoryFromDeathRequestId(String requestId) {
        if (requestId == null) {
            return null;
        }
        String[] parts = requestId.split("/");
        return parts.length > 3 && "DDF".equalsIgnoreCase(parts[1]) ? parts[3] : null;
    }

    private static String resolveKnownStaffDescription(String code) {
        return switch (code) {
            case "NS" -> "Normal Staff";
            case "EX-OP1" -> "Executive Staff - Option 01";
            case "EX-OP2" -> "Executive Staff - Option 02";
            case "EX-OP3" -> "Executive Staff - Option 03";
            case "MM" -> "Middle Management level Staff - Option 03";
            case "SNR" -> "Senior Staff";
            default -> code;
        };
    }
}
