/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 9:56 AM
 * <p>
 */

package com.dtech.claim.service.impl;


import com.dtech.claim.dto.AvailableInsuranceLimitDTO;
import com.dtech.claim.dto.SimpleBaseDTO;
import com.dtech.claim.enums.*;
import com.dtech.claim.feign.MessageFeignClient;
import com.dtech.claim.util.MultipartFileUtil;
import com.dtech.claim.dto.ClaimRequestIdGen;
import com.dtech.claim.dto.PagingResult;
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
    private final InsuranceDetailsRepository insuranceDetailsRepository;

    @Autowired
    private final InsurancePolicyRepository insurancePolicyRepository;

    @Autowired
    private final InsurancePeriodRepository insurancePeriodRepository;

    @Autowired
    private final TreatmentRepository treatmentRepository;

    @Autowired
    private final InsuranceClaimsAccountBalanceRepository insuranceClaimsAccountBalanceRepository;

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
    private ModelMapper modelMapper;

    @Autowired
    private MessageFeignClient messageFeignClient;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> insuranceClaimRequest(ClaimRequestDTO claimRequestDTO, Locale locale) {
        try {
            log.info("Claim request processing started {}", claimRequestDTO);

            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(claimRequestDTO.getUsername().trim(), Status.ACTIVE).map((user) -> {

                ResponseEntity<ApiResponse<Object>> diagnosisValidationResult = validateDocumentCount(
                        claimRequestDTO.getDocuments(),
                        InsuranceClaimDocTypes.DIAGNOSIS_CARD.name(),
                        CommonParam.DIAGNOSIS_CARD_MAX_IMAGE.name(),
                        ResponseMessageUtil.INSURANCE_CLAIMS_DIAGNOSIS_MAX_IMAGE_INVALID,
                        ResponseMessageUtil.INSURANCE_CLAIMS_DIAGNOSIS_MIN_IMAGE_INVALID,
                        locale
                );
                if (diagnosisValidationResult != null) {
                    log.info("Invalid document count: {}", diagnosisValidationResult);
                    return diagnosisValidationResult;
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

                if (user.getApplicationOtpSession() != null) {
                    log.info("Otp request otp session  {} ", user.getApplicationOtpSession());

                    if (DateTimeUtil.getSeconds(user.getApplicationOtpSession().getCreatedDate(), 600).after(DateTimeUtil.getCurrentDateTime()) &&
                            user.getApplicationOtpSession().getOtp().equals(claimRequestDTO.getOtp()) && user.getApplicationOtpSession().isValidated()) {
                        log.info("Otp request valid {} ", user.getApplicationOtpSession());
                        updateApplicationUserOtpData(user, user.getApplicationOtpSession());

                        if (user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy() == null) {
                            log.info("User not eligible to claim request {}", claimRequestDTO.getUsername());
                            return ResponseEntity.ok().body(responseUtil.error(null, 1029, messageSource.getMessage(ResponseMessageUtil.USER_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale)));
                        }
                        return commonParameterRepository.findByCode(CommonParam.INSURANCE_CLAIM_REQUEST_PERIOD.name()).map((param) -> {
                                    log.info("get - date from claim request {}", param);
                                    Date minuesDate = DateTimeUtil.getMinuesDate(param.getValue()+1);
                                    if (claimRequestDTO.getToDate().before(minuesDate)) {
                                        log.info("older than claim request {}", claimRequestDTO.getUsername());
                                        return ResponseEntity.ok().body(responseUtil.error(null, 1037, messageSource.getMessage(ResponseMessageUtil.OLDER_DATE_CLAIM_REQUEST, null, locale)));
                                    }
                                    return insurancePolicyRepository.findByIdAndStatus(user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy().getId(), Status.ACTIVE).map((policy) -> {
                                        return insurancePeriodRepository.findByYearAndStatus(String.valueOf(LocalDate.now().getYear()), Status.ACTIVE).map((period) -> {
                                            return treatmentRepository.findByTreatmentCode(claimRequestDTO.getTreatment()).map((treatment) -> {
                                                return insuranceDetailsRepository.findByInsurancePolicyAndInsurancePeriodAndTreatmentAndStatus(policy, period, treatment, Status.ACTIVE).map((insuranceDetails) -> {

                                                    Optional<ClaimsDependents> claimsDependents = Optional.empty();

                                                    if (!claimRequestDTO.getIsEmployee()) {
                                                        log.info("Claim dependent found for request {}", true);
                                                        claimsDependents = claimDependentsRepository.
                                                                findByIdAndApplicationUserAndStatusAndEligibleFacilityIn(
                                                                        claimRequestDTO.getClaimsDependentId(),
                                                                        user,
                                                                        Workflow.ACTIVE, List.of(Facility.INSURANCE, Facility.BOTH));

                                                        if (claimsDependents.isEmpty()) {
                                                            log.info("Claim dependent not found or not eligible for insurance");
                                                            return ResponseEntity.ok().body(responseUtil.error(null, 1034, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_NOT_FOUND_OR_FACILITY_NOT_ELIGIBLE, null, locale)));
                                                        }
                                                    }

                                                    Optional<InsuranceClaimsAccountBalance> claimsAccountBalance = insuranceClaimsAccountBalanceRepository.findByEmployeeAndTreatmentAndInsurancePeriod(user, treatment, period);

                                                    //check available fund
                                                    String message = checkFundLimits(insuranceDetails, claimRequestDTO, claimsAccountBalance.orElse(null));

                                                    if (message != null && !message.isEmpty()) {
                                                        log.info("validation filed {} ", message);
                                                        return ResponseEntity.ok().body(responseUtil.error(null, 1035, message));
                                                    }
                                                    String claimRequestId = saveClaimRequest(claimRequestDTO, period, user, claimsDependents, treatment);
                                                    updateAccountBalance(claimsAccountBalance.orElse(null), claimRequestDTO, treatment, insuranceDetails, user, period);
                                                    notifyMessage(user.getPrimaryMobile(),claimRequestId);
                                                    return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.INSURANCE_CLAIM_REQUEST_SUBMIT_SUCCESS, null, locale)));

                                                }).orElseGet(() -> {
                                                    log.info("User insurance policy period treatment not found");
                                                    return ResponseEntity.ok().body(responseUtil.error(null, 1033, messageSource.getMessage(ResponseMessageUtil.POLICY_TREATMENT_PERIOD_NOT_FOUND_OR_INACTIVE, null, locale)));
                                                });
                                            }).orElseGet(() -> {
                                                log.info("User insurance treatment not found {} ", DateTimeUtil.getCurrentDateTime());
                                                return ResponseEntity.ok().body(responseUtil.error(null, 1032, messageSource.getMessage(ResponseMessageUtil.TREATMENT_NOT_FOUND, null, locale)));
                                            });
                                        }).orElseGet(() -> {
                                            log.info("User insurance period not found {} ", DateTimeUtil.getCurrentDateTime());
                                            return ResponseEntity.ok().body(responseUtil.error(null, 1031, messageSource.getMessage(ResponseMessageUtil.INSURANCE_PERIOD_NOT_FOUND, null, locale)));
                                        });
                                    }).orElseGet(() -> {
                                        log.info("User insurance policy not found {} ", user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy().getId());
                                        return ResponseEntity.ok().body(responseUtil.error(null, 1030, messageSource.getMessage(ResponseMessageUtil.INSURANCE_POLICY_NOT_FOUND, null, locale)));
                                    });
                                })
                                .orElseGet(() -> {
                                    log.info("User common param claim request {}", claimRequestDTO.getUsername());
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1036, messageSource.getMessage(ResponseMessageUtil.COMMON_PARAM_NOT_FOUND, null, locale)));

                                });
                    }

                    log.info("Otp request validation fail otp or invalid session {}", user.getApplicationOtpSession());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1016, messageSource.getMessage(ResponseMessageUtil.OTP_INVALID_OR_SESSION_TIME_OUT, null, locale)));
                }
                log.info("Otp request otp session not found {} ", claimRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1015, messageSource.getMessage(ResponseMessageUtil.OTP_SESSION_NOT_FOUND, null, locale)));

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
            messageRequestDTO.setType(NotificationsType.INSURANCE_CLAIM.name());
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
                InsurancePeriod period = insurancePeriodRepository.findByYearAndStatus(String.valueOf(LocalDate.now().getYear()), Status.ACTIVE).orElse(null);
                List<Treatment> treatmentList = treatmentRepository.findAllByStatus(Status.ACTIVE);
                log.info("Insurance claim reference data get dependence {} ", treatmentList);
                List<SimpleBaseDTO> claimsDependents = claimDependentsRepository.
                        findByApplicationUserAndStatusAndEligibleFacilityIn(user, Workflow.ACTIVE, List.of(Facility.INSURANCE, Facility.BOTH))
                        .stream().map(dep -> new SimpleBaseDTO(String.valueOf(dep.getId()), dep.getFirstName() + " " + dep.getLastName())).toList();
                log.info("Call minus insurance claim date");
                Date minuesDate = DateTimeUtil.getMinuesDate(Objects.requireNonNull(commonParameterRepository.findByCode(CommonParam.INSURANCE_CLAIM_REQUEST_PERIOD.name()).orElse(null)).getValue());
                int diagnosis = Objects.requireNonNull(commonParameterRepository.findByCode(CommonParam.DIAGNOSIS_CARD_MAX_IMAGE.name()).orElse(null)).getValue();
                int treatment = Objects.requireNonNull(commonParameterRepository.findByCode(CommonParam.TREATMENT_BILL_MAX_IMAGE.name()).orElse(null)).getValue();
                log.info("Get minus insurance claim date {} ", minuesDate);
                Map<String, Object> splashData = new HashMap<>();
                List<AvailableInsuranceLimitDTO> list = null;
                List<SimpleBaseDTO> userWiseTreatment = new ArrayList<>();
                if (period != null) {
                    list = treatmentList.stream().map(tre -> {
                        InsuranceClaimsAccountBalance insuranceClaimsAccountBalance = insuranceClaimsAccountBalanceRepository
                                .findByEmployeeAndTreatmentAndInsurancePeriod(user, tre, period)
                                .orElse(null);
                        log.info("Insurance claim reference data acc balance + treatment {} {} ", insuranceClaimsAccountBalance, tre);

                        log.info("Insurance claim reference data acc balance is null");
                        InsuranceDetails insuranceDetails = insuranceDetailsRepository.
                                findByInsurancePolicyAndInsurancePeriodAndTreatmentAndStatus(user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy(), period, tre, Status.ACTIVE).orElse(null);
                        log.info("Insurance claim reference data balance is {} ", insuranceDetails);

                        if (insuranceDetails != null && insuranceDetails.getTreatment() != null) {
                            userWiseTreatment.add(new SimpleBaseDTO(insuranceDetails.getTreatment()
                                    .getTreatmentCode(), insuranceDetails.getTreatment()
                                    .getTreatmentDescription()));
                        }

                        AvailableInsuranceLimitDTO availableInsuranceLimitDTO = AvailableInsuranceLimitDTO.builder()
                                .treatment(tre.getTreatmentCode())
                                .availableLimit(insuranceClaimsAccountBalance == null ? Objects.nonNull(insuranceDetails) ? insuranceDetails.getClaimLimit() : BigDecimal.valueOf(0.00) : insuranceClaimsAccountBalance.getAvailableBalance())
                                .fundLimit(Objects.nonNull(insuranceDetails) ? insuranceDetails.getClaimLimit() : BigDecimal.valueOf(0.00))
                                .build();
                        log.info("AvailableInsuranceLimitDTO create success {}", availableInsuranceLimitDTO);
                        return availableInsuranceLimitDTO;

                    }).toList();
                }
                log.info("Claims data success {}", list);
                splashData.put("insuranceClaimsDependents", claimsDependents);
                splashData.put("treatment", userWiseTreatment);
                splashData.put("insuranceClaimsFundLimits", list);
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

    @Transactional
    protected void updateApplicationUserOtpData(ApplicationUser applicationUser, ApplicationOtpSession applicationOtpSession) {
        log.info("Update otp validation request otp records");
        applicationUser.setOtpAttemptCount(0);
        applicationOtpSession.setValidated(true);
        applicationUserRepository.saveAndFlush(applicationUser);
        applicationOtpSessionRepository.saveAndFlush(applicationOtpSession);
    }

    @Transactional
    protected void updateAccountBalance(InsuranceClaimsAccountBalance insuranceClaimsAccountBalance, ClaimRequestDTO claimRequestDTO, Treatment treatment, InsuranceDetails insuranceDetails, ApplicationUser applicationUser, InsurancePeriod insurancePeriod) {
        try {
            log.info("Claim request account balance update started {}", claimRequestDTO);

            if (insuranceClaimsAccountBalance == null) {
                log.info("Claim request account balance is null");
                insuranceClaimsAccountBalance = new InsuranceClaimsAccountBalance();
                insuranceClaimsAccountBalance.setUtilizeAmount(claimRequestDTO.getRequestAmount());
                insuranceClaimsAccountBalance.setAvailableBalance(insuranceDetails.getClaimLimit().subtract(claimRequestDTO.getRequestAmount()));
                insuranceClaimsAccountBalance.setEmployee(applicationUser);
                insuranceClaimsAccountBalance.setTreatment(treatment);
                insuranceClaimsAccountBalance.setInsurancePeriod(insurancePeriod);

            } else {
                log.info("claim request account balance not null {}", claimRequestDTO);
                insuranceClaimsAccountBalance.setUtilizeAmount(insuranceClaimsAccountBalance.getUtilizeAmount().add(claimRequestDTO.getRequestAmount()));
                insuranceClaimsAccountBalance.setAvailableBalance(insuranceClaimsAccountBalance.getAvailableBalance().subtract(claimRequestDTO.getRequestAmount()));
            }
            insuranceClaimsAccountBalanceRepository.saveAndFlush(insuranceClaimsAccountBalance);
            log.info("claim request account balance update finished");
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected InsuranceClaimsDetails saveClaimRequestDetails(ClaimRequestDTO claimRequestDTO, Treatment treatment) {
        try {
            log.info("Claim request details save started {}", claimRequestDTO);
            InsuranceClaimsDetails insuranceClaimsDetails = new InsuranceClaimsDetails();
            insuranceClaimsDetails.setTreatment(treatment);
            insuranceClaimsDetails.setFromTreatmentDate(claimRequestDTO.getFromDate());
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
    protected String saveClaimRequest(ClaimRequestDTO claimRequestDTO, InsurancePeriod insurancePeriod, ApplicationUser applicationUser,
                                    Optional<ClaimsDependents> claimsDependents, Treatment treatment) {
        try {
            log.info("Claim request save started {}", claimRequestDTO);

            InsuranceClaimsDetails insuranceClaimsDetails = saveClaimRequestDetails(claimRequestDTO, treatment);

            ClaimRequestIdGen claimRequestIdGen = ClaimRequestIdGen.builder().year(String.valueOf(insurancePeriod.getYear())).company(applicationUser.getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode()).staffCategory(applicationUser.getUserPersonalDetails().getUserCompanyDetails().getStaffTypes().getCode()).build();
            RequestIdGenUtil requestIdGenUtil = new RequestIdGenUtil();
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
            insuranceClaimsRequestRepository.saveAndFlush(insuranceClaimsRequest);
            log.info("Complete save claim request id {}", claimRequestId);
            return claimRequestId;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected String checkFundLimits(InsuranceDetails insuranceDetails, ClaimRequestDTO claimRequestDTO, InsuranceClaimsAccountBalance insuranceClaimsAccountBalance) {
        try {
            log.info("Check global credit limit");
            String message = "";
            if (insuranceClaimsAccountBalance != null) {
                message = checkClaimLimitExceeded(insuranceClaimsAccountBalance.getAvailableBalance(), claimRequestDTO.getRequestAmount(), "Claim limit exceeded", "val.request.account.balance.insufficient");
                if (message != null) {
                    return message;
                }
            } else {
                message = checkClaimLimitExceeded(insuranceDetails.getClaimLimit(), claimRequestDTO.getRequestAmount(), "Claim global limit exceeded", "val.request.credit.limit.exceed.to.global.limit");
                if (message != null) {
                    return message;
                }
            }
            return message;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private String checkClaimLimitExceeded(BigDecimal claimLimit, BigDecimal requestAmount, String logMessage, String message) {
        if (claimLimit.compareTo(requestAmount) < 0) {
            log.info(logMessage, claimLimit, requestAmount);
            return messageSource.getMessage(message, null, null);
        }
        return null;
    }
}
