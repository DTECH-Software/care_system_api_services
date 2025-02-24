/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 2:07 PM
 * <p>
 */

package com.dtech.auth.service.impl;

import com.dtech.auth.dto.request.ChannelRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.dto.response.ApplicationUserDetailsResponseDTO;
import com.dtech.auth.dto.response.UserPersonalDetailsResponseDTO;
import com.dtech.auth.enums.Status;
import com.dtech.auth.repository.ApplicationUserRepository;
import com.dtech.auth.service.ProfileService;
import com.dtech.auth.util.DateTimeUtil;
import com.dtech.auth.util.ResponseMessageUtil;
import com.dtech.auth.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> profile(ChannelRequestDTO channelRequestDTO, Locale locale) {

        try {
            log.info("User profile request {} ", channelRequestDTO);
            return applicationUserRepository.
                    findByUsernameAndUserPersonalDetails_UserStatus(channelRequestDTO.getUsername().trim(),
                            Status.ACTIVE).map((ap) -> {
                                log.info("User profile request user found{} ", ap);
                        ApplicationUserDetailsResponseDTO applicationUserDetailsResponseDTO = modelMapper.map(ap,ApplicationUserDetailsResponseDTO.class);
                        getAge(applicationUserDetailsResponseDTO.getUserPersonalDetails());
                        log.info("User profile request success{} ", applicationUserDetailsResponseDTO);
                        return ResponseEntity.ok().body(responseUtil.success((Object) applicationUserDetailsResponseDTO, messageSource.getMessage(ResponseMessageUtil.APPLICATION_PROFILE_SPLASH_SUCCESS, null, locale)));
                    }).orElseGet(() -> {
                        log.info("User profile request user not found {} ", channelRequestDTO);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
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
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
