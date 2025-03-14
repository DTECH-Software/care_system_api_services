/**
 * User: Himal_J
 * Date: 3/14/2025
 * Time: 11:08 AM
 * <p>
 */

package com.dtech.claim.mapper;

import com.dtech.claim.dto.response.ClaimRequestResponseDto;
import com.dtech.claim.model.ClaimsRequest;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;

@Log4j2
public class EntityToDtoMapper {
    private static final ModelMapper modelMapper = new ModelMapper();
    public static ClaimRequestResponseDto mapClaimHistoryDetails(ClaimsRequest claimsRequest) {
        try {
            log.info("Call to mapClaimHistoryDetails method {} ", claimsRequest);
            return modelMapper.map(claimsRequest, ClaimRequestResponseDto.class);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
