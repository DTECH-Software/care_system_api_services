/**
 * User: Himal_J
 * Date: 2/28/2025
 * Time: 12:37 PM
 * <p>
 */

package com.dtech.auth.mapper.DtoToEntity;


import com.dtech.auth.dto.request.ClaimDependentDetailsRequestDTO;
import com.dtech.auth.dto.response.ApplicationUserDetailsResponseDTO;
import com.dtech.auth.dto.response.ClaimDependentDetailsResponseDTO;
import com.dtech.auth.dto.response.DocumentDownloadResponseDTO;
import com.dtech.auth.enums.Gender;
import com.dtech.auth.enums.Title;
import com.dtech.auth.enums.Workflow;
import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.ClaimsDependents;
import com.dtech.auth.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.dtech.auth.util.StringUtil.ifNotOrEmpty;

@Log4j2
@Service
@RequiredArgsConstructor
public class DependenceMapper {

    private static final ModelMapper modelMapper = new ModelMapper();

    public static ClaimsDependents mapDependence(ClaimDependentDetailsRequestDTO claimDependentDetailsRequestDTO) {
        try {
            log.info("application dependence mapper");
            ClaimsDependents claimsDependents = modelMapper.map(claimDependentDetailsRequestDTO, ClaimsDependents.class);
            claimsDependents.setStatus(Workflow.UNDER_REVIEW);
            log.info("Success dependence mapper {} ", claimDependentDetailsRequestDTO);
            return claimsDependents ;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}
