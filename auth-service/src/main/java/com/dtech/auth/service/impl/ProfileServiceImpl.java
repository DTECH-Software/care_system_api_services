/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 2:07 PM
 * <p>
 */

package com.dtech.auth.service.impl;

import com.dtech.auth.dto.request.ChannelRequestDTO;
import com.dtech.auth.dto.request.DependentDetailsRequestDTO;
import com.dtech.auth.dto.request.DependentRequestDTO;
import com.dtech.auth.dto.request.SupportingDocumentDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.dto.response.ApplicationUserDetailsResponseDTO;
import com.dtech.auth.dto.response.UserPersonalDetailsResponseDTO;
import com.dtech.auth.enums.RelationCategory;
import com.dtech.auth.enums.Status;
import com.dtech.auth.enums.Workflow;
import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.ClaimsDependents;
import com.dtech.auth.model.ClaimsDependentsVerificationDocument;
import com.dtech.auth.repository.ApplicationUserRepository;
import com.dtech.auth.repository.ClaimDependentsRepository;
import com.dtech.auth.repository.ClaimsDependentsVerificationDocumentRepository;
import com.dtech.auth.service.ProfileService;
import com.dtech.auth.util.DateTimeUtil;
import com.dtech.auth.util.ResponseMessageUtil;
import com.dtech.auth.util.ResponseUtil;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Log4j2
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final ModelMapper modelMapper;

    @Autowired
    private final ClaimDependentsRepository claimDependentsRepository;

    @Value("${client.mobile}")
    private String clientMobile;

    @Autowired
    private final Gson gson;

    @Autowired
    private final ClaimsDependentsVerificationDocumentRepository claimsDependentsVerificationDocumentRepository;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> profile(ChannelRequestDTO channelRequestDTO, Locale locale) {

        try {
            log.info("User profile request {} ", channelRequestDTO);
            return applicationUserRepository.
                    findByUsernameAndUserPersonalDetails_UserStatus(channelRequestDTO.getUsername().trim(),
                            Status.ACTIVE).map((ap) -> {
                        log.info("User profile request user found {} ", ap);
                        ApplicationUserDetailsResponseDTO applicationUserDetailsResponseDTO = modelMapper.map(ap, ApplicationUserDetailsResponseDTO.class);
                        getAge(applicationUserDetailsResponseDTO.getUserPersonalDetails());
                        if (ap.isExpectingFirstTimeLogging()) {
                            updateApplicationUserDetails(ap);
                        }
                        log.info("User profile request success{} ", applicationUserDetailsResponseDTO);
                        return ResponseEntity.ok().body(responseUtil.success((Object) applicationUserDetailsResponseDTO, messageSource.getMessage(ResponseMessageUtil.APPLICATION_PROFILE_SUCCESS, null, locale)));
                    }).orElseGet(() -> {
                        log.info("User profile request user not found {} ", channelRequestDTO);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> addDependents(DependentRequestDTO dependentRequestDTO, Locale locale) {
        try {
            log.info("User profile add dependant request {} ", dependentRequestDTO);
            return applicationUserRepository.
                    findByUsernameAndUserPersonalDetails_UserStatus(dependentRequestDTO.getUsername().trim(), Status.ACTIVE)
                    .map(applicationUser -> {

                        if (dependentRequestDTO.getDependents()
                                .stream().anyMatch(dependentDetailsRequestDTO ->
                                        dependentDetailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.MOTHER.name()))) {
                            claimDependentsRepository.findByApplicationUserAndRelationCategoryAndStatus(applicationUser, RelationCategory.MOTHER, Status.ACTIVE).map((cd) -> {
                                log.info("User profile add dependent request already active mother {} ", cd);
                                return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_MOTHER_FOUND, new Object[]{clientMobile}, locale)));
                            });
                        } else if (dependentRequestDTO.getDependents()
                                .stream().anyMatch(dependentDetailsRequestDTO ->
                                        dependentDetailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.FATHER.name()))) {
                            claimDependentsRepository.findByApplicationUserAndRelationCategoryAndStatus(applicationUser, RelationCategory.FATHER, Status.ACTIVE).map((cd) -> {
                                log.info("User profile add dependent request already active father {} ", cd);
                                return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_FATHER_FOUND, new Object[]{clientMobile}, locale)));
                            });
                        }
                        saveClaimDependent(dependentRequestDTO.getDependents(),applicationUser);
                        return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_ADDED_SUCCESS, null, locale)));
                    })
                    .orElseGet(() -> {
                        log.info("User profile add dependant request application user not found {} ", dependentRequestDTO);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }

    }

    @Transactional
    protected void saveClaimDependent(List<DependentDetailsRequestDTO> dependents,ApplicationUser applicationUser) {
        try {
            log.info("User claim dependent save {} ", dependents);
            dependents.forEach(dependentDetailsRequestDTO -> {
                ClaimsDependents claimsDependents = new ClaimsDependents();
                gson.fromJson(gson.toJson(dependentDetailsRequestDTO), claimsDependents.getClass());
                claimsDependents.setStatus(Workflow.UNDER_REVIEW);
                claimsDependents.setApplicationUser(applicationUser);
                claimsDependents.setClaimsDependentsVerificationDocuments(saveClaimDocument(dependentDetailsRequestDTO.getDocument()));
                log.info("User claim dependent attachment  save {} ", dependentDetailsRequestDTO);
                claimDependentsRepository.saveAndFlush(claimsDependents);
            });
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected List<ClaimsDependentsVerificationDocument> saveClaimDocument(List<SupportingDocumentDTO> supportingDocumentDTO) {
        try {
            log.info("User dependent document save {} ", supportingDocumentDTO);
           return supportingDocumentDTO.stream().map(val -> {
                ClaimsDependentsVerificationDocument claimsDependentsVerificationDocument = modelMapper.map(val, ClaimsDependentsVerificationDocument.class);
                return claimsDependentsVerificationDocumentRepository.saveAndFlush(claimsDependentsVerificationDocument);
            }).toList();
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateApplicationUserDetails(ApplicationUser applicationUser) {
        try {
            log.info("User profile request update user details {} ", applicationUser);
            applicationUser.setExpectingFirstTimeLogging(false);
            applicationUserRepository.saveAndFlush(applicationUser);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    protected void getAge(UserPersonalDetailsResponseDTO userPersonalDetailsResponseDTO) {
        try {
            log.info("Processing getAge {}", userPersonalDetailsResponseDTO);
            userPersonalDetailsResponseDTO.setAge(DateTimeUtil.getAge(
                    String.valueOf(userPersonalDetailsResponseDTO.getDob())));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
