/**
 * User: Himal_J
 * Date: 2/28/2025
 * Time: 4:12 PM
 * <p>
 */

package com.dtech.claim.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Value("${feign.api.key}")
    private String apiKey;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                template.header("X-API-KEY", apiKey);
            }
        };
    }

}
