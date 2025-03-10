/**
 * User: Himal_J
 * Date: 2/28/2025
 * Time: 12:37 PM
 * <p>
 */

package com.dtech.auth.mapper.EntityToDto;


import com.dtech.auth.dto.response.*;
import com.dtech.auth.enums.Gender;
import com.dtech.auth.enums.Title;
import com.dtech.auth.feign.DocumentFeignClient;
import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.ClaimsDependents;
import com.dtech.auth.util.DateTimeUtil;
import com.google.gson.Gson;
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
public class ProfileMapper {

    private static final ModelMapper modelMapper = new ModelMapper();
    private static final Gson gson = new Gson();

    @Transactional(readOnly = true)
    public ApplicationUserDetailsResponseDTO mapApplicationUser(ApplicationUser applicationUser) {
        try {
            log.info("application user mapper");
            ApplicationUserDetailsResponseDTO applicationUserDetailsResponseDTO = modelMapper.map(applicationUser, ApplicationUserDetailsResponseDTO.class);
            log.info("application user get age");
            applicationUserDetailsResponseDTO.getUserPersonalDetails().setAge(DateTimeUtil.getAge(
                    String.valueOf(applicationUser.getUserPersonalDetails().getDob())));
            applicationUserDetailsResponseDTO.getUserPersonalDetails().setGenderDescription(Gender.valueOf(applicationUserDetailsResponseDTO.getUserPersonalDetails().getGender()).getDescription());
            applicationUserDetailsResponseDTO.getUserPersonalDetails().setTitleDescription(Title.valueOf(applicationUserDetailsResponseDTO.getUserPersonalDetails().getTitle()).getDescription());

            if (applicationUser.getProfileImg() != null) {
                log.info("application user get profile img");
                DocumentDownloadResponseDTO documentDownloadResponseDTO = gson.fromJson(gson.toJson(applicationUser.getProfileImg()), DocumentDownloadResponseDTO.class);
                applicationUserDetailsResponseDTO.setProfileImg(documentDownloadResponseDTO);
                log.info("application user get profile img downloaded");
            }
            log.info("Success profile mapper {} ", applicationUserDetailsResponseDTO);
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
                List<DocumentDownloadResponseDTO> collect = dependents.getDocuments().stream().map((document -> {
                    log.info("inside document mapper");
                    return gson.fromJson(gson.toJson(document), DocumentDownloadResponseDTO.class);
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

//    public static byte[] getDocuments(Long id, DocumentFeignClient documentFeignClient) {
//        try {
//            log.info("documents download from auth {} ", id);
//            id = id != null ? id : 0;
//            ResponseEntity<byte[]> image = documentFeignClient.getImage(new DocumentDownloadRequestDTO(id));
//            log.info("documents download from auth after response from extract {} ", id);
//            byte[] imageResponse = ExtractApiResponseUtil.extractApiImageResponse(image);
//            log.info("documents download from auth before extract{} ", id);
//            return imageResponse;
//        } catch (Exception e) {
//            log.error(e);
//            throw e;
//        }
//    }

//    public static Object getDocuments(Long id, DocumentFeignClient documentFeignClient, boolean state) {
//        try {
//            log.info("documents download from auth {} ", id);
//            id = id != null ? id : 0;
//            ResponseEntity<ApiResponse<Object>> image = documentFeignClient.getImage(new DocumentDownloadRequestDTO(id, state));
//            log.info("documents download from auth after response from extract {} ", id);
//            Object objectApiResponse = ExtractApiResponseUtil.extractApiResponse(image);
//            log.info("After message mapper response {}", objectApiResponse);
//            return objectApiResponse;
//        } catch (Exception e) {
//            log.error(e);
//            throw e;
//        }
//    }
}
