/**
 * User: Himal_J
 * Date: 2/18/2025
 * Time: 9:18 AM
 * <p>
 */

package com.dtech.auth.service.impl;


import com.dtech.auth.dto.request.SignupRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.service.SignupService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Log4j2
public class SignupServiceImpl implements SignupService {

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> signUp(SignupRequestDTO signupRequestDTO, Locale locale) {

        try{

        }catch (Exception e){
            log.error(e);
            throw e;
        }

    }
}
