/**
 * User: Himal_J
 * Date: 2/18/2025
 * Time: 9:18 AM
 * <p>
 */

package com.dtech.auth.service.impl;


import com.dtech.auth.dto.request.SignupInquiryDTO;
import com.dtech.auth.dto.request.SignupRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.dto.response.UserPersonalDetailsResponseDTO;
import com.dtech.auth.repository.ApplicationUserRepository;
import com.dtech.auth.repository.UserPersonalDetailsRepository;
import com.dtech.auth.service.SignupService;
import com.dtech.auth.util.DateTimeUtil;
import com.dtech.auth.util.ResponseMessageUtil;
import com.dtech.auth.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Locale;

@Service
@Log4j2
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

    @Autowired
    private final UserPersonalDetailsRepository userPersonalDetailsRepository;

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Value("${client.mobile}")
    private String clientMobile;

    @Autowired
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> signupInquiry(SignupInquiryDTO signupInquiryDTO, Locale locale) {
        try {
            log.info("Processing SignupInquiry {}", signupInquiryDTO);

            return userPersonalDetailsRepository.findByEpfNoAndNicIgnoreCase(signupInquiryDTO.getEpfNo(), signupInquiryDTO.getNic())
                    .map(user -> {
                        return applicationUserRepository.findByUserPersonalDetails(user).map(applicationUser -> {
                            log.info("User already sign up {}", applicationUser);
                            return ResponseEntity.ok().body(responseUtil.error(null, 1018, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_ALREADY_SIGN_UP, null, locale)));
                        }).orElseGet(() -> {
                            log.info("Sign up inquiry start");
                            UserPersonalDetailsResponseDTO userPersonalDetailsResponseDTO = modelMapper.map(user,UserPersonalDetailsResponseDTO.class);
                            getAge(userPersonalDetailsResponseDTO);
                            log.info("Sign up inquiry end");
                            return ResponseEntity.ok().body(responseUtil.success(userPersonalDetailsResponseDTO, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_INQUIRY_SUCCESS, null, locale)));
                        });
                    })
                    .orElseGet(() -> {
                        log.info("Signup inquiry user not found {}", signupInquiryDTO);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1017, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND_ON_SYSTEM, new Object[]{clientMobile}, locale)));
                    });

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

        }catch (Exception e){
            log.error(e);
            throw e;
        }
    }

//    @Transactional(readOnly = true)
//    protected void setEmployeeAddressDetails(UserAddress userAddress){
//        try {
//            log.info("Processing setEmployeeAddressDetails {}", userAddress);
//
//        }catch (Exception e){
//            log.error(e);
//            throw e;
//        }
//    }

//    @Transactional(readOnly = true)
//    protected void setEmployeeCompanyDetails(UserCompanyDetails userCompanyDetails){
//        try {
//            log.info("Processing setEmployeeCompanyDetails {}", userCompanyDetails);
//
//        }catch (Exception e){
//            log.error(e);
//            throw e;
//        }
//    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> signUp(SignupRequestDTO signupRequestDTO, Locale locale) {

        try {
            return null;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }

    }
}
