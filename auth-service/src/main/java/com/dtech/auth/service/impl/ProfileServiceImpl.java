/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 2:07 PM
 * <p>
 */

package com.dtech.auth.service.impl;

import com.dtech.auth.dto.request.*;
import com.dtech.auth.dto.response.*;
import com.dtech.auth.enums.*;
import com.dtech.auth.feign.DocumentFeignClient;
import com.dtech.auth.feign.MessageFeignClient;
import com.dtech.auth.mapper.DtoToEntity.DependenceMapper;
import com.dtech.auth.mapper.EntityToDto.ProfileMapper;
import com.dtech.auth.model.*;
import com.dtech.auth.repository.*;
import com.dtech.auth.service.ProfileService;
import com.dtech.auth.util.*;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final ClaimDependentsRepository claimDependentsRepository;

    @Value("${client.mobile}")
    private String clientMobile;

    @Autowired
    private final Gson gson;

    @Autowired
    private final DocumentFeignClient documentFeignClient;

    @Autowired
    private final ApplicationPasswordPolicyRepository applicationPasswordPolicyRepository;

    @Autowired
    private final ApplicationOtpSessionRepository applicationOtpSessionRepository;

    @Autowired
    private final MessageFeignClient messageFeignClient;

    @Autowired
    private final MarriedRepository marriedRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> profile(ChannelRequestDTO channelRequestDTO, Locale locale) {

        try {
            log.info("User profile request {} ", channelRequestDTO);
            String username = channelRequestDTO.getUsername().trim();

            Optional<ApplicationUser> optionalUser = applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(username, Status.ACTIVE);

            if (optionalUser.isEmpty()) {
                log.info("Login request find by email {} ", username);
                optionalUser = applicationUserRepository.findByPrimaryEmailIgnoreCaseAndUserPersonalDetails_UserStatus(username, Status.ACTIVE);
                channelRequestDTO.setUsername(optionalUser.map(ApplicationUser::getUsername).orElse(""));
            }

            return optionalUser.map((ap) -> {
                log.info("User profile request user found {} ", ap);
                ProfileMapper dependenceMapper = new ProfileMapper();
                ApplicationUserDetailsResponseDTO applicationUserDetailsResponseDTO = dependenceMapper.mapApplicationUser(ap);
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
    public ResponseEntity<ApiResponse<Object>> addDependents(ClaimDependentRequestDTO claimDependentRequestDTO, Locale locale) {
        try {
            log.info("User profile add dependant request {} ", claimDependentRequestDTO);
            return applicationUserRepository.
                    findByUsernameAndUserPersonalDetails_UserStatus(claimDependentRequestDTO.getUsername().trim(), Status.ACTIVE)
                    .map(applicationUser -> {

                        for (ClaimDependentDetailsRequestDTO detailsRequestDTO : claimDependentRequestDTO.getDependents()) {

                            if (!applicationUser.getUserPersonalDetails().isMaritalStatus()) {
                                if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.WIFE.name())
                                        || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.HUSBAND.name())
                                        || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.FATHER_IN_LAW.name())
                                        || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.MOTHER_IN_LAW.name())
                                ) {
                                    log.info("User not eligible add wife or husband {} ", detailsRequestDTO.getRelationCategory());
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1042, messageSource.getMessage(ResponseMessageUtil.USER_NOT_ELIGIBLE_WIFE_OR_HUSBAND_DEPENDENTS, new Object[]{clientMobile}, locale)));
                                }
                            } else if (applicationUser.getUserPersonalDetails().getGender().equals(Gender.MALE) &&
                                    detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.HUSBAND.name())) {
                                log.info("Can't relation husband {} ", detailsRequestDTO.getRelationCategory());
                                return ResponseEntity.ok().body(responseUtil.error(null, 1041, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_HUSBAND_CANT_ADDED, new Object[]{clientMobile}, locale)));

                            } else if (applicationUser.getUserPersonalDetails().getGender().equals(Gender.FEMALE) &&
                                    detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.WIFE.name())) {
                                log.info("Can't relation wife {} ", detailsRequestDTO.getRelationCategory());
                                return ResponseEntity.ok().body(responseUtil.error(null, 1041, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_WIFE_CANT_ADDED, new Object[]{clientMobile}, locale)));

                            } else if ((detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.HUSBAND.name())
                                    || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.FATHER.name())
                                    || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.BROTHER.name())
                                    || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.FATHER_IN_LAW.name())) && detailsRequestDTO.getGender().equalsIgnoreCase(Gender.FEMALE.name())
                            ) {
                                log.info("Gender is not male correct {}", detailsRequestDTO.getFirstName());
                                return ResponseEntity.ok().body(responseUtil.error(null, 1042, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_GENDER_INCORRECT, new Object[]{clientMobile}, locale)));

                            } else if ((detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.WIFE.name())
                                    || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.MOTHER.name())
                                    || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.SISTER.name())
                                    || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.MOTHER_IN_LAW.name())) && detailsRequestDTO.getGender().equalsIgnoreCase(Gender.MALE.name())
                            ) {
                                log.info("Gender is not female correct {}", detailsRequestDTO.getFirstName());
                                return ResponseEntity.ok().body(responseUtil.error(null, 1042, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_GENDER_INCORRECT, new Object[]{clientMobile}, locale)));

                            }

                            if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.MOTHER.name())) {
                                boolean claimsDependents = claimDependentsRepository
                                        .existsAllByApplicationUserAndRelationCategoryAndStatusIn(applicationUser,
                                                RelationCategory.MOTHER, Arrays.asList(Workflow.ACTIVE, Workflow.UNDER_REVIEW));

                                if (claimsDependents) {
                                    log.info("User profile add dependent request already active mother {} ", claimsDependents);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_MOTHER_FOUND, new Object[]{clientMobile}, locale)));
                                }

                            } else if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.FATHER.name())) {
                                boolean claimsDependents = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusIn(applicationUser,
                                        RelationCategory.FATHER, Arrays.asList(Workflow.ACTIVE, Workflow.UNDER_REVIEW));
                                if (claimsDependents) {
                                    log.info("User profile add dependent request already active father {} ", claimsDependents);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_FATHER_FOUND, new Object[]{clientMobile}, locale)));
                                }
                            } else if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.WIFE.name())) {

                                boolean existed = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusInAndMarried_Id(applicationUser,
                                        RelationCategory.WIFE, Arrays.asList(Workflow.ACTIVE, Workflow.UNDER_REVIEW), Long.valueOf(detailsRequestDTO.getMarried()));
                                if (existed) {
                                    log.info("User profile add dependent request already married round wife {} ", existed);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_WIFE_MARRIED_ROUND_ALREADY_FOUND, new Object[]{clientMobile}, locale)));
                                }
                            } else if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.HUSBAND.name())) {

                                boolean existed = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusInAndMarried_Id(applicationUser,
                                        RelationCategory.HUSBAND, Arrays.asList(Workflow.ACTIVE, Workflow.UNDER_REVIEW), Long.valueOf(detailsRequestDTO.getMarried()));
                                if (existed) {
                                    log.info("User profile add dependent request already married round husband {} ", existed);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_HUSBAND_MARRIED_ROUND_ALREADY_FOUND, new Object[]{clientMobile}, locale)));
                                }
                            } else if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.FATHER_IN_LAW.name())) {

                                boolean existed = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusInAndMarried_Id(applicationUser,
                                        RelationCategory.FATHER_IN_LAW, Arrays.asList(Workflow.ACTIVE, Workflow.UNDER_REVIEW), Long.valueOf(detailsRequestDTO.getMarried()));
                                if (existed) {
                                    log.info("User profile add dependent request already married round father in law {} ", existed);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_FATHER_IN_LAW_MARRIED_ROUND_ALREADY_FOUND, new Object[]{clientMobile}, locale)));
                                }
                            } else if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.MOTHER_IN_LAW.name())) {

                                boolean existed = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusInAndMarried_Id(applicationUser,
                                        RelationCategory.MOTHER_IN_LAW, Arrays.asList(Workflow.ACTIVE, Workflow.UNDER_REVIEW), Long.valueOf(detailsRequestDTO.getMarried()));
                                if (existed) {
                                    log.info("User profile add dependent request already married round mother in law {} ", existed);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_MOTHER_IN_LAW_MARRIED_ROUND_ALREADY_FOUND, new Object[]{clientMobile}, locale)));
                                }
                            }

                            if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.WIFE.name())
                                    || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.HUSBAND.name())) {

                                if (detailsRequestDTO.getDocuments().size() != 2) {
                                    log.info("User profile add dependent request out of wife and husband document {} ", claimDependentRequestDTO);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1023, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_WIFE_DOCUMENT_IS_EMPTY_OR_OUT_OF_RANGE, new Object[]{detailsRequestDTO.getFirstName()}, locale)));

                                } else {

                                    boolean birth = detailsRequestDTO.getDocuments().stream().anyMatch(val -> {
                                        return val.getType().equals(DependentImageTypes.BIRTH.name());
                                    });

                                    boolean married = detailsRequestDTO.getDocuments().stream().anyMatch(val -> {
                                        return val.getType().equals(DependentImageTypes.MARRIED.name());
                                    });

                                    if (!birth && !married) {
                                        log.info("Birth and married certificate missing");
                                        return ResponseEntity.ok().body(responseUtil.error(null, 1040, messageSource.getMessage(ResponseMessageUtil.BIRTH_MARRIED_CERTIFICATE_MISSING, new Object[]{detailsRequestDTO.getFirstName()}, locale)));
                                    } else if (!birth) {
                                        log.info("Birth certificate missing");
                                        return ResponseEntity.ok().body(responseUtil.error(null, 1040, messageSource.getMessage(ResponseMessageUtil.BIRTH_CERTIFICATE_MISSING, new Object[]{detailsRequestDTO.getFirstName()}, locale)));
                                    } else if (!married) {
                                        log.info("Married certificate missing");
                                        return ResponseEntity.ok().body(responseUtil.error(null, 1040, messageSource.getMessage(ResponseMessageUtil.MARRIED_CERTIFICATE_MISSING, new Object[]{detailsRequestDTO.getFirstName()}, locale)));
                                    }
                                }
                            } else if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.MOTHER.name())
                                    || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.FATHER.name())
                                    || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.CHILD.name())
                                    || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.BROTHER.name())
                                    || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.SISTER.name())) {

                                if (detailsRequestDTO.getDocuments().size() != 1) {
                                    log.info("User profile add dependent request out of parent or child document {} ", claimDependentRequestDTO);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1023, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_OTHER_RELATION_CATEGORY_DOCUMENT_IS_EMPTY_OR_OUT_OF_RANGE, new Object[]{detailsRequestDTO.getFirstName()}, locale)));
                                } else {
                                    boolean birth = detailsRequestDTO.getDocuments().stream().anyMatch(val -> {
                                        return val.getType().equals(DependentImageTypes.BIRTH.name());
                                    });
                                    if (!birth) {
                                        log.info("Birth certificate missing");
                                        return ResponseEntity.ok().body(responseUtil.error(null, 1040, messageSource.getMessage(ResponseMessageUtil.BIRTH_CERTIFICATE_MISSING, new Object[]{detailsRequestDTO.getFirstName()}, locale)));
                                    }
                                }
                            }
                        }
                        saveDependent(claimDependentRequestDTO.getDependents(), applicationUser, locale);
                        return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_ADDED_SUCCESS, null, locale)));
                    })
                    .orElseGet(() -> {
                        log.info("User profile add dependant request application user not found {} ", claimDependentRequestDTO);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }

    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> getDependentsDetails(ChannelRequestDTO channelRequestDTO, Locale locale) {

        try {
            log.info("User profile view dependent request {} ", channelRequestDTO);
            return applicationUserRepository
                    .findByUsernameAndUserPersonalDetails_UserStatus(channelRequestDTO.getUsername(), Status.ACTIVE)
                    .map((user) -> {
                        log.info("User profile view dependent available {} ", user);
                        List<ClaimsDependents> collect = user.getClaimsDependents().stream().sorted(
                                (c1, c2) ->
                                        c2.getLastModifiedDate().compareTo(c1.getLastModifiedDate())).collect(Collectors.toList());
                        List<ClaimDependentDetailsResponseDTO> claimDependentDetailsResponseDTOS = ProfileMapper
                                .mapDependentList(collect);
                        ClaimDependentResponseDTO dependentResponseDTO = new ClaimDependentResponseDTO();
                        dependentResponseDTO.setDependents(claimDependentDetailsResponseDTOS);
                        return ResponseEntity.ok().body(responseUtil.success((Object) dependentResponseDTO, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_LIST_VIEW_SUCCESS, null, locale)));
                    }).orElseGet(() -> {
                        log.info("User profile view dependent request application user not found {} ", channelRequestDTO.getUsername());
                        return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> updateProfileImage(ProfileImageUpdateRequestDTO profileImageUpdateRequestDTO, Locale locale) {

        try {
            log.info("User profile update request {} ", profileImageUpdateRequestDTO);
            return applicationUserRepository.
                    findByUsernameAndUserPersonalDetails_UserStatus(profileImageUpdateRequestDTO.getUsername(), Status.ACTIVE).map((user) -> {

                        try {
                            Document uploadedDocument = uploadImage(profileImageUpdateRequestDTO.getType(), profileImageUpdateRequestDTO.getFile(), profileImageUpdateRequestDTO.getFileType(), profileImageUpdateRequestDTO.getFileName());
                            if (uploadedDocument == null) {
                                log.info("User profile image upload failed");
                                return ResponseEntity.ok().body(responseUtil.error(null, 1039, messageSource.getMessage(ResponseMessageUtil.PROFILE_IMAGE_UPLOAD_FAILED, null, locale)));
                            }
                            user.setProfileImg(uploadedDocument);
                            log.info("set image to profile image");
                            applicationUserRepository.saveAndFlush(user);
                            log.info("User profile update successful {} ", user.getUsername());
                            DocumentDownloadResponseDTO documentDownloadResponseDTO = gson.fromJson(gson.toJson(uploadedDocument), DocumentDownloadResponseDTO.class);
                            log.info("User profile update profile load success {} ", user.getUsername());
                            return ResponseEntity.ok().body(responseUtil.success((Object) documentDownloadResponseDTO, messageSource.getMessage(ResponseMessageUtil.PROFILE_IMAGE_UPDATE_SUCCESS, null, locale)));
                        } catch (Exception e) {
                            log.error(e);
                            throw new RuntimeException(e);
                        }

                    }).orElseGet(() -> {
                        log.info("User profile update request application user not found {} ", profileImageUpdateRequestDTO.getUsername());
                        return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
                    });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    protected Document uploadImage(String tye, String file, String fileType, String fileName) throws IOException {
        try {
            log.info("Upload profile image");
            MultipartFile multipartFile = MultipartFileUtil.convertToMultipartFile(file, fileType, fileName);
            log.info("Before calling document service {}", documentFeignClient);
            ResponseEntity<ApiResponse<Object>> documentResponse = documentFeignClient.upload(tye, multipartFile);
            log.info("After response document service {}", documentResponse);
            Object objectApiResponse = ExtractApiResponseUtil.extractApiResponse(documentResponse);
            log.info("After document mapper response {}", objectApiResponse);
            Document document = gson.fromJson(gson.toJson(objectApiResponse), Document.class);
            log.info("image upload success {}", document);
            return document;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> updateProfileDetails(ProfileEditRequestDTO profileEditRequestDTO, Locale locale) {
        try {
            log.info("User profile update details request {} ", profileEditRequestDTO);
            String username = profileEditRequestDTO.getUsername().trim();
            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(username, Status.ACTIVE).map(user -> {

                String primaryEmail = profileEditRequestDTO.getPrimaryEmail().trim();
                String primaryMobile = profileEditRequestDTO.getPrimaryMobile().trim();
                if (user.getPrimaryEmail().equalsIgnoreCase(primaryEmail) && user.getPrimaryMobile().equalsIgnoreCase(primaryMobile)) {
                    log.info("User profile update request details not change {} ", profileEditRequestDTO);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1027, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_DETAILS_NOT_CHANGE, null, locale)));
                } else if (user.getApplicationOtpSession() != null) {
                    ApplicationOtpSession applicationOtpSession = user.getApplicationOtpSession();
                    if (!applicationOtpSession.getOtp().equalsIgnoreCase(profileEditRequestDTO.getOtp()) || !applicationOtpSession.isValidated()) {
                        log.info("User profile update request details not change {} ", profileEditRequestDTO);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1028, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_DETAILS_OTP_VERIFICATION_FAILED, null, locale)));
                    }
                }
                updateDetailsApplicationUser(user, primaryEmail, primaryMobile);
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_DETAILS_UPDATE_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("User profile add dependant request application user not found {} ", profileEditRequestDTO);
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateDetailsApplicationUser(ApplicationUser applicationUser, String email, String mobile) {
        try {
            log.info("Update profile details {} ", applicationUser);
            applicationUser.setPrimaryEmail(email);
            applicationUser.setPrimaryMobile(mobile);
            applicationUserRepository.saveAndFlush(applicationUser);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void saveDependent(List<ClaimDependentDetailsRequestDTO> dependents, ApplicationUser applicationUser, Locale locale) {
        try {
            log.info("User claim dependent save {} ", dependents);
            dependents.forEach(claimDependentDetailsRequestDTO -> {

                Married married = null;
                if (claimDependentDetailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.WIFE.name())
                        || claimDependentDetailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.HUSBAND.name())
                        || claimDependentDetailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.CHILD.name())
                        || claimDependentDetailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.FATHER_IN_LAW.name())
                        || claimDependentDetailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.MOTHER_IN_LAW.name())) {

                }
                {
                    married = marriedRepository.findByCodeAndStatus(claimDependentDetailsRequestDTO.getMarried(),Status.ACTIVE).map(ma -> {
                        log.info("Married  {}", ma);
                        return ma;
                    }).orElse(null);

                }
                ClaimsDependents claimsDependents = DependenceMapper.mapDependence(claimDependentDetailsRequestDTO);
                boolean isMarried = applicationUser.getUserPersonalDetails().isMaritalStatus();
                String dependentCategory = claimDependentDetailsRequestDTO.getDependentCategory();
                String relationCategory = claimDependentDetailsRequestDTO.getRelationCategory();

                boolean isEligibleForBoth = false;
                if (isMarried) {
                    isEligibleForBoth = DependentCategory.SPOUSE.name().equalsIgnoreCase(dependentCategory) ||
                            DependentCategory.CHILDREN.name().equalsIgnoreCase(dependentCategory);
                } else {
                    isEligibleForBoth = RelationCategory.FATHER.name().equalsIgnoreCase(relationCategory) ||
                            RelationCategory.MOTHER.name().equalsIgnoreCase(relationCategory);
                }

                claimsDependents.setEligibleFacility(isEligibleForBoth ? Facility.BOTH : Facility.DEATH);

               claimsDependents.setApplicationUser(applicationUser);
                if (claimDependentDetailsRequestDTO.getMarried() != null && married != null) {
                    claimsDependents.setMarried(married);
                }
                List<Document> uploadSupportingDocumentFromDependent = claimDependentDetailsRequestDTO.getDocuments().stream().map(doc -> {
                    log.info("Upload supporting document from dependent");
                    try {
                        return uploadImage(doc.getType(), doc.getFile(), doc.getFileType(), doc.getFileName());
                    } catch (IOException e) {
                        log.error(e);
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

                claimsDependents.setDocuments(
                        uploadSupportingDocumentFromDependent
                );
                log.info("User claim dependent attachment  save {} ", claimDependentDetailsRequestDTO);
                claimDependentsRepository.saveAndFlush(claimsDependents);
            });
            applicationUser.setExpectingDependentsRegister(false);
            applicationUserRepository.saveAndFlush(applicationUser);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}
