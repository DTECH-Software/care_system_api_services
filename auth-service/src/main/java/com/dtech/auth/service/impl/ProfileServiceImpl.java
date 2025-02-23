/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 2:07 PM
 * <p>
 */

package com.dtech.auth.service.impl;

import com.dtech.auth.dto.request.SignupInquiryDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.service.ProfileService;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@Log4j2
public class ProfileServiceImpl implements ProfileService {
    @Override
    public ResponseEntity<ApiResponse<Object>> profile(SignupInquiryDTO signupInquiryDTO, Locale locale) {
        return null;
    }
}
