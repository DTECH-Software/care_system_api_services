/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 9:56 AM
 * <p>
 */

package com.dtech.claim.service.impl;

import com.dtech.claim.dto.*;
import com.dtech.claim.dto.response.DocumentDownloadResponseDTO;
import com.dtech.claim.enums.*;
import com.dtech.claim.enums.TreatmentCategory;
import com.dtech.claim.feign.MessageFeignClient;
import com.dtech.claim.util.MultipartFileUtil;
import com.dtech.claim.dto.request.*;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.dto.response.InsuranceClaimRequestResponseDTO;
import com.dtech.claim.dto.search.ClaimHistory;
import com.dtech.claim.feign.DocumentFeignClient;
import com.dtech.claim.mapper.EntityToDtoMapper;
import com.dtech.claim.model.*;
import com.dtech.claim.repository.*;
import com.dtech.claim.service.InsuranceClaimRequestService;
import com.dtech.claim.specifications.InsuranceClaimHistorySpecification;
import com.dtech.claim.util.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Log4j2
@RequiredArgsConstructor
public class InsuranceClaimRequestServiceImpl implements InsuranceClaimRequestService {

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final ClaimDependentsRepository claimDependentsRepository;

    @Autowired
    private final InsurancePolicyRepository insurancePolicyRepository;

    @Autowired
    private final TreatmentRepository treatmentRepository;

    @Autowired
    private final EntityManager entityManager;

    @Autowired
    private final InsuranceClaimsDetailsRepository insuranceClaimsDetailsRepository;

    @Autowired
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Autowired
    private final CommonParameterRepository commonParameterRepository;

    @Autowired
    private final ApplicationOtpSessionRepository applicationOtpSessionRepository;

    @Autowired
    private final DocumentFeignClient documentFeignClient;

    @Autowired
    private final ModelMapper modelMapper;

    @Autowired
    private final MessageFeignClient messageFeignClient;

    @Autowired
    private final DeathClaimRequestRepository deathClaimRequestRepository;

    @Autowired
    private final TreatmentCategoryRepository treatmentCategoryRepository;

    @Autowired
    private final ApprovalWorkFlowRepository approvalWorkFlowRepository;

    @Autowired
    private final InsuranceDetailsLimitRepository insuranceDetailsLimitRepository;

    @Autowired
    private final InsuranceQuarterRepository insuranceQuarterRepository;

    @Autowired
    private final InsuranceStaffCategoryPeriodRepository insuranceStaffCategoryPeriodRepository;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> insuranceClaimRequest(ClaimRequestDTO claimRequestDTO, Locale locale) {
        try {
            log.info("Claim request processing started {}", claimRequestDTO);

            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(claimRequestDTO.getUsername().trim(), Status.ACTIVE).map((user) -> {

                if (user.getUserPersonalDetails().getIsTemp() || user.getUserPersonalDetails().getUserCompanyDetails().getFacility().equals(Facility.DEATH)) {
                    log.info("Claims request user not {} ", user.getUsername());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1029, messageSource.getMessage(ResponseMessageUtil.USER_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale)));
                }

                ResponseEntity<ApiResponse<Object>> treatmentValidationResult = validateDocumentCount(
                        claimRequestDTO.getDocuments(),
                        InsuranceClaimDocTypes.TREATMENT_BILL.name(),
                        CommonParam.TREATMENT_BILL_MAX_IMAGE.name(),
                        ResponseMessageUtil.INSURANCE_CLAIMS_TREATMENT_MAX_IMAGE_INVALID,
                        ResponseMessageUtil.INSURANCE_CLAIMS_TREATMENT_MIN_IMAGE_INVALID,
                        locale
                );

                if (treatmentValidationResult != null) {
                    log.info("Invalid document count: {}", treatmentValidationResult);
                    return treatmentValidationResult;
                }

                if (user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy() == null) {
                    log.info("User not eligible to claim request {}", claimRequestDTO.getUsername());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1029, messageSource.getMessage(ResponseMessageUtil.USER_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale)));
                } else if ((!claimRequestDTO.getIsEmployee()) && (claimRequestDTO.getTreatment().equals(TreatmentType.CRIC.name())) &&
                        user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode().equals("MM") || user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode().equals("EX-01") || user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode().equals("EX-02")) {
                    log.info("This cri facility cant eligibility dependent");
                    return ResponseEntity.ok().body(responseUtil.error(null, 1049, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale)));
                }

                return commonParameterRepository.findByCode(CommonParam.INSURANCE_CLAIM_REQUEST_PERIOD.name()).map((param) -> {
                            log.info("get - date from claim request {}", param);
                            Date minuesDate = DateTimeUtil.getMinuesDate(param.getValue() + 1);

                            if (claimRequestDTO.getToDate().before(minuesDate)) {
                                log.info("older than claim request {}", claimRequestDTO.getUsername());
                                return ResponseEntity.ok().body(responseUtil.error(null, 1037, messageSource.getMessage(ResponseMessageUtil.OLDER_DATE_INSURANCE_CLAIM_REQUEST, null, locale)));
                            }

                            return insurancePolicyRepository.findByIdAndStatus(user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy().getId(), Status.ACTIVE).map((policy) ->
                                    treatmentRepository.findByTreatmentCodeAndStatus(claimRequestDTO.getTreatment(), Status.ACTIVE).map((treatment) ->
                                            treatmentCategoryRepository.findByCodeAndStatus(claimRequestDTO.getTreatmentCategory(), Status.ACTIVE).map(tc -> {

                                                Optional<ClaimsDependents> claimsDependents;

                                                if (!claimRequestDTO.getIsEmployee()) {
                                                    log.info("Claim dependent found for request {}", true);
                                                    claimsDependents = claimDependentsRepository.
                                                            findByIdAndApplicationUserAndStatusAndEligibleFacilityIn(
                                                                    claimRequestDTO.getClaimsDependentId(),
                                                                    user,
                                                                    Workflow.APPROVED, List.of(Facility.INSURANCE, Facility.BOTH));

                                                    if (claimsDependents.isEmpty()) {
                                                        log.info("Claim dependent not found or not eligible for insurance");
                                                        return ResponseEntity.ok().body(responseUtil.error(null, 1034, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_NOT_FOUND_OR_FACILITY_NOT_ELIGIBLE, null, locale)));
                                                    }

                                                    boolean existsed = deathClaimRequestRepository
                                                            .existsByClaimsDependentsAndEmployeeAndRequestStatusIn(claimsDependents.get(), user, List.of(Workflow.APPROVED));

                                                    if (existsed) {
                                                        log.info("Claim dependent death claim request approved {}", true);
                                                        return ResponseEntity.ok().body(responseUtil.error(null, 1047, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_DEATH_REQUEST_ALREADY_PROCEED, null, locale)));
                                                    }

                                                    if (user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode().equals("NS") && !user.getUserPersonalDetails().isMaritalStatus() && claimsDependents.get().getDependentCategory().equals(DependentCategory.PARENTS)) {
                                                        int age = DateTimeUtil.getAge(String.valueOf(claimsDependents.get().getDob()));
                                                        log.info("Claim deendent age {} ", age);
                                                        if (age > 65) {
                                                            log.info("Claim deendent age {} ", age);
                                                            return ResponseEntity.ok().body(responseUtil.error(null, 1047, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_INSURANCE_REQUEST_PARENT_AGE_LIMIT_EXCEED, new Object[]{65}, locale)));
                                                        }
                                                    }else if(claimsDependents.get().getDependentCategory().equals(DependentCategory.CHILDREN)){
                                                        int age = DateTimeUtil.getAge(String.valueOf(claimsDependents.get().getDob()));
                                                        log.info("Claim deendent child age {} ", age);
                                                        if (age > 25) {
                                                            log.info("Claim deendent child age {} ", age);
                                                            return ResponseEntity.ok().body(responseUtil.error(null, 1047, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_INSURANCE_REQUEST_CHILDREN_AGE_LIMIT_EXCEED, new Object[]{65}, locale)));

                                                        }
                                                    }

                                                } else {
                                                    claimsDependents = Optional.empty();
                                                }
                                                log.info("Gey current year {}", DateTimeUtil.getCurrentYear());
                                                log.info(user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode());
                                                log.info(DateTimeUtil.getCurrentDateTime());
                                                InsuranceStaffCategoryPeriod insuranceYear = insuranceStaffCategoryPeriodRepository.
                                                        findByDateWithinRange(DateTimeUtil.getCurrentDateTime(), user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode()).orElse(null);
                                                log.info("Current insurance year {}", insuranceYear);
                                                if (insuranceYear == null) {
                                                    log.info("Insurance period not found");
                                                    return ResponseEntity.ok().body(responseUtil.error(null, 1057, messageSource.getMessage(ResponseMessageUtil.INSURANCE_PERIOD_NOT_FOUND, null, locale)));
                                                }

                                                BigDecimal sumOfClaims = BigDecimal.ZERO;

                                                sumOfClaims = insuranceClaimsRequestRepository.
                                                        getSumRequestAmountByEmployeeAndTreatmentAndStatus(user,
                                                                claimRequestDTO.getTreatment(),
                                                                insuranceYear.getId(),
                                                                List.of(Workflow.APPROVED));

                                                log.info("Already claims {} sum of claims ", sumOfClaims);

                                                //   if (currentYear == year) {
                                                log.info("Request fund limit exceeded with ent limit");
                                                Date permentDateTime = user.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate();
                                                log.info("User month per {}", permentDateTime);
                                                InsuranceDetailsLimit insuranceDetailsLimit = insuranceDetailsLimitRepository.findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment(
                                                        user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy(), Status.ACTIVE, insuranceYear, treatment).orElse(null);

                                                if (insuranceDetailsLimit == null) {
                                                    log.info("Insurance detail is null");
                                                    return ResponseEntity.ok().body(responseUtil.error(null, 1030, messageSource.getMessage(ResponseMessageUtil.INSURANCE_POLICY_NOT_FOUND, null, locale)));
                                                }

                                                if (claimRequestDTO.getTreatmentCategory().equals(TreatmentCategory.OTHER.name())) {

                                                    BigDecimal limit = null;
                                                    InsuranceQuarter treatmentQuarter = null;
                                                    if (insuranceDetailsLimit.getIsQuarter()) {
                                                        treatmentQuarter = insuranceQuarterRepository.findByDateWithinRangeAndCodeWithLimit(insuranceDetailsLimit, TreatmentCategory.OTHER.name(), permentDateTime).orElse(null);
                                                        if (treatmentQuarter == null) {
                                                            limit = insuranceDetailsLimit.getGlobalLimit();
                                                        } else {
                                                            limit = treatmentQuarter.getQuarterLimit();
                                                        }

                                                    } else {
                                                        limit = insuranceDetailsLimit.getGlobalLimit();
                                                    }

                                                    BigDecimal remainingBalance = limit.subtract(sumOfClaims != null ? sumOfClaims : BigDecimal.valueOf(0.00));
                                                    log.info("Remaining balance {}", remainingBalance);
                                                    if (claimRequestDTO.getRequestAmount().compareTo(remainingBalance) > 0) {
                                                        log.info("Request fund limit exceeded with ent limit {} {} {} {}", claimRequestDTO.getRequestAmount(), limit, remainingBalance, sumOfClaims);
                                                        return ResponseEntity.ok().body(responseUtil.error(null, 1052, messageSource.getMessage(ResponseMessageUtil.CLAIM_LIMIT_EXCEED_WITH_LIMIT, new Object[]{remainingBalance}, locale)));
                                                    }

                                                    if (claimRequestDTO.getIsValidation()) {
                                                        log.info("Insurance claim request validate only success {}", true);
                                                        return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.INSURANCE_CLAIM_REQUEST_VALIDATION_SUCCESS, null, locale)));
                                                    } else {

                                                        if (user.getApplicationOtpSession() != null) {
                                                            log.info("Otp request otp session  {} ", user.getApplicationOtpSession());

                                                            if (DateTimeUtil.getSeconds(user.getApplicationOtpSession().getCreatedDate(), 600).after(DateTimeUtil.getCurrentDateTime()) &&
                                                                    user.getApplicationOtpSession().getOtp().equals(claimRequestDTO.getOtp()) && user.getApplicationOtpSession().isValidated()) {
                                                                log.info("Otp request valid {} ", user.getApplicationOtpSession());
                                                                updateApplicationUserOtpData(user, user.getApplicationOtpSession());
                                                                ApprovalWorkFlow approvalWorkFlow = updateApprovalData();
                                                                String claimRequestId = saveClaimRequest(claimRequestDTO, user, claimsDependents, treatment, tc, approvalWorkFlow, insuranceYear, insuranceDetailsLimit, treatmentQuarter);
                                                                notifyMessage(user.getPrimaryMobile(), claimRequestId);
                                                                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.INSURANCE_CLAIM_REQUEST_SUBMIT_SUCCESS, null, locale)));
                                                            } else {
                                                                log.info("Otp request validation fail otp or invalid session {}", user.getApplicationOtpSession());
                                                                return ResponseEntity.ok().body(responseUtil.error(null, 1016, messageSource.getMessage(ResponseMessageUtil.OTP_INVALID_OR_SESSION_TIME_OUT, null, locale)));
                                                            }
                                                        } else {
                                                            log.info("Otp request otp session not found {} ", claimRequestDTO.getUsername());
                                                            return ResponseEntity.ok().body(responseUtil.error(null, 1015, messageSource.getMessage(ResponseMessageUtil.OTP_SESSION_NOT_FOUND, null, locale)));
                                                        }
                                                    }

                                                } else {

                                                    InsuranceQuarter treatmentQuarter = insuranceQuarterRepository.findByCodeWithLimit(insuranceDetailsLimit, claimRequestDTO.getTreatmentCategory()).orElse(null);

                                                    if (treatmentQuarter == null) {
                                                        log.info("Insurance detail quarter is null");
                                                        return ResponseEntity.ok().body(responseUtil.error(null, 1030, messageSource.getMessage(ResponseMessageUtil.INSURANCE_POLICY_NOT_FOUND, null, locale)));
                                                    }

                                                    if (claimRequestDTO.getRequestAmount().compareTo(treatmentQuarter.getQuarterLimit()) > 0) {
                                                        log.info("Request fund limit exceeded without event limit but dental max limit {} {} {} ", claimRequestDTO.getRequestAmount(), treatmentQuarter.getQuarterLimit(), sumOfClaims);
                                                        return ResponseEntity.ok().body(responseUtil.error(null, 1052, messageSource.getMessage(ResponseMessageUtil.CLAIM_LIMIT_EXCEED_WITH_LIMIT, new Object[]{treatmentQuarter.getQuarterLimit()}, locale)));

                                                    } else {
                                                        log.info("Request already dental spec exist fund limit");
                                                        BigDecimal sumOfClaimsCategory = BigDecimal.ZERO;
                                                        sumOfClaimsCategory = insuranceClaimsRequestRepository.
                                                                getSumRequestAmountByEmployeeAndTreatmentAndTreatmentCategoryAndStatus(user,
                                                                        claimRequestDTO.getTreatment(),
                                                                        claimRequestDTO.getTreatmentCategory(),
                                                                        insuranceYear.getId(),
                                                                        List.of(Workflow.APPROVED));

                                                        if (sumOfClaimsCategory != null) {
                                                            if (sumOfClaimsCategory.compareTo(treatmentQuarter.getQuarterLimit()) == 0) {
                                                                log.info("Request fund limit exceeded without event limit but dental max limit equals {} {} {} ", claimRequestDTO.getRequestAmount(), treatmentQuarter.getQuarterLimit(), sumOfClaims);
                                                                return ResponseEntity.ok().body(responseUtil.error(null, 1052, messageSource.getMessage(ResponseMessageUtil.CLAIM_LIMIT_EXCEED_WITH_LIMIT, new Object[]{treatmentQuarter.getQuarterLimit()}, locale)));

                                                            } else if (treatmentQuarter.getQuarterLimit().subtract(sumOfClaimsCategory).compareTo(claimRequestDTO.getRequestAmount()) < 0) {
                                                                log.info("Request fund limit exceeded without event limit but dental max limit request balance {} {} {} ", claimRequestDTO.getRequestAmount(), treatmentQuarter.getQuarterLimit(), sumOfClaimsCategory);
                                                                return ResponseEntity.ok().body(responseUtil.error(null, 1052, messageSource.getMessage(ResponseMessageUtil.CLAIM_LIMIT_EXCEED_WITH_LIMIT, new Object[]{treatmentQuarter.getQuarterLimit()}, locale)));

                                                            }
                                                        }

                                                    }

                                                    BigDecimal remainingBalance = insuranceDetailsLimit.getGlobalLimit().subtract(sumOfClaims != null ? sumOfClaims : BigDecimal.valueOf(0.00));
                                                    log.info("Remaining balance {}", remainingBalance);
                                                    if (claimRequestDTO.getRequestAmount().compareTo(remainingBalance) > 0) {
                                                        log.info("Request fund limit exceeded without event limit {} {} {} {}", claimRequestDTO.getRequestAmount(), treatmentQuarter.getQuarterLimit(), remainingBalance, sumOfClaims);
                                                        return ResponseEntity.ok().body(responseUtil.error(null, 1052, messageSource.getMessage(ResponseMessageUtil.CLAIM_LIMIT_EXCEED_WITH_LIMIT, new Object[]{remainingBalance}, locale)));
                                                    }

                                                    if (claimRequestDTO.getIsValidation()) {
                                                        log.info("Insurance claim request validate only success{}", true);
                                                        return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.INSURANCE_CLAIM_REQUEST_VALIDATION_SUCCESS, null, locale)));
                                                    } else {

                                                        if (user.getApplicationOtpSession() != null) {
                                                            log.info("Otp request otp session  {} ", user.getApplicationOtpSession());

                                                            if (DateTimeUtil.getSeconds(user.getApplicationOtpSession().getCreatedDate(), 600).after(DateTimeUtil.getCurrentDateTime()) &&
                                                                    user.getApplicationOtpSession().getOtp().equals(claimRequestDTO.getOtp()) && user.getApplicationOtpSession().isValidated()) {
                                                                log.info("Otp request valid {} ", user.getApplicationOtpSession());
                                                                ApprovalWorkFlow approvalWorkFlow = updateApprovalData();
                                                                updateApplicationUserOtpData(user, user.getApplicationOtpSession());
                                                                String claimRequestId = saveClaimRequest(claimRequestDTO, user, claimsDependents, treatment, tc, approvalWorkFlow, insuranceYear, insuranceDetailsLimit, treatmentQuarter);
                                                                notifyMessage(user.getPrimaryMobile(), claimRequestId);
                                                                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.INSURANCE_CLAIM_REQUEST_SUBMIT_SUCCESS, null, locale)));
                                                            } else {
                                                                log.info("Otp request validation fail otp or invalid session {}", user.getApplicationOtpSession());
                                                                return ResponseEntity.ok().body(responseUtil.error(null, 1016, messageSource.getMessage(ResponseMessageUtil.OTP_INVALID_OR_SESSION_TIME_OUT, null, locale)));
                                                            }
                                                        } else {
                                                            log.info("Otp request otp session not found {} ", claimRequestDTO.getUsername());
                                                            return ResponseEntity.ok().body(responseUtil.error(null, 1015, messageSource.getMessage(ResponseMessageUtil.OTP_SESSION_NOT_FOUND, null, locale)));
                                                        }
                                                    }

                                                }

                                            }).orElseGet(() -> {
                                                log.info("User insurance policy period treatment not found");
                                                return ResponseEntity.ok().body(responseUtil.error(null, 1033, messageSource.getMessage(ResponseMessageUtil.POLICY_TREATMENT_PERIOD_NOT_FOUND_OR_INACTIVE, null, locale)));
                                            })).orElseGet(() -> {
                                        log.info("User insurance treatment not found {} ", DateTimeUtil.getCurrentDateTime());
                                        return ResponseEntity.ok().body(responseUtil.error(null, 1032, messageSource.getMessage(ResponseMessageUtil.TREATMENT_NOT_FOUND, null, locale)));
                                    })).orElseGet(() -> {
                                log.info("User insurance period not found {} ", DateTimeUtil.getCurrentDateTime());
                                return ResponseEntity.ok().body(responseUtil.error(null, 1031, messageSource.getMessage(ResponseMessageUtil.INSURANCE_PERIOD_NOT_FOUND, null, locale)));
                            });
                        })
                        .orElseGet(() -> {
                            log.info("User common param claim request {}", claimRequestDTO.getUsername());
                            return ResponseEntity.ok().body(responseUtil.error(null, 1036, messageSource.getMessage(ResponseMessageUtil.COMMON_PARAM_NOT_FOUND, null, locale)));
                        });

            }).orElseGet(() -> {
                log.info("User claim request user not found {} ", claimRequestDTO);
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Async
    protected void notifyMessage(String mobile, String requestId) {
        try {
            log.info("Insurance request notify email");
            MessageRequestDTO messageRequestDTO = new MessageRequestDTO();
            messageRequestDTO.setValue(requestId);
            messageRequestDTO.setMobileNo(mobile);
            messageRequestDTO.setType(MessageType.INSURANCE_CLAIM.name());
            log.info("Before message request mapper {} ", messageRequestDTO);
            log.info("Before calling message service {}", messageFeignClient);
            messageFeignClient.sendMessage(messageRequestDTO);
        } catch (RuntimeException e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    protected ResponseEntity<ApiResponse<Object>> validateDocumentCount(List<SupportingDocumentDTO> documents, String documentType,
                                                                        String commonParamCode, String maxMessage, String minMessage, Locale locale) {
        try {
            long count = documents.stream().filter(val -> val.getType().equals(documentType)).count();
            CommonParameter commonParameter = commonParameterRepository.findByCode(commonParamCode).orElse(null);
            long maxImages = commonParameter != null ? commonParameter.getValue() : 1;

            if (count > maxImages) {
                log.info("Claim request max {} invalid", documentType);
                return ResponseEntity.ok().body(responseUtil.error(null, 1043, messageSource.getMessage(maxMessage, new Object[]{maxImages}, locale)));
            } else if (count == 0) {
                log.info("Claim request min {} invalid", documentType);
                return ResponseEntity.ok().body(responseUtil.error(null, 1044, messageSource.getMessage(minMessage, null, locale)));
            }
            return null;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> insuranceClaimReferenceData(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Insurance claim reference data {}", channelRequestDTO);
            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(channelRequestDTO.getUsername().trim(), Status.ACTIVE).map((user) -> {
                InsuranceStaffCategoryPeriod period = insuranceStaffCategoryPeriodRepository.findByDateWithinRange(DateTimeUtil.getCurrentDateTime(), user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode()).orElse(null);
                List<Treatment> treatmentList = treatmentRepository.findAllByStatus(Status.ACTIVE);
                log.info("Insurance claim reference data get dependence {} ", treatmentList);

                List<DependentBaseDTO> collect = claimDependentsRepository
                        .findByApplicationUserAndStatusAndEligibleFacilityInAndLiveStatus(user, Workflow.APPROVED, Arrays.asList(Facility.INSURANCE, Facility.BOTH), true).
                        stream()
                        .filter(dep -> {
                            log.info("SUCCESS FILTER {} ", dep.getId());
                            boolean ex = deathClaimRequestRepository.existsByClaimsDependentsAndEmployeeAndRequestStatusIn(dep, user, List.of(Workflow.APPROVED));
                            return !ex;
                        }).map(dep -> new DependentBaseDTO(
                                String.valueOf(dep.getId()),
                                dep.getFirstName() + " " + dep.getLastName(),
                                dep.getRelationCategory().getDescription()
                        )).collect(Collectors.toList());

                log.info("Call minus insurance claim date");
                Date minuesDate = DateTimeUtil.getMinuesDate(Objects.requireNonNull(commonParameterRepository.findByCode(CommonParam.INSURANCE_CLAIM_REQUEST_PERIOD.name()).orElse(null)).getValue());
                int diagnosis = Objects.requireNonNull(commonParameterRepository.findByCode(CommonParam.DIAGNOSIS_CARD_MAX_IMAGE.name()).orElse(null)).getValue();
                int treatment = Objects.requireNonNull(commonParameterRepository.findByCode(CommonParam.TREATMENT_BILL_MAX_IMAGE.name()).orElse(null)).getValue();
                log.info("Get minus insurance claim date {} ", minuesDate);
                Map<String, Object> splashData = new HashMap<>();
                List<SimpleBaseDTO> userWiseTreatment = new ArrayList<>();
                Map<String, List<SimpleBaseDTO>> userWiseTreatmentCategory = new HashMap<>();
                Map<String, Map<String, AvailableInsuranceLimitDTO>> limits = new HashMap<>();
                if (period != null) {

                    List<InsuranceDetailsLimit> insuranceDetailsLimits = insuranceDetailsLimitRepository.
                            findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriod(
                                    user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy(),
                                    Status.ACTIVE, period);

                    insuranceDetailsLimits.forEach(in -> {
                        log.info("Add treatment");
                        addIfNotPresent(userWiseTreatment, in);
                        String insuranceCategory = in.getTreatment().getTreatmentCode();
                        userWiseTreatmentCategory.computeIfAbsent(insuranceCategory, k -> new ArrayList<>());
                        addIfNotPresentTreatmentCategory(userWiseTreatmentCategory.get(insuranceCategory), in);
                        try {
                            setLimitMap(limits, in, user, period);
                        } catch (ParseException e) {
                            log.error(e.getMessage());
                            throw new RuntimeException(e);
                        }
                    });

                }
                log.info("Claims data success ");
                splashData.put("insuranceClaimsDependents", collect);
                splashData.put("treatment", userWiseTreatment);
                splashData.put("treatmentCategory", userWiseTreatmentCategory);
                splashData.put("insuranceClaimsFundLimits", limits);
                splashData.put("insuranceMinPastDate", minuesDate);
                splashData.put("maxImageForDiagnosis", diagnosis);
                splashData.put("maxImageForTreatment", treatment);
                return ResponseEntity.ok().body(responseUtil.success((Object) splashData, messageSource.getMessage(ResponseMessageUtil.INSURANCE_CLAIMS_REFERENCE_DETAILS_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("User insurance claim request user not found {} ", channelRequestDTO);
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public void setLimitMap(Map<String, Map<String, AvailableInsuranceLimitDTO>> limitMap,
                            InsuranceDetailsLimit insuranceDetailsLimit,
                            ApplicationUser applicationUser, InsuranceStaffCategoryPeriod period) throws ParseException {


        try {
            log.info("Insurance ref {}", insuranceDetailsLimit.getId());

            for (InsuranceQuarter insuranceQuarter : insuranceDetailsLimit.getInsuranceQuarters()) {

                String category = insuranceQuarter.getTreatmentCategory().getCode();
                log.info("Insurance category {}", category);
                String treatmentCode = insuranceDetailsLimit.getTreatment().getTreatmentCode();
                Long insurancePeriod = insuranceDetailsLimit.getInsuranceStaffCategoryPeriod().getId();

                int currentYear = DateTimeUtil.getCurrentYear();
                log.info("Current year {}", currentYear);
                int year = DateTimeUtil.getYear(applicationUser.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate());
                int month = DateTimeUtil.getMonth(applicationUser.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate());
                log.info("Year {}", year);
                log.info("Month {}", month);

                BigDecimal funLimit = BigDecimal.valueOf(0.00);

                if (category.equals(TreatmentCategory.OTHER.name())) {

                    BigDecimal sum = insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndStatus(
                            applicationUser,
                            treatmentCode,
                            insurancePeriod,
                            List.of(Workflow.APPROVED)
                    );

                    if (insuranceDetailsLimit.getIsQuarter()) {
                        log.info("First request {} ", insuranceQuarter.getQuarterLimit());

                        Date permentDateTime = applicationUser.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate();

                        InsuranceQuarter treatmentQuarter = insuranceQuarterRepository.findByDateWithinRangeAndCodeWithLimit(insuranceDetailsLimit, TreatmentCategory.OTHER.name(), permentDateTime).orElse(null);

                        BigDecimal maxLimit = BigDecimal.valueOf(0.00);

                        if (treatmentQuarter != null) {
                            funLimit = treatmentQuarter.getQuarterLimit();

                            maxLimit = treatmentQuarter.getQuarterLimit();
                        } else {
                            funLimit = insuranceDetailsLimit.getGlobalLimit();

                            maxLimit = insuranceDetailsLimit.getGlobalLimit();
                        }

                        log.info("Sum amount insurance ref data {} {} ", sum, insuranceDetailsLimit.getGlobalLimit());
                        BigDecimal reValue = funLimit.subtract(sum != null ? sum : BigDecimal.valueOf(0.00));
                        BigDecimal remaining = reValue.compareTo(BigDecimal.ZERO) > 0 ? reValue : BigDecimal.valueOf(0.00);
                        log.info("Remaining amount insurance ref data {}", reValue);

                        limitMap
                                .computeIfAbsent(treatmentCode, k -> new HashMap<>())
                                .merge(category, new AvailableInsuranceLimitDTO(remaining, maxLimit),
                                        (existing, newDetails) -> new AvailableInsuranceLimitDTO(
                                                newDetails.getAvailableLimit(),
                                                newDetails.getFundLimit()
                                        ));
                    } else {

                        funLimit = insuranceDetailsLimit.getGlobalLimit();

                        BigDecimal maxLimit = insuranceDetailsLimit.getGlobalLimit();

                        log.info("Sum amount insurance ref data {} {}", sum, insuranceDetailsLimit.getGlobalLimit());
                        BigDecimal reValue = funLimit.subtract(sum != null ? sum : BigDecimal.valueOf(0.00));
                        BigDecimal remaining = reValue.compareTo(BigDecimal.ZERO) > 0 ? reValue : BigDecimal.valueOf(0.00);
                        log.info("Remaining amount insurance ref data {}", reValue);

                        limitMap
                                .computeIfAbsent(treatmentCode, k -> new HashMap<>())
                                .merge(category, new AvailableInsuranceLimitDTO(remaining, maxLimit),
                                        (existing, newDetails) -> new AvailableInsuranceLimitDTO(
                                                newDetails.getAvailableLimit(),
                                                newDetails.getFundLimit()
                                        ));
                    }

                } else {
                    log.info("Event period but dental or Spec");

                    BigDecimal gSum = insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndStatus(
                            applicationUser,
                            treatmentCode,
                            insurancePeriod,
                            List.of(Workflow.APPROVED)
                    );

                    Date permentDateTime = applicationUser.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate();

                    InsuranceQuarter treatmentQuarter = insuranceQuarterRepository.
                            findByDateWithinRangeAndCodeWithLimit(insuranceDetailsLimit, TreatmentCategory.OTHER.name(), permentDateTime).orElse(null);

                    if(treatmentQuarter != null) {

                        BigDecimal maxLimit = insuranceQuarter.getQuarterLimit();

                        if(treatmentQuarter.getQuarterLimit().compareTo(insuranceQuarter.getQuarterLimit()) < 0){
                            maxLimit = treatmentQuarter.getQuarterLimit();
                        }

                        if(gSum == null){
                            funLimit = treatmentQuarter.getQuarterLimit();
                        }else {
                            log.info("Remaining amount insurance ref data {} {} ", gSum, maxLimit);
                            BigDecimal gFLimit = treatmentQuarter.getQuarterLimit();
                            if (gFLimit.compareTo(gSum) == 0) {
                                funLimit = BigDecimal.ZERO;
                                gSum = null;
                            } else {

                                BigDecimal sum = insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndTreatmentCategoryAndStatus(
                                        applicationUser,
                                        treatmentCode,
                                        insuranceQuarter.getTreatmentCategory().getCode(),
                                        insurancePeriod,
                                        List.of(Workflow.APPROVED)
                                );

                                if (sum != null) {
                                    if (maxLimit.compareTo(sum) == 0) {
                                        funLimit = BigDecimal.valueOf(0.00);
                                        gSum = null;
                                    } else if (maxLimit.compareTo(sum) > 0) {
                                        funLimit = maxLimit.subtract(sum);
                                        gSum = null;
                                    } else {
                                        funLimit = gFLimit.subtract(gSum);
                                        gSum = null;
                                    }

                                } else {

                                    funLimit = gFLimit.subtract(gSum);
                                    if (funLimit.compareTo(maxLimit) > 0) {
                                        funLimit = maxLimit;
                                    }

                                    gSum = null;
                                }
                            }
                        }

                        BigDecimal reValue = funLimit.subtract(gSum != null ? gSum : BigDecimal.valueOf(0.00));
                        BigDecimal remaining = reValue.compareTo(BigDecimal.ZERO) > 0 ? reValue : BigDecimal.valueOf(0.00);
                        log.info("Remaining amount insurance ref data {}", reValue);

                        limitMap
                                .computeIfAbsent(treatmentCode, k -> new HashMap<>())
                                .merge(category, new AvailableInsuranceLimitDTO(remaining, maxLimit),
                                        (existing, newDetails) -> new AvailableInsuranceLimitDTO(
                                                newDetails.getAvailableLimit(),
                                                newDetails.getFundLimit()
                                        ));


                    }else{
                        BigDecimal maxLimit = insuranceQuarter.getQuarterLimit();

                        if (gSum == null) {
                            funLimit = insuranceQuarter.getQuarterLimit();
                        } else {
                            log.info("Remaining amount insurance ref data {} {} ", gSum, maxLimit);
                            BigDecimal gFLimit = insuranceDetailsLimit.getGlobalLimit();
                            if (gFLimit.compareTo(gSum) == 0) {
                                funLimit = BigDecimal.ZERO;
                                gSum = null;
                            } else {

                                BigDecimal sum = insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndTreatmentCategoryAndStatus(
                                        applicationUser,
                                        treatmentCode,
                                        insuranceQuarter.getTreatmentCategory().getCode(),
                                        insurancePeriod,
                                        List.of(Workflow.APPROVED)
                                );

                                if (sum != null) {
                                    if (maxLimit.compareTo(sum) == 0) {
                                        funLimit = BigDecimal.valueOf(0.00);
                                        gSum = null;
                                    } else if (maxLimit.compareTo(sum) > 0) {
                                        funLimit = maxLimit.subtract(sum);
                                        gSum = null;
                                    } else {
                                        funLimit = gFLimit.subtract(gSum);
                                        gSum = null;
                                    }

                                } else {

                                    funLimit = gFLimit.subtract(gSum);
                                    if (funLimit.compareTo(maxLimit) > 0) {
                                        funLimit = maxLimit;
                                    }

                                    gSum = null;
                                }
                            }
                        }

                        BigDecimal reValue = funLimit.subtract(gSum != null ? gSum : BigDecimal.valueOf(0.00));
                        BigDecimal remaining = reValue.compareTo(BigDecimal.ZERO) > 0 ? reValue : BigDecimal.valueOf(0.00);
                        log.info("Remaining amount insurance ref data {}", reValue);

                        limitMap
                                .computeIfAbsent(treatmentCode, k -> new HashMap<>())
                                .merge(category, new AvailableInsuranceLimitDTO(remaining, maxLimit),
                                        (existing, newDetails) -> new AvailableInsuranceLimitDTO(
                                                newDetails.getAvailableLimit(),
                                                newDetails.getFundLimit()
                                        ));
                    }

                }

            }
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private void addIfNotPresent(List<SimpleBaseDTO> tCategoryList, InsuranceDetailsLimit insuranceDetailsLimit) {
        boolean alreadyPresent = tCategoryList.stream()
                .anyMatch(dto -> dto.getCode().equals(insuranceDetailsLimit.getTreatment().getTreatmentCode()));
        if (!alreadyPresent) {
            tCategoryList.add(new SimpleBaseDTO(insuranceDetailsLimit.getTreatment().getTreatmentCode(),
                    insuranceDetailsLimit.getTreatment().getTreatmentDescription()));
        }
    }

    private void addIfNotPresentTreatmentCategory(List<SimpleBaseDTO> tCategoryList, InsuranceDetailsLimit insuranceDetailsLimit) {

        for (InsuranceQuarter insuranceQuarter : insuranceDetailsLimit.getInsuranceQuarters()) {
            String categoryCode = insuranceQuarter.getTreatmentCategory().getCode();

            boolean alreadyPresent = tCategoryList.stream()
                    .anyMatch(dto -> dto.getCode().equals(categoryCode));

            if (!alreadyPresent) {
                tCategoryList.add(new SimpleBaseDTO(
                        categoryCode,
                        insuranceQuarter.getTreatmentCategory().getDescription()
                ));
            }
        }
    }


    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> insuranceClaimHistoryList(PaginationRequest<ClaimHistory> paginationRequest, Locale locale) {
        try {
            log.info("Claim history filter list {}", paginationRequest);

            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(paginationRequest.getUsername().trim(), Status.ACTIVE).map((user) -> {

                Pageable pageable = PaginationUtil.getPageable(paginationRequest);

                Page<InsuranceClaimsRequest> claimsRequests = Objects.nonNull(paginationRequest.getSearch()) ?
                        insuranceClaimsRequestRepository.findAll(InsuranceClaimHistorySpecification.getSpecification(paginationRequest.getSearch(), user.getId()), pageable) :
                        insuranceClaimsRequestRepository.findAll(InsuranceClaimHistorySpecification.getSpecification(user.getId()), pageable);
                log.info("Filter records {}", claimsRequests);
                long totalElements = Objects.nonNull(paginationRequest.getSearch()) ?
                        insuranceClaimsRequestRepository.count(InsuranceClaimHistorySpecification.getSpecification(paginationRequest.getSearch(), user.getId())) :
                        insuranceClaimsRequestRepository.count(InsuranceClaimHistorySpecification.getSpecification(user.getId()));
                log.info("Total elements count records {}", totalElements);
                log.info("Filter list data fetching success");
                List<InsuranceClaimRequestResponseDTO> collectList = claimsRequests.stream()
                        .map(EntityToDtoMapper::mapInsuranceClaimHistoryDetails).toList();
                log.info("Filter list {} success", collectList);
                return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<InsuranceClaimRequestResponseDTO>(collectList, collectList.size(), totalElements),
                        messageSource.getMessage(ResponseMessageUtil.INSURANCE_CLAIM_REQUEST_HISTORY_FILTER_LIST_SUCCESS,
                                null, locale)));

            }).orElseGet(() -> {
                log.info("User insurance claim filter request user not found {} ", paginationRequest);
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> insuranceDetailsFindById(DetailsViewRequestDTO detailsViewRequestDTO, Locale locale) {
        try {
            log.info("Insurance claims find by id {}", detailsViewRequestDTO.getId());

            return insuranceClaimsRequestRepository.findById(detailsViewRequestDTO.getId()).map((insuranceClaimsRequest) -> {

                List<DocumentDownloadResponseDTO> collect = insuranceClaimsRequest.getInsuranceClaimsDetails().getDocuments().stream().map((document -> {
                    log.info("inside document list claims request details attachment view {} ", document);
                    return new DocumentDownloadResponseDTO(String.valueOf(document.getType()), document.getFileName(), document.getFileType(), document.getDoc());
                })).toList();
                return ResponseEntity.ok().body(responseUtil.success((Object) Map.of("documents", collect),
                        messageSource.getMessage(ResponseMessageUtil.INSURANCE_CLAIM_REQUEST_FIND_BY_ID_SUCCESS,
                                null, locale)));
            }).orElseGet(() -> {
                log.info("Insurance claims details not found by {}", detailsViewRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1053, messageSource.getMessage(ResponseMessageUtil.INSURANCE_CLAIMS_REQUEST_DETAILS_NOT_FOUND_BY_ID, null, locale)));
            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateApplicationUserOtpData(ApplicationUser applicationUser,
                                                ApplicationOtpSession applicationOtpSession) {
        log.info("Update otp validation request otp records");
        applicationUser.setOtpAttemptCount(0);
        applicationOtpSession.setValidated(true);
        applicationUserRepository.saveAndFlush(applicationUser);
        applicationOtpSessionRepository.saveAndFlush(applicationOtpSession);
    }

    @Transactional
    protected ApprovalWorkFlow updateApprovalData() {
        log.info("Update approval process data");
        ApprovalWorkFlow approvalWorkFlow = new ApprovalWorkFlow();
        approvalWorkFlow.setApprovalLevel(ApprovalLevel.LEVEL01);
        approvalWorkFlow.setStatus(Workflow.UNDER_REVIEW);
        return approvalWorkFlowRepository.saveAndFlush(approvalWorkFlow);
    }

    @Transactional
    protected InsuranceClaimsDetails saveClaimRequestDetails(ClaimRequestDTO claimRequestDTO, Treatment treatment, com.dtech.claim.model.TreatmentCategory treatmentCategory, InsuranceStaffCategoryPeriod insuranceYear) {
        try {
            log.info("Claim request details save started {}", claimRequestDTO);
            InsuranceClaimsDetails insuranceClaimsDetails = new InsuranceClaimsDetails();
            insuranceClaimsDetails.setTreatment(treatment);
            insuranceClaimsDetails.setTreatmentCategory(treatmentCategory);
            insuranceClaimsDetails.setInsuranceStaffCategoryPeriod(insuranceYear);
            if (claimRequestDTO.getFromDate() != null) {
                insuranceClaimsDetails.setFromTreatmentDate(claimRequestDTO.getFromDate());
            }
            insuranceClaimsDetails.setToTreatmentDate(claimRequestDTO.getToDate());
            insuranceClaimsDetails.setDisease(claimRequestDTO.getDisease());
            List<Document> uploadSupportingDocument = claimRequestDTO.getDocuments().stream().map(doc -> {
                log.info("Upload supporting document from dependent");
                try {
                    return uploadImage(doc.getType(), doc.getFile(), doc.getFileType(), doc.getFileName());
                } catch (IOException e) {
                    log.error(e);
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());

            insuranceClaimsDetails.setDocuments(
                    uploadSupportingDocument
            );
            return insuranceClaimsDetailsRepository.saveAndFlush(insuranceClaimsDetails);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    protected Document uploadImage(String tye, String file, String fileType, String fileName) throws IOException {
        try {
            log.info("Upload Document image");
            MultipartFile multipartFile = MultipartFileUtil.convertToMultipartFile(file, fileType, fileName);
            log.info("Before calling document service {}", documentFeignClient);
            ResponseEntity<ApiResponse<Object>> documentResponse = documentFeignClient.upload(tye, multipartFile);
            log.info("After response document service {}", documentResponse);
            Object objectApiResponse = ExtractApiResponseUtil.extractApiResponse(documentResponse);
            log.info("After document mapper response {}", objectApiResponse);
            Document document = modelMapper.map(objectApiResponse, Document.class);
            log.info("image upload success {}", document);
            return document;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected String saveClaimRequest(ClaimRequestDTO claimRequestDTO, ApplicationUser applicationUser,
                                      Optional<ClaimsDependents> claimsDependents, Treatment treatment,
                                      com.dtech.claim.model.TreatmentCategory treatmentCategory,
                                      ApprovalWorkFlow approvalWorkFlow,
                                      InsuranceStaffCategoryPeriod insuranceYear,
                                      InsuranceDetailsLimit insuranceDetailsLimit, InsuranceQuarter insuranceQuarter) {
        try {
            log.info("Claim request save started {}", claimRequestDTO);

            InsuranceClaimsDetails insuranceClaimsDetails = saveClaimRequestDetails(claimRequestDTO, treatment, treatmentCategory, insuranceYear);

            ClaimRequestIdGen claimRequestIdGen = ClaimRequestIdGen
                    .builder().year(String.valueOf(LocalDate.now().getYear()))
                    .company(applicationUser.getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode())
                    .staffCategory(applicationUser.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode())
                    .build();
            RequestIdGenUtil requestIdGenUtil = new RequestIdGenUtil(true);
            log.info("Generate request id {}", claimRequestIdGen);
            String claimRequestId = (String) requestIdGenUtil.generate(entityManager.unwrap(SharedSessionContractImplementor.class), claimRequestIdGen);
            log.info("after generate request id {}", claimRequestId);
            InsuranceClaimsRequest insuranceClaimsRequest = new InsuranceClaimsRequest();
            insuranceClaimsRequest.setRequestId(claimRequestId);
            insuranceClaimsRequest.setRequestAmount(claimRequestDTO.getRequestAmount());
            insuranceClaimsRequest.setRequestStatus(Workflow.UNDER_REVIEW);
            insuranceClaimsRequest.setRemark(claimRequestDTO.getRemark());
            insuranceClaimsRequest.setClaimsDependents(claimsDependents.orElse(null));
            insuranceClaimsRequest.setEmployee(applicationUser);
            insuranceClaimsRequest.setInsuranceClaimsDetails(insuranceClaimsDetails);
            insuranceClaimsRequest.setApprovalWorkFlows(List.of(approvalWorkFlow));
            insuranceClaimsRequest.setApprovalLevel(ApprovalLevel.LEVEL01);
            insuranceClaimsRequest.setInsuranceDetailsLimit(insuranceDetailsLimit);
            insuranceClaimsRequest.setInsuranceQuarter(insuranceQuarter);
            insuranceClaimsRequestRepository.saveAndFlush(insuranceClaimsRequest);
            log.info("Complete save claim request id {}", claimRequestId);
            return claimRequestId;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
