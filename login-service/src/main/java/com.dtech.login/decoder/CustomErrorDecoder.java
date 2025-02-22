package com.dtech.login.decoder;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;

public class CustomErrorDecoder implements ErrorDecoder {
    
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == HttpStatus.BAD_REQUEST.value()) {
            return new Exception("Validation error occurred: " + response.reason());
        }
        
        return new Default().decode(methodKey, response);
    }

}