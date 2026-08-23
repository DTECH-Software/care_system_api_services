/**
 * User: Himal_J
 * Date: 2/4/2025
 * Time: 4:43 PM
 * <p>
 */

package com.dtech.login.util;

import com.dtech.login.dto.response.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Log4j2
public class ExtractApiResponseUtil {
    public static Object extractApiResponse(ResponseEntity<ApiResponse<Object>> responseEntity) {
        try {
            log.info("call api response {}", responseEntity.getStatusCode());
            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                log.info("call api response inside body {}", responseEntity.getBody());
                ApiResponse<Object> apiResponseBody = responseEntity.getBody();
                if (apiResponseBody.getData() != null) {
                    log.info("call api response inside api response body data {}" , apiResponseBody.getData());
                    return apiResponseBody.getData();
                }
            }
            log.info("call api response without body response {}", responseEntity.getBody());
            return null;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
