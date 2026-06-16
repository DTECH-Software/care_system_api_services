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
    private static final int NORMAL_STAFF_PARENT_MAX_CLAIM_AGE = 64;
    private static final int CHILD_MAX_CLAIM_AGE = 24;
    private static final int NORMAL_STAFF_EMPLOYEE_MAX_CLAIM_AGE = 59;
    private static final int OTHER_STAFF_EMPLOYEE_MAX_CLAIM_AGE = 69;
    private static final int NORMAL_STAFF_SPOUSE_MAX_CLAIM_AGE = 59;
    private static final int OTHER_STAFF_SPOUSE_MAX_CLAIM_AGE = 69;


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

    @Autowired
    private final RejoinCarryForwardService rejoinCarryForwardService;

    private static final BigDecimal NS_MAX_CLAIM_AMOUNT = BigDecimal.valueOf(800000);
    private static final int NS_MAX_EMPLOYEE_REQUESTS = 4;

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

                String staffCategoryCode = user.getUserPersonalDetails()
                        .getUserCompanyDetails()
                        .getStaffCategories()
                        .getCode();

                boolean isEligibleForCRICRestriction = Arrays.asList("MM", "EX-01", "EX-02").contains(staffCategoryCode);
                //    boolean isUnmarried = !user.getUserPersonalDetails().isMaritalStatus();
                boolean isForDependent = !claimRequestDTO.getIsEmployee();
                boolean isCRIC = claimRequestDTO.getTreatment().equals(TreatmentType.CRIC.name());

                //   log.info("Married {}", isUnmarried);
                log.info("Dependent {}", isForDependent);

                if (user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy() == null) {
                    log.info("User not eligible to claim request {}", claimRequestDTO.getUsername());
                    return ResponseEntity.ok().body(
                            responseUtil.error(null, 1029,
                                    messageSource.getMessage(ResponseMessageUtil.USER_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale))
                    );
                } else if (isForDependent && isCRIC && isEligibleForCRICRestriction) {
                    log.info("This CRIC facility is not eligible for dependents");
                    return ResponseEntity.ok().body(
                            responseUtil.error(null, 1049,
                                    messageSource.getMessage(ResponseMessageUtil.DEPENDENT_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale))
                    );
                } else if (isForDependent && user.getUserPersonalDetails().getMaritalStatus().equals(MaritalStatus.UNMARRIED) && !staffCategoryCode.equals("NS")) {
                    log.info("Dependent not eligible due to staff category and marital status");
                    return ResponseEntity.ok().body(
                            responseUtil.error(null, 1050,
                                    messageSource.getMessage(ResponseMessageUtil.DEPENDENT_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale))
                    );
                }

//                else if (!user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode().equals("NS")) {
//                    log.info("Senior staff cover age limit exceeded {} ", user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode());
//
//                    int trAge = 70;
//                    if (claimRequestDTO.getTreatment().equals(TreatmentType.CRIC.name()) ||
//                            claimRequestDTO.getTreatment().equals(TreatmentType.LIFC.name()) ||
//                            claimRequestDTO.getTreatment().equals(TreatmentType.ACCD.name()) ||
//                            claimRequestDTO.getTreatment().equals(TreatmentType.TPPD.name()) ||
//                            claimRequestDTO.getTreatment().equals(TreatmentType.PPPD.name())) {
//                        trAge = 65;
//                    }
//
//                    int age = DateTimeUtil.getAge(String.valueOf(user.getUserPersonalDetails().getDob()));
//                    log.info("Senior staff age {} ", age);
//                    if (age > trAge) {
//                        log.info("Senior staff age {} ", age);
//                        return ResponseEntity.ok().body(responseUtil.error(null, 1047, messageSource.getMessage(ResponseMessageUtil.CLAIM_SENIOR_STAFF_AGE_LIMIT_EXCEED, new Object[]{trAge}, locale)));
//                    }
//
//                }

                boolean normalStaff = user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode().equals("NS");

                if (normalStaff) {
                    log.info("Normal staff cover age limit exceeded {} ", user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode());
                    int trAge = NORMAL_STAFF_EMPLOYEE_MAX_CLAIM_AGE;
                    int age = DateTimeUtil.getAge(String.valueOf(user.getUserPersonalDetails().getDob()));
                    log.info("Senior staff age {} ", age);
                    if (age > trAge) {
                        log.info("Senior staff age {} ", age);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1047, messageSource.getMessage(ResponseMessageUtil.CLAIM_SENIOR_STAFF_AGE_LIMIT_EXCEED, new Object[]{trAge + 1}, locale)));
                    }

                } else {
                    log.info("Other staff cover age limit exceeded {} ", user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode());
                    int trAge = OTHER_STAFF_EMPLOYEE_MAX_CLAIM_AGE;
                    int age = DateTimeUtil.getAge(String.valueOf(user.getUserPersonalDetails().getDob()));
                    log.info("Senior staff age {} ", age);
                    if (age > trAge) {
                        log.info("Senior staff age {} ", age);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1047, messageSource.getMessage(ResponseMessageUtil.CLAIM_SENIOR_STAFF_AGE_LIMIT_EXCEED, new Object[]{trAge + 1}, locale)));
                    }
                }

                if (claimRequestDTO.getTreatment().equals(TreatmentType.CRIC.name()) ||
                        claimRequestDTO.getTreatment().equals(TreatmentType.LIFC.name()) ||
                        claimRequestDTO.getTreatment().equals(TreatmentType.ACCD.name()) ||
                        claimRequestDTO.getTreatment().equals(TreatmentType.TPPD.name()) ||
                        claimRequestDTO.getTreatment().equals(TreatmentType.PPPD.name())) {

                    int trAge = resolveEmployeeClaimMaxAge(staffCategoryCode);

                    int age = DateTimeUtil.getAge(String.valueOf(user.getUserPersonalDetails().getDob()));
                    log.info("Senior staff age {} ", age);
                    if (age > trAge) {
                        log.info("Senior staff age {} ", age);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1047, messageSource.getMessage(ResponseMessageUtil.CLAIM_SENIOR_STAFF_AGE_LIMIT_EXCEED, new Object[]{trAge}, locale)));
                    }
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

                                                    if (isParentClaimBlockedForMedical(staffCategoryCode, user.getUserPersonalDetails().getMaritalStatus())
                                                            && claimsDependents.get().getDependentCategory().equals(DependentCategory.PARENTS)) {
                                                        log.info("Parent dependent claim is not allowed for staff category {}", staffCategoryCode);
                                                        return ResponseEntity.ok().body(responseUtil.error(null, 1049, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale)));
                                                    }

                                                    if (user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode().equals("NS") && user.getUserPersonalDetails().getMaritalStatus().equals(MaritalStatus.UNMARRIED) && claimsDependents.get().getDependentCategory().equals(DependentCategory.PARENTS)) {
                                                        int age = DateTimeUtil.getAge(String.valueOf(claimsDependents.get().getDob()));
                                                        log.info("Claim deendent age {} ", age);
                                                        if (age > NORMAL_STAFF_PARENT_MAX_CLAIM_AGE) {
                                                            log.info("Claim deendent age {} ", age);
                                                            return ResponseEntity.ok().body(responseUtil.error(null, 1047, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_INSURANCE_REQUEST_PARENT_AGE_LIMIT_EXCEED, new Object[]{NORMAL_STAFF_PARENT_MAX_CLAIM_AGE}, locale)));
                                                        }
                                                    } else if (claimsDependents.get().getDependentCategory().equals(DependentCategory.CHILDREN)) {
                                                        int age = DateTimeUtil.getAge(String.valueOf(claimsDependents.get().getDob()));
                                                        log.info("Claim deendent child age {} ", age);
                                                        if (age > CHILD_MAX_CLAIM_AGE) {
                                                            log.info("Claim deendent child age {} ", age);
                                                            return ResponseEntity.ok().body(responseUtil.error(null, 1047, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_INSURANCE_REQUEST_CHILDREN_AGE_LIMIT_EXCEED, new Object[]{CHILD_MAX_CLAIM_AGE}, locale)));

                                                        }
                                                    } else if (!isSpouseWithinMedicalAgeLimit(user, claimsDependents.get())) {
                                                        int maxAge = resolveSpouseClaimMaxAge(staffCategoryCode);
                                                        log.info("Spouse dependent claim is not allowed because age reaches or exceeds {}", maxAge + 1);
                                                        return ResponseEntity.ok().body(responseUtil.error(null, 1047, messageSource.getMessage(ResponseMessageUtil.CLAIM_SENIOR_STAFF_AGE_LIMIT_EXCEED, new Object[]{maxAge + 1}, locale)));
                                                    }

                                                } else {
                                                    claimsDependents = Optional.empty();
                                                }
                                                log.info("Gey current year {}", DateTimeUtil.getCurrentYear());
                                                log.info(user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode());
                                                log.info(DateTimeUtil.getCurrentDateTime());
                                                InsuranceStaffCategoryPeriod insuranceYear = insuranceStaffCategoryPeriodRepository.
                                                        findByDateWithinRange(DateTimeUtil.getCurrentDateTime(), user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode())
                                                        .stream()
                                                        .findFirst()
                                                        .orElse(null);
                                                log.info("Current insurance year {}", insuranceYear);
                                                if (insuranceYear == null) {
                                                    log.info("Insurance period not found");
                                                    return ResponseEntity.ok().body(responseUtil.error(null, 1057, messageSource.getMessage(ResponseMessageUtil.INSURANCE_PERIOD_NOT_FOUND, null, locale)));
                                                }

                                                InsuranceStaffCategoryPeriod previousCategoryPeriod = resolvePreviousPeriodForCarry(
                                                        user,
                                                        insuranceYear,
                                                        claimRequestDTO.getTreatment());
                                                BigDecimal sumOfClaims = rejoinCarryForwardService
                                                        .getApprovedAmountByTreatment(
                                                                user,
                                                                claimRequestDTO.getTreatment(),
                                                                insuranceYear.getId(),
                                                                previousCategoryPeriod
                                                        );
                                                Long currentPolicyId = policy.getId();

                                                log.info("Already claims {} sum of claims ", sumOfClaims);

                                                //   if (currentYear == year) {
                                                log.info("Request fund limit exceeded with ent limit");
                                                Date permanentDate = rejoinCarryForwardService
                                                        .resolveEffectivePermanentDateForLimit(user);
                                                Date quarterLookupDate = permanentDate != null ? permanentDate : DateTimeUtil.getCurrentDateTime();
                                                log.info("Claim quarter lookup date {}", quarterLookupDate);
                                                 List<InsuranceDetailsLimit> insuranceDetailsLimits = insuranceDetailsLimitRepository
                                                         .findAllByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment_TreatmentCode(
                                                                 user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy(),
                                                                 Status.ACTIVE,
                                                                 insuranceYear,
                                                                 treatment.getTreatmentCode()
                                                         );
                                                 InsuranceDetailsLimit insuranceDetailsLimit = resolveInsuranceDetailsLimitForCategory(
                                                         insuranceDetailsLimits,
                                                         claimRequestDTO.getTreatmentCategory(),
                                                         quarterLookupDate
                                                 );

                                                 if (insuranceDetailsLimit == null) {
                                                     log.info("Insurance detail is null");
                                                     return ResponseEntity.ok().body(responseUtil.error(null, 1030, messageSource.getMessage(ResponseMessageUtil.INSURANCE_POLICY_NOT_FOUND, null, locale)));
                                                 }

                                                Map<String, AvailableInsuranceLimitDTO> categoryLimitMap = buildCategoryAvailableLimitMap(
                                                        insuranceDetailsLimit,
                                                        user,
                                                        insuranceYear.getId(),
                                                        previousCategoryPeriod,
                                                        quarterLookupDate,
                                                        claimRequestDTO.getTreatmentCategory()
                                                );

                                                if (claimRequestDTO.getTreatmentCategory().equals(TreatmentCategory.OTHER.name())) {

                                                    AvailableInsuranceLimitDTO requestedLimit = categoryLimitMap.get(TreatmentCategory.OTHER.name());
                                                    if (requestedLimit == null) {
                                                        log.info("Insurance detail quarter is null for category OTHER");
                                                        return ResponseEntity.ok().body(responseUtil.error(null, 1030, messageSource.getMessage(ResponseMessageUtil.INSURANCE_POLICY_NOT_FOUND, null, locale)));
                                                    }

                                                    BigDecimal limit = requestedLimit.getFundLimit();
                                                    BigDecimal remainingBalance = requestedLimit.getAvailableLimit();
                                                    InsuranceQuarter treatmentQuarter = null;
                                                    if (insuranceDetailsLimit.getIsQuarter()) {
                                                        treatmentQuarter = resolveApplicableQuarter(insuranceDetailsLimit, TreatmentCategory.OTHER.name(), quarterLookupDate);
                                                        limit = treatmentQuarter != null ? treatmentQuarter.getQuarterLimit() : limit;
                                                    }

                                                    if (isCRIC && "NS".equals(staffCategoryCode)) {
                                                        log.info("Dependent eligible due to CRIC {} ", sumOfClaims);
                                                        int requestEmp = insuranceClaimsRequestRepository.
                                                                countByInsuranceClaimsDetails_Treatment_TreatmentCodeAndInsuranceClaimsDetails_InsuranceStaffCategoryPeriod_IdAndInsuranceDetailsLimit_InsurancePolicy_IdAndRequestStatusIn(
                                                                        TreatmentType.CRIC.name(),
                                                                        insuranceYear.getId(),
                                                                        currentPolicyId,
                                                                        List.of(Workflow.APPROVED));
                                                        log.info("Dependent not eligible due to CRIC {} {}", sumOfClaims, requestEmp);

                                                        boolean exists = insuranceClaimsRequestRepository.
                                                                existsByEmployeeAndInsuranceClaimsDetails_Treatment_TreatmentCodeAndInsuranceClaimsDetails_InsuranceStaffCategoryPeriod_IdAndInsuranceDetailsLimit_InsurancePolicy_IdAndRequestStatus(
                                                                        user,
                                                                        TreatmentType.CRIC.name(),
                                                                        insuranceYear.getId(),
                                                                        currentPolicyId,
                                                                        Workflow.APPROVED);

                                                        if (requestEmp >= NS_MAX_EMPLOYEE_REQUESTS
                                                                || exists
                                                                || claimRequestDTO.getRequestAmount().compareTo(NS_MAX_CLAIM_AMOUNT) > 0) {

                                                            return ResponseEntity.ok().body(
                                                                    responseUtil.error(
                                                                            null,
                                                                            1034,
                                                                            messageSource.getMessage(
                                                                                    ResponseMessageUtil.STAFF_CLAIM_LIMIT_OR_OUT_OF_EMPLOYEE_REQUEST_EXCEED,
                                                                                    null,
                                                                                    locale
                                                                            )
                                                                    )
                                                            );
                                                        }
                                                        limit = NS_MAX_CLAIM_AMOUNT;
                                                        remainingBalance = limit.subtract(sumOfClaims != null ? sumOfClaims : BigDecimal.ZERO);
                                                    } else if (isCRIC && "SNR".equals(staffCategoryCode)) {
                                                        log.info("Dependent eligible due to CRIC {} ", sumOfClaims);
                                                        int age = DateTimeUtil.getAge(String.valueOf(user.getUserPersonalDetails().getDob()));
                                                        if (age > OTHER_STAFF_EMPLOYEE_MAX_CLAIM_AGE) {
                                                            log.info("Cant snr cri request");
                                                            return ResponseEntity.ok().body(responseUtil.error(null, 1052, messageSource.getMessage(ResponseMessageUtil.SENIOR_STAFF_CANT_REQUEST_UP_TO_60_AGE, new Object[]{OTHER_STAFF_EMPLOYEE_MAX_CLAIM_AGE}, locale)));

                                                        }

                                                        int requestEmp = insuranceClaimsRequestRepository.
                                                                countByInsuranceClaimsDetails_Treatment_TreatmentCodeAndInsuranceClaimsDetails_InsuranceStaffCategoryPeriod_IdAndInsuranceDetailsLimit_InsurancePolicy_IdAndRequestStatusInAndEmployee(
                                                                        TreatmentType.CRIC.name(),
                                                                        insuranceYear.getId(),
                                                                        currentPolicyId,
                                                                        List.of(Workflow.APPROVED),
                                                                        user);
                                                        log.info("Dependent not eligible due to CRIC {} {}", sumOfClaims, requestEmp);

                                                        BigDecimal safeSumOfClaims = sumOfClaims == null ? BigDecimal.ZERO : sumOfClaims;

                                                        if (requestEmp > 4
                                                                || safeSumOfClaims.compareTo(BigDecimal.valueOf(2000000)) > 0
                                                                || claimRequestDTO.getRequestAmount().compareTo(BigDecimal.valueOf(500000)) > 0) {

                                                            log.info("Invalid claim limit {} {} ", safeSumOfClaims, requestEmp);
                                                            return ResponseEntity.ok().body(
                                                                    responseUtil.error(
                                                                            null,
                                                                            1034,
                                                                            messageSource.getMessage(
                                                                                    ResponseMessageUtil.STAFF_CLAIM_LIMIT_OR_OUT_OF_EMPLOYEE_REQUEST_EXCEED,
                                                                                    null,
                                                                                    locale
                                                                            )
                                                                    )
                                                            );
                                                        }


                                                    }

                                                    if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                                                        remainingBalance = BigDecimal.ZERO;
                                                    }
                                                    log.info("Remaining balance {}", remainingBalance);
                                                    if (claimRequestDTO.getRequestAmount().compareTo(remainingBalance) > 0) {
                                                        log.info("Request fund limit exceeded with ent limit {} {} {} {}", claimRequestDTO.getRequestAmount(), limit, remainingBalance, sumOfClaims);
                                                        return ResponseEntity.ok().body(responseUtil.error(null, 1052, messageSource.getMessage(ResponseMessageUtil.CLAIM_LIMIT_EXCEED_WITH_LIMIT, new Object[]{remainingBalance}, locale)));
                                                    }

                                                    if ("NS".equals(user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode())) {
                                                        int dob = DateTimeUtil.getAge(String.valueOf(user.getUserPersonalDetails().getDob()));
                                                        if (dob > NORMAL_STAFF_EMPLOYEE_MAX_CLAIM_AGE) {
                                                            log.info("User normal staff category {} ", dob);
                                                            return ResponseEntity.ok().body(responseUtil.error(null, 1052, messageSource.getMessage(ResponseMessageUtil.SENIOR_STAFF_CANT_REQUEST_UP_TO_60_AGE, new Object[]{NORMAL_STAFF_EMPLOYEE_MAX_CLAIM_AGE}, locale)));
                                                        }
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

                                                                String messageUtil = ResponseMessageUtil.INSURANCE_CLAIM_DEFAULT_REQUEST_SUBMIT_SUCCESS;

                                                                if ((!user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode().equals("NS")) && claimRequestDTO.getTreatment().equals(TreatmentType.INDOOR.name()) ||
                                                                        claimRequestDTO.getTreatment().equals(TreatmentType.CRIC.name()) ||
                                                                        claimRequestDTO.getTreatment().equals(TreatmentType.LIFC.name()) ||
                                                                        claimRequestDTO.getTreatment().equals(TreatmentType.TPPD.name()) ||
                                                                        claimRequestDTO.getTreatment().equals(TreatmentType.PPPD.name()) ||
                                                                        claimRequestDTO.getTreatment().equals(TreatmentType.ACCD.name())) {

                                                                    messageUtil = ResponseMessageUtil.INSURANCE_CLAIM_SPECIAL_SUBMIT_SUCCESS;
                                                                }
                                                                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(messageUtil, null, locale)));
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

                                                    AvailableInsuranceLimitDTO requestedLimit = categoryLimitMap.get(claimRequestDTO.getTreatmentCategory());
                                                    if (requestedLimit == null) {
                                                        log.info("Insurance detail quarter/global limit is null");
                                                        return ResponseEntity.ok().body(responseUtil.error(null, 1030, messageSource.getMessage(ResponseMessageUtil.INSURANCE_POLICY_NOT_FOUND, null, locale)));
                                                    }
                                                    InsuranceQuarter treatmentQuarter = resolveApplicableQuarter(
                                                            insuranceDetailsLimit,
                                                            claimRequestDTO.getTreatmentCategory(),
                                                            quarterLookupDate
                                                    );
                                                    BigDecimal categoryLimit = requestedLimit.getFundLimit();
                                                    BigDecimal remainingBalance = requestedLimit.getAvailableLimit();

                                                    if (isCRIC && "NS".equals(staffCategoryCode)) {
                                                        log.info("Dependent eligible due to CRIC {} ", sumOfClaims);
                                                        int requestEmp = insuranceClaimsRequestRepository.
                                                                countByInsuranceClaimsDetails_Treatment_TreatmentCodeAndInsuranceClaimsDetails_InsuranceStaffCategoryPeriod_IdAndInsuranceDetailsLimit_InsurancePolicy_IdAndRequestStatusIn(
                                                                        TreatmentType.CRIC.name(),
                                                                        insuranceYear.getId(),
                                                                        currentPolicyId,
                                                                        List.of(Workflow.APPROVED));
                                                        log.info("Dependent not eligible due to CRIC {} {}", sumOfClaims, requestEmp);

                                                        boolean exists = insuranceClaimsRequestRepository.
                                                                existsByEmployeeAndInsuranceClaimsDetails_Treatment_TreatmentCodeAndInsuranceClaimsDetails_InsuranceStaffCategoryPeriod_IdAndInsuranceDetailsLimit_InsurancePolicy_IdAndRequestStatus(
                                                                        user,
                                                                        TreatmentType.CRIC.name(),
                                                                        insuranceYear.getId(),
                                                                        currentPolicyId,
                                                                        Workflow.APPROVED);

                                                        if (requestEmp >= NS_MAX_EMPLOYEE_REQUESTS
                                                                || exists
                                                                || claimRequestDTO.getRequestAmount().compareTo(NS_MAX_CLAIM_AMOUNT) > 0) {

                                                            return ResponseEntity.ok().body(
                                                                    responseUtil.error(
                                                                            null,
                                                                            1034,
                                                                            messageSource.getMessage(
                                                                                    ResponseMessageUtil.STAFF_CLAIM_LIMIT_OR_OUT_OF_EMPLOYEE_REQUEST_EXCEED,
                                                                                    null,
                                                                                    locale
                                                                            )
                                                                    )
                                                            );
                                                        }
                                                        insuranceDetailsLimit.setGlobalLimit(NS_MAX_CLAIM_AMOUNT);
                                                        sumOfClaims = null;
                                                    }

                                                    log.info("Remaining balance {}", remainingBalance);
                                                    if (claimRequestDTO.getRequestAmount().compareTo(remainingBalance) > 0) {
                                                        log.info("Request fund limit exceeded without event limit {} {} {} {}", claimRequestDTO.getRequestAmount(), categoryLimit, remainingBalance, sumOfClaims);
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

                                                                String messageUtil = ResponseMessageUtil.INSURANCE_CLAIM_DEFAULT_REQUEST_SUBMIT_SUCCESS;

                                                                if ((!user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode().equals("NS")) && claimRequestDTO.getTreatment().equals(TreatmentType.INDOOR.name()) ||
                                                                        claimRequestDTO.getTreatment().equals(TreatmentType.CRIC.name()) ||
                                                                        claimRequestDTO.getTreatment().equals(TreatmentType.LIFC.name()) ||
                                                                        claimRequestDTO.getTreatment().equals(TreatmentType.TPPD.name()) ||
                                                                        claimRequestDTO.getTreatment().equals(TreatmentType.PPPD.name()) ||
                                                                        claimRequestDTO.getTreatment().equals(TreatmentType.ACCD.name())) {

                                                                    messageUtil = ResponseMessageUtil.INSURANCE_CLAIM_SPECIAL_SUBMIT_SUCCESS;
                                                                }

                                                                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(messageUtil, null, locale)));
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
                InsuranceStaffCategoryPeriod period = insuranceStaffCategoryPeriodRepository
                        .findByDateWithinRange(DateTimeUtil.getCurrentDateTime(), user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode())
                        .stream()
                        .findFirst()
                        .orElse(null);
                List<Treatment> treatmentList = treatmentRepository.findAllByStatus(Status.ACTIVE);
                log.info("Insurance claim reference data get dependence {} ", treatmentList);

                List<DependentBaseDTO> collect = claimDependentsRepository
                        .findByApplicationUserAndStatusAndEligibleFacilityInAndLiveStatus(user, Workflow.APPROVED, Arrays.asList(Facility.INSURANCE, Facility.BOTH), true).
                        stream()
                        .filter(dep -> {
                            log.info("SUCCESS FILTER {} ", dep.getId());
                            boolean ex = deathClaimRequestRepository.existsByClaimsDependentsAndEmployeeAndRequestStatusIn(dep, user, List.of(Workflow.APPROVED));
                            return !ex;
                        })
                        .filter(dep -> !isParentClaimBlockedForMedical(
                                user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode(),
                                user.getUserPersonalDetails().getMaritalStatus())
                                || !dep.getDependentCategory().equals(DependentCategory.PARENTS))
                        .filter(dep -> isParentWithinNormalStaffMedicalAgeLimit(user, dep))
                        .filter(this::isChildWithinMedicalAgeLimit)
                        .filter(dep -> isSpouseWithinMedicalAgeLimit(user, dep))
                        .map(dep -> new DependentBaseDTO(
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

//                    List<InsuranceDetailsLimit> insuranceDetailsLimits = insuranceDetailsLimitRepository.
//                            findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriod(
//                                    user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy(),
//                                    Status.ACTIVE, period);

                    List<InsuranceDetailsLimit> insuranceDetailsLimits = insuranceDetailsLimitRepository
                            .findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriod(
                                    user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy(),
                                    Status.ACTIVE,
                                    period
                            );
                    log.info("Insurance claim reference data periodId={}, limitCount={}",
                            period.getId(),
                            insuranceDetailsLimits.size());

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

            String treatmentCode = insuranceDetailsLimit.getTreatment().getTreatmentCode();
            Long insurancePeriod = insuranceDetailsLimit.getInsuranceStaffCategoryPeriod().getId();
            Date currentDate = DateTimeUtil.getCurrentDateTime();
            Date permanentDate = rejoinCarryForwardService.resolveEffectivePermanentDateForLimit(applicationUser);
            Date quarterLookupDate = permanentDate != null ? permanentDate : currentDate;

            InsuranceStaffCategoryPeriod currentPeriod = insuranceDetailsLimit.getInsuranceStaffCategoryPeriod();
            log.info("CLAIM_REF_LIMIT currentPeriodId={}, staffCategory={}",
                    insurancePeriod,
                    currentPeriod != null && currentPeriod.getStaffCategories() != null
                            ? currentPeriod.getStaffCategories().getCode()
                            : null);
            Date previousPermanentDate = applicationUser.getUserPersonalDetails()
                    .getUserCompanyDetails()
                    .getPreviousPermanentDate();
            Date changeDate = previousPermanentDate != null
                    ? applicationUser.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate()
                    : null;
            InsuranceStaffCategoryPeriod prevPeriod = resolvePreviousPeriodFromClaimHistory(
                    applicationUser,
                    treatmentCode,
                    currentPeriod);
            if (prevPeriod == null
                    && changeDate != null
                    && currentPeriod != null
                    && currentPeriod.getStaffCategories() != null) {
                prevPeriod = insuranceStaffCategoryPeriodRepository
                        .findByDateWithinRangeAnyStaff(changeDate)
                        .stream()
                        .filter(p -> p.getStaffCategories() != null)
                        .filter(p -> !p.getStaffCategories().getCode()
                                .equals(currentPeriod.getStaffCategories().getCode()))
                        .findFirst()
                        .orElse(null);
                if (prevPeriod != null && prevPeriod.getStaffCategories() != null) {
                    log.info("CLAIM_REF_LIMIT prevPeriodId={}, prevStaffCategory={}, prevFrom={}, prevTo={}",
                            prevPeriod.getId(),
                            prevPeriod.getStaffCategories().getCode(),
                            prevPeriod.getFromDate(),
                            prevPeriod.getToDate());
                } else {
                    log.info("CLAIM_REF_LIMIT prevPeriod=NONE or same staff category");
                }
            }

            Map<String, AvailableInsuranceLimitDTO> categoryLimitMap = buildCategoryAvailableLimitMap(
                    insuranceDetailsLimit,
                    applicationUser,
                    insurancePeriod,
                    prevPeriod,
                    quarterLookupDate,
                    null
            );

            for (Map.Entry<String, AvailableInsuranceLimitDTO> entry : categoryLimitMap.entrySet()) {
                        limitMap
                        .computeIfAbsent(treatmentCode, k -> new HashMap<>())
                        .merge(entry.getKey(), entry.getValue(),
                                (existing, newDetails) -> new AvailableInsuranceLimitDTO(
                                        minAmount(existing.getAvailableLimit(), newDetails.getAvailableLimit()),
                                        minAmount(existing.getFundLimit(), newDetails.getFundLimit())
                                ));
            }
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private boolean isParentClaimBlockedForMedical(String staffCategoryCode, MaritalStatus maritalStatus) {
        return Set.of("EX-OP1", "EX-OP2", "MM", "SNR").contains(staffCategoryCode)
                || ("NS".equals(staffCategoryCode) && MaritalStatus.MARRIED.equals(maritalStatus));
    }

    private boolean isParentWithinNormalStaffMedicalAgeLimit(ApplicationUser user, ClaimsDependents dependent) {
        if (user == null
                || dependent == null
                || !DependentCategory.PARENTS.equals(dependent.getDependentCategory())
                || user.getUserPersonalDetails() == null
                || user.getUserPersonalDetails().getUserCompanyDetails() == null
                || user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories() == null) {
            return true;
        }
        String staffCategoryCode = user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode();
        if (!"NS".equalsIgnoreCase(staffCategoryCode)
                || !MaritalStatus.UNMARRIED.equals(user.getUserPersonalDetails().getMaritalStatus())) {
            return true;
        }
        int age = DateTimeUtil.getAge(String.valueOf(dependent.getDob()));
        return age <= NORMAL_STAFF_PARENT_MAX_CLAIM_AGE;
    }

    private boolean isChildWithinMedicalAgeLimit(ClaimsDependents dependent) {
        if (dependent == null || !DependentCategory.CHILDREN.equals(dependent.getDependentCategory())) {
            return true;
        }
        int age = DateTimeUtil.getAge(String.valueOf(dependent.getDob()));
        return age <= CHILD_MAX_CLAIM_AGE;
    }

    private boolean isSpouseWithinMedicalAgeLimit(ApplicationUser user, ClaimsDependents dependent) {
        if (user == null || dependent == null || !DependentCategory.SPOUSE.equals(dependent.getDependentCategory())) {
            return true;
        }
        String staffCategoryCode = user.getUserPersonalDetails()
                .getUserCompanyDetails()
                .getStaffCategories()
                .getCode();
        int age = DateTimeUtil.getAge(String.valueOf(dependent.getDob()));
        return age <= resolveSpouseClaimMaxAge(staffCategoryCode);
    }

    private int resolveSpouseClaimMaxAge(String staffCategoryCode) {
        return "NS".equals(staffCategoryCode)
                ? NORMAL_STAFF_SPOUSE_MAX_CLAIM_AGE
                : OTHER_STAFF_SPOUSE_MAX_CLAIM_AGE;
    }

    private int resolveEmployeeClaimMaxAge(String staffCategoryCode) {
        return "NS".equals(staffCategoryCode)
                ? NORMAL_STAFF_EMPLOYEE_MAX_CLAIM_AGE
                : OTHER_STAFF_EMPLOYEE_MAX_CLAIM_AGE;
    }

    private Map<String, AvailableInsuranceLimitDTO> buildCategoryAvailableLimitMap(InsuranceDetailsLimit insuranceDetailsLimit,
                                                                                    ApplicationUser applicationUser,
                                                                                    Long insurancePeriod,
                                                                                    InsuranceStaffCategoryPeriod prevPeriod,
                                                                                    Date quarterLookupDate,
                                                                                    String requiredCategoryCode) {
        Map<String, CategoryLimitContext> categoryContextMap = new LinkedHashMap<>();
        String treatmentCode = insuranceDetailsLimit.getTreatment().getTreatmentCode();
        List<InsuranceDetailsLimit> matchingDetailsLimits = resolveMatchingInsuranceDetailsLimits(insuranceDetailsLimit);
        Set<String> treatmentCategoryCodes = collectCategoryCodes(matchingDetailsLimits, requiredCategoryCode);

        for (String categoryCode : treatmentCategoryCodes) {
            InsuranceDetailsLimit categoryLimitSource = resolveInsuranceDetailsLimitForCategory(
                    matchingDetailsLimits,
                    categoryCode,
                    quarterLookupDate
            );
            if (categoryLimitSource == null) {
                continue;
            }

            InsuranceQuarter categoryQuarter = resolveApplicableQuarter(categoryLimitSource, categoryCode, quarterLookupDate);
            BigDecimal fundLimit = resolveCategoryFundLimit(categoryLimitSource, categoryQuarter);
            if (fundLimit == null) {
                continue;
            }

            BigDecimal approvedAmount = getCategoryApprovedAmount(
                    applicationUser,
                    treatmentCode,
                    categoryCode,
                    insurancePeriod,
                    prevPeriod
            );
            categoryContextMap.put(categoryCode, new CategoryLimitContext(fundLimit, approvedAmount));
        }

        if (categoryContextMap.isEmpty()) {
            return Collections.emptyMap();
        }

        BigDecimal treatmentFundLimit = resolveTreatmentFundLimit(insuranceDetailsLimit, categoryContextMap);
        BigDecimal directTreatmentApprovedAmount = rejoinCarryForwardService.getApprovedAmountByTreatment(
                applicationUser,
                treatmentCode,
                insurancePeriod,
                prevPeriod
        );
        BigDecimal categoryApprovedTotal = treatmentCategoryCodes.stream()
                .map(categoryCode -> getCategoryApprovedAmount(
                        applicationUser,
                        treatmentCode,
                        categoryCode,
                        insurancePeriod,
                        prevPeriod
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal treatmentApprovedAmount = directTreatmentApprovedAmount.max(categoryApprovedTotal);
        if (categoryApprovedTotal.compareTo(directTreatmentApprovedAmount) > 0) {
            log.info("CLAIM_REF_LIMIT using category aggregate for treatment {}. direct={}, categoryTotal={}",
                    treatmentCode, directTreatmentApprovedAmount, categoryApprovedTotal);
        }
        BigDecimal treatmentRemainingAmount = subtractToZero(treatmentFundLimit, treatmentApprovedAmount);
        log.info("CLAIM_REF_DEBUG user={}, treatment={}, periodId={}, prevPeriodId={}, directTreatmentApproved={}, categoryApprovedTotal={}, treatmentApproved={}, treatmentFundLimit={}, treatmentRemaining={}",
                applicationUser != null ? applicationUser.getUsername() : null,
                treatmentCode,
                insurancePeriod,
                prevPeriod != null ? prevPeriod.getId() : null,
                directTreatmentApprovedAmount,
                categoryApprovedTotal,
                treatmentApprovedAmount,
                treatmentFundLimit,
                treatmentRemainingAmount);

        Map<String, AvailableInsuranceLimitDTO> availableLimitMap = new LinkedHashMap<>();
        for (Map.Entry<String, CategoryLimitContext> entry : categoryContextMap.entrySet()) {
            BigDecimal categoryRemainingAmount = subtractToZero(
                    entry.getValue().getFundLimit(),
                    entry.getValue().getApprovedAmount()
            );
            BigDecimal availableAmount = treatmentRemainingAmount.min(categoryRemainingAmount);
            log.info("CLAIM_REF_DEBUG_CATEGORY user={}, treatment={}, category={}, categoryFundLimit={}, categoryApproved={}, categoryRemaining={}, available={}",
                    applicationUser != null ? applicationUser.getUsername() : null,
                    treatmentCode,
                    entry.getKey(),
                    entry.getValue().getFundLimit(),
                    entry.getValue().getApprovedAmount(),
                    categoryRemainingAmount,
                    availableAmount);

            availableLimitMap.put(
                    entry.getKey(),
                    new AvailableInsuranceLimitDTO(
                            availableAmount,
                            entry.getValue().getFundLimit()
                    )
            );
        }

        return availableLimitMap;
    }

    private BigDecimal resolveTreatmentFundLimit(InsuranceDetailsLimit insuranceDetailsLimit,
                                                 Map<String, CategoryLimitContext> categoryContextMap) {
        if (!Boolean.TRUE.equals(insuranceDetailsLimit.getIsQuarter())
                && insuranceDetailsLimit.getGlobalLimit() != null) {
            return insuranceDetailsLimit.getGlobalLimit();
        }

        return categoryContextMap.values().stream()
                .map(CategoryLimitContext::getFundLimit)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal subtractToZero(BigDecimal fundLimit, BigDecimal usedAmount) {
        BigDecimal safeFundLimit = fundLimit != null ? fundLimit : BigDecimal.ZERO;
        BigDecimal safeUsedAmount = usedAmount != null ? usedAmount : BigDecimal.ZERO;
        BigDecimal remainingAmount = safeFundLimit.subtract(safeUsedAmount);
        return remainingAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remainingAmount;
    }

    private BigDecimal minAmount(BigDecimal first, BigDecimal second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.min(second);
    }

    private BigDecimal getCategoryApprovedAmount(ApplicationUser applicationUser,
                                                 String treatmentCode,
                                                 String categoryCode,
                                                 Long insurancePeriod,
                                                 InsuranceStaffCategoryPeriod prevPeriod) {
        return rejoinCarryForwardService.getApprovedAmountByTreatmentCategory(
                applicationUser,
                treatmentCode,
                categoryCode,
                insurancePeriod,
                prevPeriod
        );
    }

    private InsuranceQuarter resolveApplicableQuarter(InsuranceDetailsLimit insuranceDetailsLimit,
                                                      String categoryCode,
                                                      Date lookupDate) {
        InsuranceQuarter matchingQuarter = insuranceQuarterRepository
                .findByDateWithinRangeAndCodeWithLimit(insuranceDetailsLimit, categoryCode, lookupDate)
                .stream()
                .findFirst()
                .orElse(null);
        if (matchingQuarter != null) {
            return matchingQuarter;
        }

        InsuranceQuarter firstQuarter = insuranceQuarterRepository
                .findFirstByInsuranceDetailsLimitAndTreatmentCategory_CodeOrderByFromDateAsc(
                        insuranceDetailsLimit,
                        categoryCode
                ).orElse(null);
        if (firstQuarter == null || lookupDate == null || firstQuarter.getFromDate() == null) {
            return null;
        }

        return lookupDate.before(firstQuarter.getFromDate()) ? firstQuarter : null;
    }

    private BigDecimal resolveCategoryFundLimit(InsuranceDetailsLimit insuranceDetailsLimit,
                                                InsuranceQuarter insuranceQuarter) {
        if (!insuranceDetailsLimit.getIsQuarter()) {
            return insuranceDetailsLimit.getGlobalLimit();
        }
        return insuranceQuarter != null ? insuranceQuarter.getQuarterLimit() : null;
    }

    private InsuranceStaffCategoryPeriod resolvePreviousPeriodForCarry(ApplicationUser applicationUser,
                                                                       InsuranceStaffCategoryPeriod currentPeriod,
                                                                       String treatmentCode) {
        Date previousPermanentDate = applicationUser.getUserPersonalDetails()
                .getUserCompanyDetails()
                .getPreviousPermanentDate();
        Date changeDate = previousPermanentDate != null
                ? applicationUser.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate()
                : null;
        if (currentPeriod == null
                || currentPeriod.getStaffCategories() == null) {
            return null;
        }

        InsuranceStaffCategoryPeriod previousPeriod = resolvePreviousPeriodFromClaimHistory(
                applicationUser,
                treatmentCode,
                currentPeriod);
        if (previousPeriod == null && changeDate != null) {
            previousPeriod = insuranceStaffCategoryPeriodRepository
                    .findByDateWithinRangeAnyStaff(changeDate)
                    .stream()
                    .filter(p -> p.getStaffCategories() != null)
                    .filter(p -> !p.getStaffCategories().getCode()
                            .equals(currentPeriod.getStaffCategories().getCode()))
                    .findFirst()
                    .orElse(null);
        }
        return previousPeriod;
    }

    private InsuranceStaffCategoryPeriod resolvePreviousPeriodFromClaimHistory(ApplicationUser applicationUser,
                                                                              String treatmentCode,
                                                                              InsuranceStaffCategoryPeriod currentPeriod) {
        if (applicationUser == null
                || currentPeriod == null
                || currentPeriod.getStaffCategories() == null
                || treatmentCode == null) {
            return null;
        }

        String currentStaffCode = currentPeriod.getStaffCategories().getCode();
        return insuranceClaimsRequestRepository
                .findAllByEmployeeAndRequestStatusIn(applicationUser, List.of(Workflow.APPROVED))
                .stream()
                .filter(claim -> claim.getInsuranceClaimsDetails() != null)
                .filter(claim -> claim.getInsuranceClaimsDetails().getTreatment() != null)
                .filter(claim -> treatmentCode.equalsIgnoreCase(
                        claim.getInsuranceClaimsDetails().getTreatment().getTreatmentCode()))
                .map(this::resolveClaimPeriod)
                .filter(Objects::nonNull)
                .filter(claimPeriod -> claimPeriod.getId() != null && currentPeriod.getId() != null)
                .filter(claimPeriod -> !claimPeriod.getId().equals(currentPeriod.getId()))
                .filter(claimPeriod -> claimPeriod.getStaffCategories() != null)
                .filter(claimPeriod -> !currentStaffCode.equals(claimPeriod.getStaffCategories().getCode()))
                .filter(claimPeriod -> isOverlappingPeriod(claimPeriod, currentPeriod))
                .max(Comparator.comparing(InsuranceStaffCategoryPeriod::getFromDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private InsuranceStaffCategoryPeriod resolveClaimPeriod(InsuranceClaimsRequest claim) {
        if (claim.getInsuranceDetailsLimit() != null
                && claim.getInsuranceDetailsLimit().getInsuranceStaffCategoryPeriod() != null) {
            return claim.getInsuranceDetailsLimit().getInsuranceStaffCategoryPeriod();
        }
        if (claim.getInsuranceClaimsDetails() != null) {
            return claim.getInsuranceClaimsDetails().getInsuranceStaffCategoryPeriod();
        }
        return null;
    }

    private boolean isOverlappingPeriod(InsuranceStaffCategoryPeriod candidate,
                                        InsuranceStaffCategoryPeriod currentPeriod) {
        if (candidate.getFromDate() == null || candidate.getToDate() == null
                || currentPeriod.getFromDate() == null || currentPeriod.getToDate() == null) {
            return true;
        }
        return !candidate.getToDate().before(currentPeriod.getFromDate())
                && !candidate.getFromDate().after(currentPeriod.getToDate());
    }

    private List<InsuranceDetailsLimit> resolveMatchingInsuranceDetailsLimits(InsuranceDetailsLimit insuranceDetailsLimit) {
        List<InsuranceDetailsLimit> matchingDetailsLimits = insuranceDetailsLimitRepository
                .findAllByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment_TreatmentCode(
                        insuranceDetailsLimit.getInsurancePolicy(),
                        Status.ACTIVE,
                        insuranceDetailsLimit.getInsuranceStaffCategoryPeriod(),
                        insuranceDetailsLimit.getTreatment().getTreatmentCode()
                );
        if (matchingDetailsLimits == null || matchingDetailsLimits.isEmpty()) {
            return List.of(insuranceDetailsLimit);
        }
        return matchingDetailsLimits;
    }

    private InsuranceDetailsLimit resolveInsuranceDetailsLimitForCategory(List<InsuranceDetailsLimit> insuranceDetailsLimits,
                                                                          String categoryCode,
                                                                          Date lookupDate) {
        if (insuranceDetailsLimits == null || insuranceDetailsLimits.isEmpty()) {
            return null;
        }

        if (categoryCode != null && !categoryCode.isBlank()) {
            for (InsuranceDetailsLimit insuranceDetailsLimit : insuranceDetailsLimits) {
                InsuranceQuarter quarter = resolveApplicableQuarter(insuranceDetailsLimit, categoryCode, lookupDate);
                if (quarter != null) {
                    return insuranceDetailsLimit;
                }
            }

            for (InsuranceDetailsLimit insuranceDetailsLimit : insuranceDetailsLimits) {
                boolean categoryExists = insuranceDetailsLimit.getInsuranceQuarters() != null
                        && insuranceDetailsLimit.getInsuranceQuarters().stream()
                        .filter(Objects::nonNull)
                        .filter(quarter -> quarter.getTreatmentCategory() != null)
                        .anyMatch(quarter -> categoryCode.equalsIgnoreCase(quarter.getTreatmentCategory().getCode()));
                if (categoryExists) {
                    return insuranceDetailsLimit;
                }
            }
        }

        return insuranceDetailsLimits.get(0);
    }

    private Set<String> collectCategoryCodes(List<InsuranceDetailsLimit> insuranceDetailsLimits,
                                             String requiredCategoryCode) {
        Set<String> categoryCodes = new LinkedHashSet<>();
        if (insuranceDetailsLimits != null) {
            for (InsuranceDetailsLimit insuranceDetailsLimit : insuranceDetailsLimits) {
                if (insuranceDetailsLimit.getInsuranceQuarters() == null) {
                    continue;
                }
                for (InsuranceQuarter insuranceQuarter : insuranceDetailsLimit.getInsuranceQuarters()) {
                    if (insuranceQuarter == null || insuranceQuarter.getTreatmentCategory() == null) {
                        continue;
                    }
                    categoryCodes.add(insuranceQuarter.getTreatmentCategory().getCode());
                }
            }
        }
        if (requiredCategoryCode != null && !requiredCategoryCode.isBlank()) {
            categoryCodes.add(requiredCategoryCode);
        }
        if (categoryCodes.isEmpty()) {
            categoryCodes.add(TreatmentCategory.OTHER.name());
        }
        return categoryCodes;
    }

    private static final class CategoryLimitContext {
        private final BigDecimal fundLimit;
        private final BigDecimal approvedAmount;

        private CategoryLimitContext(BigDecimal fundLimit, BigDecimal approvedAmount) {
            this.fundLimit = fundLimit;
            this.approvedAmount = approvedAmount != null ? approvedAmount : BigDecimal.ZERO;
        }

        private BigDecimal getFundLimit() {
            return fundLimit;
        }

        private BigDecimal getApprovedAmount() {
            return approvedAmount;
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
