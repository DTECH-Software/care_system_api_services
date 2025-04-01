/**
 * User: Himal_J
 * Date: 3/16/2025
 * Time: 10:18 AM
 * <p>
 */

package com.dtech.claim.service.impl;

import com.dtech.claim.dto.ClaimRequestIdGen;
import com.dtech.claim.dto.DeathLimitDTO;
import com.dtech.claim.dto.PagingResult;
import com.dtech.claim.dto.SimpleBaseDTO;
import com.dtech.claim.dto.request.*;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.dto.response.DeathClaimRequestResponseDTO;
import com.dtech.claim.dto.response.InsuranceClaimRequestResponseDTO;
import com.dtech.claim.dto.search.ClaimHistory;
import com.dtech.claim.enums.*;
import com.dtech.claim.enums.DeathBeneficiary;
import com.dtech.claim.feign.DocumentFeignClient;
import com.dtech.claim.feign.MessageFeignClient;
import com.dtech.claim.mapper.EntityToDtoMapper;
import com.dtech.claim.model.*;
import com.dtech.claim.repository.*;
import com.dtech.claim.service.DeathClaimRequestService;
import com.dtech.claim.specifications.DeathClaimHistorySpecification;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@Service
@Log4j2
@RequiredArgsConstructor
public class DeathClaimRequestServiceImpl implements DeathClaimRequestService {

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final CommonParameterRepository commonParameterRepository;

    @Autowired
    private final DeathBeneficiaryRepository deathBeneficiaryRepository;

    @Autowired
    private final ClaimDependentsRepository claimDependentsRepository;

    @Autowired
    private DeathClaimRequestRepository deathClaimRequestRepository;

    @Autowired
    private final DocumentFeignClient documentFeignClient;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private final EntityManager entityManager;

    @Autowired
    private MessageFeignClient messageFeignClient;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> deathClaimReferenceData(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Death claim reference data {} ", channelRequestDTO);
            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(channelRequestDTO.getUsername().trim(), Status.ACTIVE).map((user) -> {
                Map<String, Object> splashData = new HashMap<>();

                List<ClaimsDependents> claimsDependents = claimDependentsRepository.
                        findByApplicationUserAndStatusAndEligibleFacilityIn(user, Workflow.ACTIVE, List.of(Facility.DEATH, Facility.BOTH))
                        .stream().filter(dep -> {
                            boolean exists = deathClaimRequestRepository.existsByClaimsDependentsAndEmployeeAndRequestStatusIn(dep, user, List.of(Workflow.APPROVED, Workflow.UNDER_REVIEW));
                            return !exists;
                        }).toList();

                CommonParameter deathAge = commonParameterRepository.findByCode(CommonParam.DEATH_AGE.name()).orElse(null);

                ArrayList<DeathLimitDTO> deathLimitDTOS = new ArrayList<>();
                ArrayList<SimpleBaseDTO> claimDependent = new ArrayList<>();

                claimsDependents.forEach((dep) -> {
                    claimDependent.add(new SimpleBaseDTO(String.valueOf(dep.getId()), dep.getFirstName() + " " + dep.getLastName()));

                    if (dep.getRelationCategory().equals(RelationCategory.CHILD) ||
                            dep.getRelationCategory().equals(RelationCategory.SISTER) ||
                            dep.getRelationCategory().equals(RelationCategory.BROTHER)) {
                        log.info("Relation claim reference data {} ", dep.getRelationCategory());
                        int age = DateTimeUtil.getAge(String.valueOf(dep.getDob()));
                        Range range = Range.LOWER;

                        if (age > (deathAge != null ? deathAge.getValue() : 1)) {
                            log.info("Upper range claim reference data {} ", age);
                            range = Range.UPPER;
                        }
                        com.dtech.claim.model.DeathBeneficiary deathBeneficiary = deathBeneficiaryRepository.
                                findByCodeAndRangeAndStatus(DeathBeneficiary.valueOf(dep.getRelationCategory().name()),
                                        range, Status.ACTIVE).orElse(null);

                        deathLimitDTOS.add(DeathLimitDTO.builder()
                                .dependentId(String.valueOf(dep.getId()))
                                .deathLimit(deathBeneficiary != null ? deathBeneficiary.getClaimLimit() : null)
                                .ageRange(deathBeneficiary != null ? deathBeneficiary.getRange().name() : null)
                                .build());

                    } else {

                        com.dtech.claim.model.DeathBeneficiary deathBeneficiary = deathBeneficiaryRepository.
                                findByCodeAndStatus(DeathBeneficiary.valueOf(dep.getRelationCategory().name()), Status.ACTIVE).orElse(null);

                        deathLimitDTOS.add(DeathLimitDTO.builder()
                                .dependentId(String.valueOf(dep.getId()))
                                .deathLimit(deathBeneficiary != null ? deathBeneficiary.getClaimLimit() : null)
                                .ageRange(deathBeneficiary != null ? deathBeneficiary.getRange() != null ? deathBeneficiary.getRange().name() : null : null)
                                .build());
                    }
                });
                log.info("Call minus insurance claim date");
                Date minuesDate = DateTimeUtil.getDaysDifference(Objects.requireNonNull(commonParameterRepository.findByCode(CommonParam.DEATH_CLAIM_REQUEST_PERIOD.name()).orElse(null)).getValue());
                splashData.put("insuranceClaimsDependents", claimDependent);
                splashData.put("deathClaimsFundLimits", deathLimitDTOS);
                splashData.put("deathMinPastDate", minuesDate);
                return ResponseEntity.ok().body(responseUtil.success((Object) splashData, messageSource.getMessage(ResponseMessageUtil.DEATH_CLAIMS_REFERENCE_DETAILS_SUCCESS, null, locale)));

            }).orElseGet(() -> {
                log.info("User death claim request user not found {} ", channelRequestDTO);
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> deathClaimRequest(DeathClaimRequestDTO deathClaimRequestDTO, Locale locale) {
        try {
            log.info("Death Claim Request: " + deathClaimRequestDTO);
            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(deathClaimRequestDTO.getUsername().trim(), Status.ACTIVE)
                    .map((user) -> {

                        ResponseEntity<ApiResponse<Object>> deathCertification = validateDocumentCount(
                                deathClaimRequestDTO.getDocuments(),
                                DeathClaimDocTypes.DEATH_CERTIFICATE.name(),
                                CommonParam.DEATH_CERTIFICATE_MAX_IMAGE.name(),
                                ResponseMessageUtil.DEATH_CLAIMS_DEATH_MAX_IMAGE_INVALID,
                                ResponseMessageUtil.DEATH_CLAIMS_DEATH_MIN_IMAGE_INVALID,
                                locale
                        );

                        if (deathCertification != null) {
                            log.info("Invalid document count: {}", deathCertification);
                            return deathCertification;
                        }

                        if (user.getApplicationOtpSession() != null) {
                            if (DateTimeUtil.getSeconds(user.getApplicationOtpSession().getCreatedDate(), 600).after(DateTimeUtil.getCurrentDateTime()) &&
                                    user.getApplicationOtpSession().getOtp().equals(deathClaimRequestDTO.getOtp()) && user.getApplicationOtpSession().isValidated()) {

                                log.info("Otp request valid {} ", user.getApplicationOtpSession());
                                return commonParameterRepository.findByCode(CommonParam.DEATH_CLAIM_REQUEST_PERIOD.name())
                                        .map((param) -> {
                                            log.info("get - date from death claim request {}", param);
                                            Date minuesDate = DateTimeUtil.getMinuesDate(param.getValue() + 1);
                                            if (deathClaimRequestDTO.getDeathDate().before(minuesDate)) {
                                                log.info("older than claim request {}", deathClaimRequestDTO.getUsername());
                                                return ResponseEntity.ok().body(responseUtil.error(null, 1037, messageSource.getMessage(ResponseMessageUtil.OLDER_DATE_DEATH_CLAIM_REQUEST, null, locale)));
                                            }

                                            Optional<ClaimsDependents> claimsDependents = claimDependentsRepository
                                                    .findByIdAndApplicationUserAndStatusAndEligibleFacilityIn(
                                                            deathClaimRequestDTO.getClaimsDependentId(),
                                                            user,
                                                            Workflow.ACTIVE, List.of(Facility.DEATH, Facility.BOTH));

                                            if (claimsDependents.isEmpty()) {
                                                log.info("Claim dependent not found or not eligible for death");
                                                return ResponseEntity.ok().body(responseUtil.error(null, 1034, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_NOT_FOUND_OR_FACILITY_NOT_ELIGIBLE, null, locale)));
                                            }

                                            AtomicReference<Range> range = new AtomicReference<>();
                                            if (claimsDependents.get().getRelationCategory().equals(RelationCategory.CHILD) ||
                                                    claimsDependents.get().getRelationCategory().equals(RelationCategory.BROTHER) ||
                                                    claimsDependents.get().getRelationCategory().equals(RelationCategory.SISTER)) {

                                                Date dob = claimsDependents.get().getDob();
                                                log.info("get - date from death claim request {}", dob);
                                                int age = DateTimeUtil.getAge(String.valueOf(dob));

                                                commonParameterRepository.findByCode(CommonParam.DEATH_AGE.name())
                                                        .map((dAge) -> {

                                                            if (age <= dAge.getValue()) {
                                                                log.info("Age less than death claim request {} {}", age, dAge.getValue());
                                                                range.set(Range.LOWER);
                                                            } else {
                                                                log.info("Age greater than death claim request {} {}", age, dAge.getValue());
                                                                range.set(Range.UPPER);
                                                            }
                                                            return null;
                                                        })
                                                        .orElseGet(() -> {
                                                            log.info("User common param death claim age request {}", deathClaimRequestDTO.getUsername());
                                                            return ResponseEntity.ok().body(responseUtil.error(null, 1036, messageSource.getMessage(ResponseMessageUtil.COMMON_PARAM_NOT_FOUND, null, locale)));
                                                        });
                                            }

                                            return deathBeneficiaryRepository.findByCodeAndRangeAndStatus(DeathBeneficiary.valueOf(claimsDependents.get().getRelationCategory().name()), range.get() == null ? null : range.get(), Status.ACTIVE)
                                                    .map((deathBeneficiary) -> {

                                                        return deathClaimRequestRepository.findByClaimsDependentsAndEmployeeAndRequestStatusIn(claimsDependents.get(), user, List.of(Workflow.APPROVED, Workflow.UNDER_REVIEW))
                                                                .map((claimRequest) -> {
                                                                    log.info("Death request already proceed {}", deathBeneficiary);
                                                                    return ResponseEntity.ok().body(responseUtil.error(null, 1046, messageSource.getMessage(ResponseMessageUtil.DEATH_CLAIM_ALREADY_PAID_OR_UNDER_REVIEW, null, locale)));
                                                                })
                                                                .orElseGet(() -> {
                                                                    log.info("Death claims not found - new request to proceed {}", deathBeneficiary);
                                                                    return commonParameterRepository.findByCode(CommonParam.DEATH_CLAIM_REQUEST_HALF_PAYMENT_PERIOD.name())
                                                                            .map((half) -> {
                                                                                Date halfDays = DateTimeUtil.getMinuesDate(half.getValue());
                                                                                PaymentType paymentType = PaymentType.FULL;
                                                                                BigDecimal amount = deathBeneficiary.getClaimLimit();
                                                                                if (deathClaimRequestDTO.getDeathDate().before(halfDays)) {
                                                                                    paymentType = PaymentType.HALF;
                                                                                    BigDecimal fiftyPercent = new BigDecimal(50).divide(new BigDecimal(100));
                                                                                    amount = deathBeneficiary.getClaimLimit().multiply(fiftyPercent);
                                                                                }

                                                                                List<Document> uploadSupportingDocument = deathClaimRequestDTO.getDocuments().stream().map(doc -> {
                                                                                    log.info("Upload supporting document from death request {}", deathClaimRequestDTO);
                                                                                    try {
                                                                                        return uploadImage(doc.getType(), doc.getFile(), doc.getFileType(), doc.getFileName());
                                                                                    } catch (IOException e) {
                                                                                        log.error(e);
                                                                                        throw new RuntimeException(e);
                                                                                    }
                                                                                }).collect(Collectors.toList());

                                                                                String claimRequestId = saveDeathClaimRequest(deathClaimRequestDTO, paymentType, amount, claimsDependents.get(), user, deathBeneficiary, uploadSupportingDocument);
                                                                                notifyMessage(user.getPrimaryMobile(), claimRequestId);
                                                                                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.DEATH_CLAIM_REQUEST_SUBMIT_SUCCESS, null, locale)));
                                                                            })
                                                                            .orElseGet(() -> {
                                                                                log.info("User common param death claim half period request {}", deathClaimRequestDTO.getUsername());
                                                                                return ResponseEntity.ok().body(responseUtil.error(null, 1036, messageSource.getMessage(ResponseMessageUtil.COMMON_PARAM_NOT_FOUND, null, locale)));
                                                                            });
                                                                });
                                                    }).orElseGet(() -> {
                                                        log.info("Death beneficiary not found or not eligible for death {} ", claimsDependents.get());
                                                        return ResponseEntity.ok().body(responseUtil.error(null, 1045, messageSource.getMessage(ResponseMessageUtil.BENEFICIARY_NOT_FOUND_OR_FACILITY_NOT_ELIGIBLE, null, locale)));
                                                    });

                                        }).orElseGet(() -> {
                                            log.info("User common param death claim request {}", deathClaimRequestDTO.getUsername());
                                            return ResponseEntity.ok().body(responseUtil.error(null, 1036, messageSource.getMessage(ResponseMessageUtil.COMMON_PARAM_NOT_FOUND, null, locale)));
                                        });

                            }
                            log.info("Otp request validation fail otp or invalid session {}", user.getApplicationOtpSession());
                            return ResponseEntity.ok().body(responseUtil.error(null, 1016, messageSource.getMessage(ResponseMessageUtil.OTP_INVALID_OR_SESSION_TIME_OUT, null, locale)));

                        }
                        log.info("Otp request otp session not found {} ", deathClaimRequestDTO.getUsername());
                        return ResponseEntity.ok().body(responseUtil.error(null, 1015, messageSource.getMessage(ResponseMessageUtil.OTP_SESSION_NOT_FOUND, null, locale)));

                    }).orElseGet(() -> {
                        log.info("User claim request user not found {} ", deathClaimRequestDTO);
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
            log.info("Death request notify email");
            MessageRequestDTO messageRequestDTO = new MessageRequestDTO();
            messageRequestDTO.setValue(requestId);
            messageRequestDTO.setMobileNo(mobile);
            messageRequestDTO.setType(MessageType.DEATH_CLAIM.name());
            log.info("Before message request mapper {} ", messageRequestDTO);
            log.info("Before calling message service {}", messageFeignClient);
            messageFeignClient.sendMessage(messageRequestDTO);
        } catch (RuntimeException e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> deathClaimHistoryList(PaginationRequest<ClaimHistory> paginationRequest, Locale locale) {
        try {
            log.info("Death claim history filter list {}", paginationRequest);

            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(paginationRequest.getUsername().trim(), Status.ACTIVE).map((user) -> {

                Pageable pageable = PaginationUtil.getPageable(paginationRequest);

                Page<DeathClaimRequest> claimsRequests = Objects.nonNull(paginationRequest.getSearch()) ?
                        deathClaimRequestRepository.findAll(DeathClaimHistorySpecification.getSpecification(paginationRequest.getSearch(), user.getId()), pageable) :
                        deathClaimRequestRepository.findAll(DeathClaimHistorySpecification.getSpecification(user.getId()), pageable);
                log.info("Filter records {}", claimsRequests);
                long totalElements = Objects.nonNull(paginationRequest.getSearch()) ?
                        deathClaimRequestRepository.count(DeathClaimHistorySpecification.getSpecification(paginationRequest.getSearch(), user.getId())) :
                        deathClaimRequestRepository.count(DeathClaimHistorySpecification.getSpecification(user.getId()));
                log.info("Total elements count records death{}", totalElements);
                log.info("Filter list data fetching death success");
                List<DeathClaimRequestResponseDTO> collectList = claimsRequests.stream()
                        .map(EntityToDtoMapper::mapDeathClaimHistoryDetails).toList();
                log.info("Filter list death {} success", collectList);
                return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<DeathClaimRequestResponseDTO>(collectList, collectList.size(), totalElements),
                        messageSource.getMessage(ResponseMessageUtil.DEATH_CLAIM_REQUEST_HISTORY_FILTER_LIST_SUCCESS,
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

    @Transactional
    protected String saveDeathClaimRequest(DeathClaimRequestDTO deathClaimRequestDTO,
                                           PaymentType paymentType, BigDecimal amount,
                                           ClaimsDependents claimsDependents,
                                           ApplicationUser applicationUser,
                                           com.dtech.claim.model.DeathBeneficiary deathBeneficiary, List<Document> uploadSupportingDocument) {
        try {
            log.info("Save death claim request death{}", deathClaimRequestDTO);
            ClaimRequestIdGen claimRequestIdGen = ClaimRequestIdGen.builder()
                    .year(String.valueOf(LocalDate.now().getYear()))
                    .company(applicationUser.getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode())
                    .staffCategory(applicationUser.getUserPersonalDetails().getUserCompanyDetails().getStaffTypes().getCode())
                    .build();
            RequestIdGenUtil requestIdGenUtil = new RequestIdGenUtil(false);
            log.info("Generate request id death {}", claimRequestIdGen);
            String claimRequestId = (String) requestIdGenUtil.generate(entityManager.unwrap(SharedSessionContractImplementor.class), claimRequestIdGen);
            log.info("after generate request id death {}", claimRequestId);
            DeathClaimRequest deathClaimRequest = new DeathClaimRequest();
            deathClaimRequest.setDeathDate(deathClaimRequestDTO.getDeathDate());
            deathClaimRequest.setRequestStatus(Workflow.UNDER_REVIEW);
            deathClaimRequest.setRemark(deathClaimRequestDTO.getRemark());
            deathClaimRequest.setPaymentType(paymentType);
            deathClaimRequest.setUtilizeAmount(amount);
            deathClaimRequest.setClaimsDependents(claimsDependents);
            deathClaimRequest.setEmployee(applicationUser);
            deathClaimRequest.setDeathBeneficiary(deathBeneficiary);
            deathClaimRequest.setDocuments(uploadSupportingDocument);
            log.info("Save death claim request {}", deathClaimRequestDTO);
            deathClaimRequestRepository.saveAndFlush(deathClaimRequest);
            return claimRequestId;
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


}



