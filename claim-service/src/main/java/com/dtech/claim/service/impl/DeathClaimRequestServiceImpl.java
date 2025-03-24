/**
 * User: Himal_J
 * Date: 3/16/2025
 * Time: 10:18 AM
 * <p>
 */

package com.dtech.claim.service.impl;

import com.dtech.claim.dto.request.DeathClaimRequestDTO;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.enums.CommonParam;
import com.dtech.claim.enums.DeathBeneficiary;
import com.dtech.claim.enums.Status;
import com.dtech.claim.repository.ApplicationUserRepository;
import com.dtech.claim.repository.CommonParameterRepository;
import com.dtech.claim.repository.DeathBeneficiaryRepository;
import com.dtech.claim.service.DeathClaimRequestService;
import com.dtech.claim.util.DateTimeUtil;
import com.dtech.claim.util.ResponseMessageUtil;
import com.dtech.claim.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Locale;

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

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> deathClaimRequest(DeathClaimRequestDTO deathClaimRequestDTO, Locale locale) {
        try {
            log.info("Death Claim Request: " + deathClaimRequestDTO);
            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(deathClaimRequestDTO.getUsername().trim(), Status.ACTIVE).map((user) -> {
                if (user.getApplicationOtpSession() != null) {
                    if (DateTimeUtil.getSeconds(user.getApplicationOtpSession().getCreatedDate(), 60).after(DateTimeUtil.getCurrentDateTime()) &&
                            user.getApplicationOtpSession().getOtp().equals(deathClaimRequestDTO.getOtp()) && !user.getApplicationOtpSession().isValidated()) {

                        log.info("Otp request valid {} ", user.getApplicationOtpSession());
                        return commonParameterRepository.findByCode(CommonParam.DEATH_CLAIM_REQUEST_PERIOD.name()).map((param) -> {
                            log.info("get - date from death claim request {}", param);
                            Date minuesDate = DateTimeUtil.getMinuesDate(param.getValue());
                            if (deathClaimRequestDTO.getDeathDate().before(minuesDate)) {
                                log.info("older than claim request {}", deathClaimRequestDTO.getUsername());
                                return ResponseEntity.ok().body(responseUtil.error(null, 1037, messageSource.getMessage(ResponseMessageUtil.OLDER_DATE_CLAIM_REQUEST, null, locale)));
                            }



                            return null;
//                            return deathBeneficiaryRepository.findByCode(DeathBeneficiary.)
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
}
