/**
 * User: Himal_J
 * Date: 2/28/2025
 * Time: 12:37 PM
 * <p>
 */

package com.dtech.auth.mapper.EntityToDto;


import com.dtech.auth.dto.request.DocumentDownloadRequestDTO;
import com.dtech.auth.dto.response.ApplicationUserDetailsResponseDTO;
import com.dtech.auth.dto.response.ClaimDependentDetailsResponseDTO;
import com.dtech.auth.dto.response.DocumentResponseDTO;
import com.dtech.auth.feign.DocumentFeignClient;
import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.ClaimsDependents;
import com.dtech.auth.util.DateTimeUtil;
import com.dtech.auth.util.ExtractApiResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.dtech.auth.util.StringUtil.ifNotOrEmpty;

@Log4j2
@Service
@RequiredArgsConstructor
public class ProfileMapper {

    private static final ModelMapper modelMapper = new ModelMapper();

    public static ApplicationUserDetailsResponseDTO mapApplicationUser(ApplicationUser applicationUser, DocumentFeignClient documentFeignClient) {
        try {
            log.info("application user mapper");
            ApplicationUserDetailsResponseDTO applicationUserDetailsResponseDTO = modelMapper.map(applicationUser, ApplicationUserDetailsResponseDTO.class);
            log.info("application user get age");
            applicationUserDetailsResponseDTO.getUserPersonalDetails().setAge(DateTimeUtil.getAge(
                    String.valueOf(applicationUser.getUserPersonalDetails().getDob())));
            if (applicationUser.getProfileImg() != null) {
                log.info("application user get profile img");
                byte[] documents = getDocuments(applicationUser.getProfileImg().getId(), documentFeignClient);
                applicationUserDetailsResponseDTO.getProfileImg().setDoc(documents);
                log.info("application user get profile img downloaded");
            }

            return applicationUserDetailsResponseDTO;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    public static List<ClaimDependentDetailsResponseDTO> mapDependentList(List<ClaimsDependents> claimsDependentsList, DocumentFeignClient documentFeignClient) {
        try {
            log.info("dependent list mapper");

            return claimsDependentsList.stream().map((dependents -> {
                ClaimDependentDetailsResponseDTO claimDependentDetailsResponseDTO = new ClaimDependentDetailsResponseDTO();
                claimDependentDetailsResponseDTO.setId(Long.valueOf(ifNotOrEmpty(String.valueOf(dependents.getId()))));
                claimDependentDetailsResponseDTO.setDependentCategory(ifNotOrEmpty(String.valueOf(dependents.getDependentCategory())));
                claimDependentDetailsResponseDTO.setDependentCategoryDescription(ifNotOrEmpty(String.valueOf(dependents.getDependentCategory().getDescription())));
                claimDependentDetailsResponseDTO.setInitials(ifNotOrEmpty(dependents.getInitials()));
                claimDependentDetailsResponseDTO.setFirstName(ifNotOrEmpty(dependents.getFirstName()));
                claimDependentDetailsResponseDTO.setLastName(ifNotOrEmpty(dependents.getLastName()));
                claimDependentDetailsResponseDTO.setDob(Date.valueOf(ifNotOrEmpty(String.valueOf(dependents.getDob()))));
                claimDependentDetailsResponseDTO.setGender(ifNotOrEmpty(String.valueOf(dependents.getGender())));
                claimDependentDetailsResponseDTO.setGenderDescription(ifNotOrEmpty(String.valueOf(dependents.getGender().getDescription())));
                claimDependentDetailsResponseDTO.setNic(ifNotOrEmpty(dependents.getNic()));
                claimDependentDetailsResponseDTO.setJobTitle(ifNotOrEmpty(dependents.getJobTitle()));
                claimDependentDetailsResponseDTO.setRelationCategory(ifNotOrEmpty(String.valueOf(dependents.getRelationCategory())));
                claimDependentDetailsResponseDTO.setRelationCategoryDescription(ifNotOrEmpty(String.valueOf(dependents.getRelationCategory().getDescription())));
                claimDependentDetailsResponseDTO.setStatus(ifNotOrEmpty(String.valueOf(dependents.getStatus())));
                claimDependentDetailsResponseDTO.setStatusDescription(ifNotOrEmpty(String.valueOf(dependents.getStatus().getDescription())));
                log.info("claim dependent details call get image method");
                List<DocumentResponseDTO> collect = dependents.getDocuments().stream().map((document -> {
                    log.info("inside document call get document method");
                    byte[] documents = getDocuments(document.getId(), documentFeignClient);
                    DocumentResponseDTO responseDTO = new DocumentResponseDTO();
                    responseDTO.setDoc(documents);
                    responseDTO.setType(document.getType().toString());
                    responseDTO.setFileName(document.getFileName());
                    responseDTO.setFileType(document.getFileType());
                    return responseDTO;
                })).collect(Collectors.toList());
                claimDependentDetailsResponseDTO.setDocuments(collect);
                log.info("claim dependent details call get documents method success map");
                return claimDependentDetailsResponseDTO;
            })).collect(Collectors.toList());
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private static byte[] getDocuments(Long id, DocumentFeignClient documentFeignClient) {
        try {
            log.info("documents download from auth {} ", id);
            id = id != null ? id : 0;
            ResponseEntity<byte[]> image = documentFeignClient.getImage(new DocumentDownloadRequestDTO(id));
            log.info("documents download from auth after response from extract {} ", id);
            byte[] imageResponse = ExtractApiResponseUtil.extractApiImageResponse(image);
            log.info("documents download from auth before extract{} ", id);
            return imageResponse;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
