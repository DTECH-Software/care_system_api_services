/**
 * User: Himal_J
 * Date: 3/14/2025
 * Time: 11:08 AM
 * <p>
 */

package com.dtech.claim.mapper;

import com.dtech.claim.dto.response.ClaimRequestResponseDto;
import com.dtech.claim.dto.response.DocumentDownloadResponseDTO;
import com.dtech.claim.enums.Workflow;
import com.dtech.claim.model.ClaimsRequest;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class EntityToDtoMapper {
    private static final ModelMapper modelMapper = new ModelMapper();
    public static ClaimRequestResponseDto mapClaimHistoryDetails(ClaimsRequest claimsRequest) {
        try {
            log.info("Call to mapClaimHistoryDetails method {} ", claimsRequest);
            ClaimRequestResponseDto claimRequestResponseDto = modelMapper.map(claimsRequest, ClaimRequestResponseDto.class);
            claimRequestResponseDto.setRequestStatusDescription(Workflow.valueOf(claimRequestResponseDto.getRequestStatus()).getDescription());
            List<DocumentDownloadResponseDTO> collect = claimsRequest.getInsuranceClaimsDetails().getDocuments().stream().map((document -> {
                log.info("inside document mapper {} ",document);
                return new DocumentDownloadResponseDTO(String.valueOf(document.getType()), document.getFileName(), document.getFileType(),document.getDoc());
            })).collect(Collectors.toList());
            claimRequestResponseDto.getInsuranceClaimsDetails().setDocuments(collect);
            return claimRequestResponseDto;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
