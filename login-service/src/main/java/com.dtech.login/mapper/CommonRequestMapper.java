/**
 * User: Himal_J
 * Date: 2/4/2025
 * Time: 4:28 PM
 * <p>
 */

package com.dtech.login.mapper;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CommonRequestMapper {

    private static final Gson gson = new Gson();

    public static <T> T mapCommonRequest(T inputObject,Class<T> tClass) {
        try {
            log.info("calling mapCommonRequest {}. mapper class {}", inputObject,tClass);
            return gson.fromJson(gson.toJson(inputObject), tClass);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}
