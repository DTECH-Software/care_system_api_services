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
import com.dtech.auth.enums.MaritalStatus;
import com.dtech.auth.feign.DocumentFeignClient;
import com.dtech.auth.mapper.DtoToEntity.DependenceMapper;
import com.dtech.auth.mapper.EntityToDto.ProfileMapper;
import com.dtech.auth.model.*;
import com.dtech.auth.model.DocumentStore;
import com.dtech.auth.repository.*;
import com.dtech.auth.service.EmailNotificationService;
import com.dtech.auth.service.ProfileService;
import com.dtech.auth.util.*;
import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Log4j2
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final List<String> HR_TEAM_ROLE_CODES = List.of(
            "HRADMIN"
    );

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
    private final MarriedRepository marriedRepository;

    @Autowired
    private final NotificationHistoryRepository notificationHistoryRepository;

    @Autowired
    private final DocumentStoreRepository documentStoreRepository;

    @Autowired
    private final DocumentRepository documentRepository;

    @Autowired
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    private final EmailNotificationService emailNotificationService;


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
                long unreadCount = notificationHistoryRepository.countByTypeAndEmployeeAndIsRead(NotificationsType.IN_APP_NOTIFICATION, ap, false);
                Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Order.asc("lastModifiedDate")));
                List<NotificationHistory> notificationHistories = notificationHistoryRepository.findAllByTypeAndEmployeeOrderByLastModifiedByDesc(NotificationsType.IN_APP_NOTIFICATION, ap, pageable);
                ProfileMapper profileMapper = new ProfileMapper();
                ApplicationUserDetailsResponseDTO applicationUserDetailsResponseDTO = profileMapper.mapApplicationUser(ap, unreadCount, notificationHistories);
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

                            if (applicationUser.getUserPersonalDetails().getMaritalStatus().equals(MaritalStatus.UNMARRIED)) {
                                if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.WIFE.name())
                                        || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.HUSBAND.name())
                                        || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.FATHER_IN_LAW.name())
                                        || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.MOTHER_IN_LAW.name())
                                        || detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.CHILD.name())
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
                                                RelationCategory.MOTHER, Arrays.asList(Workflow.APPROVED, Workflow.UNDER_REVIEW));

                                if (claimsDependents) {
                                    log.info("User profile add dependent request already active mother {} ", claimsDependents);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_MOTHER_FOUND, new Object[]{clientMobile}, locale)));
                                }

                            } else if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.FATHER.name())) {
                                boolean claimsDependents = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusIn(applicationUser,
                                        RelationCategory.FATHER, Arrays.asList(Workflow.APPROVED, Workflow.UNDER_REVIEW));
                                if (claimsDependents) {
                                    log.info("User profile add dependent request already active father {} ", claimsDependents);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_FATHER_FOUND, new Object[]{clientMobile}, locale)));
                                }
                            } else if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.WIFE.name())) {

                                if (hasActiveOrPendingSpouse(applicationUser)) {
                                    log.info("User profile add dependent request already has active spouse");
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_WIFE_MARRIED_ROUND_ALREADY_FOUND, new Object[]{clientMobile}, locale)));
                                }

                                boolean existed = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusInAndMarried_Id(applicationUser,
                                        RelationCategory.WIFE, Arrays.asList(Workflow.APPROVED, Workflow.UNDER_REVIEW), Long.valueOf(detailsRequestDTO.getMarried()));
                                if (existed) {
                                    log.info("User profile add dependent request already married round wife {} ", existed);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_WIFE_MARRIED_ROUND_ALREADY_FOUND, new Object[]{clientMobile}, locale)));
                                }
                            } else if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.HUSBAND.name())) {

                                if (hasActiveOrPendingSpouse(applicationUser)) {
                                    log.info("User profile add dependent request already has active spouse");
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_HUSBAND_MARRIED_ROUND_ALREADY_FOUND, new Object[]{clientMobile}, locale)));
                                }

                                boolean existed = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusInAndMarried_Id(applicationUser,
                                        RelationCategory.HUSBAND, Arrays.asList(Workflow.APPROVED, Workflow.UNDER_REVIEW), Long.valueOf(detailsRequestDTO.getMarried()));
                                if (existed) {
                                    log.info("User profile add dependent request already married round husband {} ", existed);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_HUSBAND_MARRIED_ROUND_ALREADY_FOUND, new Object[]{clientMobile}, locale)));
                                }
                            } else if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.FATHER_IN_LAW.name())) {

                                boolean existed = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusInAndMarried_Id(applicationUser,
                                        RelationCategory.FATHER_IN_LAW, Arrays.asList(Workflow.APPROVED, Workflow.UNDER_REVIEW), Long.valueOf(detailsRequestDTO.getMarried()));
                                if (existed) {
                                    log.info("User profile add dependent request already married round father in law {} ", existed);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_FATHER_IN_LAW_MARRIED_ROUND_ALREADY_FOUND, new Object[]{clientMobile}, locale)));
                                }
                            } else if (detailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.MOTHER_IN_LAW.name())) {

                                boolean existed = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusInAndMarried_Id(applicationUser,
                                        RelationCategory.MOTHER_IN_LAW, Arrays.asList(Workflow.APPROVED, Workflow.UNDER_REVIEW), Long.valueOf(detailsRequestDTO.getMarried()));
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

                                    boolean birth = detailsRequestDTO.getDocuments().stream().anyMatch(val -> val.getType().equals(DependentImageTypes.BIRTH.name()));

                                    boolean married = detailsRequestDTO.getDocuments().stream().anyMatch(val -> val.getType().equals(DependentImageTypes.MARRIED.name()));

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
                                    boolean birth = detailsRequestDTO.getDocuments().stream().anyMatch(val -> val.getType().equals(DependentImageTypes.BIRTH.name()));
                                    if (!birth) {
                                        log.info("Birth certificate missing");
                                        return ResponseEntity.ok().body(responseUtil.error(null, 1040, messageSource.getMessage(ResponseMessageUtil.BIRTH_CERTIFICATE_MISSING, new Object[]{detailsRequestDTO.getFirstName()}, locale)));
                                    }
                                }
                            }
                        }
                        List<ClaimsDependents> savedDependents = saveDependent(claimDependentRequestDTO.getDependents(), applicationUser, locale);
                        notifyHrTeamOnDependentPendingApproval(applicationUser, savedDependents);
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

                            if(profileImageUpdateRequestDTO.getType().equals(ProfileImageTypes.PROFILE.name())) {
                                user.setProfileImg(uploadedDocument);
                                log.info("set image to profile image");
                            }else {
                                user.getUserPersonalDetails().setBirthImg(uploadedDocument);
                                log.info("set image to birth image");
                            }

                            log.info("set image");
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
            if (applicationUser.getUserPersonalDetails() != null) {
                applicationUser.getUserPersonalDetails().setEmail(email);
                applicationUser.getUserPersonalDetails().setMobileNo(mobile);
            }
            applicationUserRepository.saveAndFlush(applicationUser);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected List<ClaimsDependents> saveDependent(List<ClaimDependentDetailsRequestDTO> dependents, ApplicationUser applicationUser, Locale locale) {
        try {
            log.info("User claim dependent save {} ", dependents);
            List<ClaimsDependents> savedDependents = new ArrayList<>();
            dependents.forEach(claimDependentDetailsRequestDTO -> {

                Married married = null;
                if (claimDependentDetailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.WIFE.name())
                        || claimDependentDetailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.HUSBAND.name())
                        || claimDependentDetailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.CHILD.name())
                        || claimDependentDetailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.FATHER_IN_LAW.name())
                        || claimDependentDetailsRequestDTO.getRelationCategory().equalsIgnoreCase(RelationCategory.MOTHER_IN_LAW.name())) {

                }
                {
                    married = marriedRepository.findByCodeAndStatus(claimDependentDetailsRequestDTO.getMarried(), Status.ACTIVE).map(ma -> {
                        log.info("Married  {}", ma);
                        return ma;
                    }).orElse(null);

                }
                ClaimsDependents claimsDependents = DependenceMapper.mapDependence(claimDependentDetailsRequestDTO);
               // boolean isMarried = applicationUser.getUserPersonalDetails().();
                String dependentCategory = claimDependentDetailsRequestDTO.getDependentCategory();
                String relationCategory = claimDependentDetailsRequestDTO.getRelationCategory();

                boolean isEligibleForBoth = false;
                if (applicationUser.getUserPersonalDetails().getMaritalStatus().equals(MaritalStatus.MARRIED)) {
                    isEligibleForBoth = DependentCategory.SPOUSE.name().equalsIgnoreCase(dependentCategory) ||
                            DependentCategory.CHILDREN.name().equalsIgnoreCase(dependentCategory);
                } else {
                    isEligibleForBoth = RelationCategory.FATHER.name().equalsIgnoreCase(relationCategory) ||
                            RelationCategory.MOTHER.name().equalsIgnoreCase(relationCategory);
                }

                claimsDependents.setEligibleFacility(isEligibleForBoth ? Facility.BOTH : Facility.DEATH);
                claimsDependents.setLiveStatus(true);

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
                savedDependents.add(claimsDependents);
            });
            applicationUser.setExpectingDependentsRegister(false);
            applicationUserRepository.saveAndFlush(applicationUser);
            return savedDependents;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private void notifyHrTeamOnDependentPendingApproval(ApplicationUser applicationUser, List<ClaimsDependents> savedDependents) {
        String companyCode = applicationUser != null
                && applicationUser.getUserPersonalDetails() != null
                && applicationUser.getUserPersonalDetails().getUserCompanyDetails() != null
                && applicationUser.getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes() != null
                ? applicationUser.getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode()
                : null;

        if (companyCode == null || savedDependents == null || savedDependents.isEmpty()) {
            log.info("Skipping dependent pending approval email. Company or dependent data missing");
            return;
        }

        List<String> recipientEmails = findHrTeamEmailsByCompany(companyCode);
        emailNotificationService.notifyHrTeamOnDependentPendingApproval(recipientEmails, applicationUser, savedDependents);
    }

    private List<String> findHrTeamEmailsByCompany(String companyCode) {
        if (companyCode == null || companyCode.isBlank()) {
            return List.of();
        }

        String placeholders = String.join(",", Collections.nCopies(HR_TEAM_ROLE_CODES.size(), "?"));
        String sql = """
                SELECT DISTINCT wu.email
                FROM web_user wu
                JOIN web_user_role wur ON wu.user_role = wur.code
                JOIN web_user_company wuc ON wu.id = wuc.web_user_id
                JOIN company_types ct ON wuc.company_id = ct.id
                WHERE wu.status = 'ACTIVE'
                  AND wur.status = 'ACTIVE'
                  AND ct.status = 'ACTIVE'
                  AND ct.code = ?
                  AND UPPER(wur.code) IN (%s)
                  AND wu.email IS NOT NULL
                  AND TRIM(wu.email) <> ''
                """.formatted(placeholders);

        List<Object> params = new ArrayList<>();
        params.add(companyCode);
        HR_TEAM_ROLE_CODES.forEach(role -> params.add(role.toUpperCase(Locale.ROOT)));

        return jdbcTemplate.query(
                sql,
                params.toArray(),
                (rs, rowNum) -> rs.getString("email")
        );
    }

    private void notifyHrTeamOnCivilStatusPendingApproval(ApplicationUser applicationUser,
                                                          MaritalStatusRequestDTO maritalStatusRequestDTO) {
        if (applicationUser == null
                || maritalStatusRequestDTO == null
                || !RequestType.MARRIED.name().equalsIgnoreCase(maritalStatusRequestDTO.getRequestType())) {
            return;
        }

        String companyCode = applicationUser.getUserPersonalDetails() != null
                && applicationUser.getUserPersonalDetails().getUserCompanyDetails() != null
                && applicationUser.getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes() != null
                ? applicationUser.getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode()
                : null;

        if (companyCode == null || companyCode.isBlank()) {
            log.info("Skipping civil status pending approval email. Company missing");
            return;
        }

        List<String> recipientEmails = findHrTeamEmailsByCompany(companyCode);
        emailNotificationService.notifyHrTeamOnCivilStatusPendingApproval(
                recipientEmails,
                applicationUser,
                getMarriageRelatedDependents(applicationUser)
        );
    }

    private List<ClaimsDependents> getMarriageRelatedDependents(ApplicationUser applicationUser) {
        if (applicationUser == null || applicationUser.getClaimsDependents() == null) {
            return List.of();
        }

        Set<RelationCategory> relationCategories = EnumSet.of(
                RelationCategory.WIFE,
                RelationCategory.HUSBAND,
                RelationCategory.FATHER_IN_LAW,
                RelationCategory.MOTHER_IN_LAW
        );

        return applicationUser.getClaimsDependents().stream()
                .filter(Objects::nonNull)
                .filter(dependent -> Boolean.TRUE.equals(dependent.getLiveStatus()))
                .filter(dependent -> dependent.getRelationCategory() != null)
                .filter(dependent -> relationCategories.contains(dependent.getRelationCategory()))
                .sorted(Comparator
                        .comparing((ClaimsDependents dependent) -> dependent.getRelationCategory().ordinal())
                        .thenComparing(ClaimsDependents::getDob, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> policyDocument(PolicyDocumentRequestDTO policyDocumentRequestDTO) {
        log.info("Sample document download request: {}", policyDocumentRequestDTO);

        Optional<ApplicationUser> user = applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(policyDocumentRequestDTO.getUsername(), Status.ACTIVE);

        StaffCategories staffCategories = user.get().getUserPersonalDetails().getUserCompanyDetails().getStaffCategories();

        String req = policyDocumentRequestDTO.getPolicy() ? com.dtech.auth.enums.DocumentStore.POLICY_INS.name().concat(staffCategories.getCode()) :
                com.dtech.auth.enums.DocumentStore.POLICY_DDF.name().concat(staffCategories.getCode());
        Optional<DocumentStore> documentStoreOpt = documentStoreRepository.findByCode(req);

        if (documentStoreOpt.isEmpty()) {
            log.info("Document store not found for code: {}", com.dtech.auth.enums.DocumentStore.POLICY_INS);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        DocumentStore documentStore = documentStoreOpt.get();
        File file = new File(documentStore.getPath());

        if (!file.exists()) {
            log.info("File not found at path: {}", documentStore.getPath());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            Resource resource = new InputStreamResource(new FileInputStream(file));

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (FileNotFoundException e) {
            log.error("Error while loading sample format file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    @Transactional(readOnly = false,isolation = Isolation.READ_COMMITTED)
    public  ResponseEntity<ApiResponse<Object>> updateMaritalStatus(MaritalStatusRequestDTO maritalStatusRequestDTO,Locale locale) {
        try {
            log.info("Update marital status request: {}", maritalStatusRequestDTO);
            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(maritalStatusRequestDTO.getUsername(), Status.ACTIVE).map(user -> {

                if(user.getUserPersonalDetails().getMaritalStatus().equals(MaritalStatus.MARRIED) && maritalStatusRequestDTO.getRequestType().equals(RequestType.MARRIED.name())){
                    log.info("User marital status update request: {}", maritalStatusRequestDTO);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1035, messageSource.getMessage(ResponseMessageUtil.ALREADY_MARRIED_EMPLOYEE, null, locale)));
                } else if ((!user.getUserPersonalDetails().getMaritalStatus().equals(MaritalStatus.MARRIED)) && maritalStatusRequestDTO.getRequestType().equals(RequestType.DIVORCE.name())) {
                    log.info("User marital status update request employee not married: {}", maritalStatusRequestDTO);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1036, messageSource.getMessage(ResponseMessageUtil.UNMARRIED_EMPLOYEE_CANT_DIVORCE, null, locale)));
                }

                List<Document> uploadSupportingDocument = maritalStatusRequestDTO.getDocuments().stream().map(doc -> {
                    log.info("Upload supporting document from dependent");
                    try {
                        Document t = uploadImage(doc.getType(), doc.getFile(), doc.getFileType(), doc.getFileName());
                        documentRepository.save(t);
                        return t;
                    } catch (IOException e) {
                        log.error(e);
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

                com.dtech.auth.model.MaritalStatus maritalStatus = new com.dtech.auth.model.MaritalStatus();
                maritalStatus.setStatus(Workflow.UNDER_REVIEW);
                maritalStatus.setApplicationUser(user);
                log.info(uploadSupportingDocument.stream().toList());
                maritalStatus.setDocuments(uploadSupportingDocument);
                maritalStatus.setMaritalStatus(maritalStatusRequestDTO.getRequestType().equals(RequestType.MARRIED.name()) ? MaritalStatus.MARRIED : MaritalStatus.UNMARRIED);
                user.setMaritalStatus(maritalStatus);
                applicationUserRepository.saveAndFlush(user);
                notifyHrTeamOnCivilStatusPendingApproval(user, maritalStatusRequestDTO);

                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_DETAILS_UPDATE_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("User profile add dependant request application user not found {} ", maritalStatusRequestDTO);
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
            });
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private boolean hasActiveOrPendingSpouse(ApplicationUser applicationUser) {
        return claimDependentsRepository.existsByApplicationUserAndRelationCategoryInAndStatusInAndLiveStatus(
                applicationUser,
                List.of(RelationCategory.WIFE, RelationCategory.HUSBAND),
                List.of(Workflow.APPROVED, Workflow.UNDER_REVIEW),
                true
        );
    }

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void updateEligibleFacility() {
        log.info("User profile update eligible facility schedule call");
        List<ClaimsDependents> allByStatus = claimDependentsRepository.findAllByStatusIn(List.of(Workflow.APPROVED, Workflow.UNDER_REVIEW, Workflow.REJECTED));

        allByStatus.forEach(de -> {

            if(de.getRelationCategory().equals(RelationCategory.CHILD)){
                int age = DateTimeUtil.getAge(String.valueOf(de.getDob()));
                if(age > 25){
                    de.setEligibleFacility(Facility.DEATH);
                    claimDependentsRepository.saveAndFlush(de);
                }
            }

            if(de.getApplicationUser().getUserPersonalDetails().getUserCompanyDetails()
                    .getStaffCategories().getCode().equals("NS") &&
                    de.getApplicationUser().getUserPersonalDetails().getMaritalStatus().equals(MaritalStatus.UNMARRIED)
            && de.getDependentCategory().equals(DependentCategory.PARENTS)){
                int age = DateTimeUtil.getAge(String.valueOf(de.getDob()));
                if(age > 65){
                    de.setEligibleFacility(Facility.DEATH);
                    claimDependentsRepository.saveAndFlush(de);
                }
            }

            if(!de.getApplicationUser().getUserPersonalDetails().getUserCompanyDetails()
                    .getStaffCategories().getCode().equals("NS")){
                int age = DateTimeUtil.getAge(String.valueOf(de.getDob()));
                if(age > 70){
                    de.setEligibleFacility(Facility.DEATH);
                    claimDependentsRepository.saveAndFlush(de);
                }
            }

        });

    }


}
