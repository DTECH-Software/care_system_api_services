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
import com.dtech.auth.enums.DependentCategory;
import com.dtech.auth.enums.RelationCategory;
import com.dtech.auth.enums.Status;
import com.dtech.auth.enums.Workflow;
import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.ClaimsDependents;
import com.dtech.auth.model.Document;
import com.dtech.auth.repository.ApplicationUserRepository;
import com.dtech.auth.repository.ClaimDependentsRepository;
import com.dtech.auth.repository.DocumentRepository;
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

import java.util.*;
import java.util.stream.Collectors;

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
    private final DocumentRepository documentRepository;

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

                        for (DependentDetailsRequestDTO detailsRequestDTO : dependentRequestDTO.getDependents()) {

                            if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.MOTHER.name())) {
                                List<ClaimsDependents> claimsDependents = claimDependentsRepository
                                        .findAllByApplicationUserAndRelationCategoryAndStatusIn(applicationUser,
                                                RelationCategory.MOTHER, Arrays.asList(Workflow.ACTIVE, Workflow.UNDER_REVIEW));

                                if (!claimsDependents.isEmpty()) {
                                    log.info("User profile add dependent request already active mother {} ", claimsDependents);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_MOTHER_FOUND, new Object[]{clientMobile}, locale)));
                                }

                            } else if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.FATHER.name())) {
                                List<ClaimsDependents> claimsDependents = claimDependentsRepository.findAllByApplicationUserAndRelationCategoryAndStatusIn(applicationUser,
                                        RelationCategory.FATHER, Arrays.asList(Workflow.ACTIVE, Workflow.UNDER_REVIEW));
                                if (!claimsDependents.isEmpty()) {
                                    log.info("User profile add dependent request already active father {} ", claimsDependents);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_FATHER_FOUND, new Object[]{clientMobile}, locale)));
                                }
                            }

                            if (detailsRequestDTO.getDependentCategory().equalsIgnoreCase(DependentCategory.WIFE.name())) {
                                if (detailsRequestDTO.getDocuments().size() != 2) {
                                    log.info("User profile add dependent request out of wife document {} ", dependentRequestDTO);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1023, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_WIFE_DOCUMENT_IS_EMPTY_OR_OUT_OF_RANGE, new Object[]{detailsRequestDTO.getFirstName()}, locale)));

                                }
                            } else if (detailsRequestDTO.getDependentCategory().equalsIgnoreCase(DependentCategory.PARENTS.name())
                                    || detailsRequestDTO.getDependentCategory().equalsIgnoreCase(DependentCategory.CHILDREN.name())) {
                                if (detailsRequestDTO.getDocuments().size() != 1) {
                                    log.info("User profile add dependent request out of parent or child document {} ", dependentRequestDTO);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1023, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_OTHER_RELATION_CATEGORY_DOCUMENT_IS_EMPTY_OR_OUT_OF_RANGE, new Object[]{detailsRequestDTO.getFirstName()}, locale)));

                                }
                            }

                        }
                        saveClaimDependent(dependentRequestDTO.getDependents(), applicationUser);
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
    protected void saveClaimDependent(List<DependentDetailsRequestDTO> dependents, ApplicationUser applicationUser) {
        try {
            log.info("User claim dependent save {} ", dependents);
            dependents.forEach(dependentDetailsRequestDTO -> {
                ClaimsDependents claimsDependents = gson.fromJson(gson.toJson(dependentDetailsRequestDTO), ClaimsDependents.class);
                claimsDependents.setStatus(Workflow.UNDER_REVIEW);
                claimsDependents.setApplicationUser(applicationUser);
                claimsDependents.setDocuments(
                        saveClaimDocument(dependentDetailsRequestDTO.getDocuments())
                );
                log.info("User claim dependent attachment  save {} ", dependentDetailsRequestDTO);
                claimDependentsRepository.saveAndFlush(claimsDependents);
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected Set<Document> saveClaimDocument(List<SupportingDocumentDTO> supportingDocumentDTO) {
        try {
            log.info("User dependent document save {} ", supportingDocumentDTO);

            return supportingDocumentDTO.stream().map(val -> documentRepository.findById(Long.valueOf(val.getId()))
                    .orElseThrow(() -> new RuntimeException("Document not found with id " + val.getId()))).collect(Collectors.toSet());

        } catch (Exception e) {
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
