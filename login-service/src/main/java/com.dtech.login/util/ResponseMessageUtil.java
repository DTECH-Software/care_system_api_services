/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 10:26 AM
 * <p>
 */

package com.dtech.login.util;

public class ResponseMessageUtil {
    /*Login*/
    public final static String USERNAME_PASSWORD_INVALID = "val.username.password.invalid";
    public final static String AUTHENTICATION_SUCCESS = "val.authentication.success";
    public final static String PASSWORD_EXPIRED_AT_LOGIN_TIME = "val.password.expired.at.login.time";
    public final static String PASSWORD_ATTEMPT_EXCEED = "val.password.attempt.exceed";

    /*Password reset*/
    public final static String PASSWORD_RESET_SUCCESS = "val.password.reset.success";
    public final static String APPLICATION_USER_NOT_FOUND = "val.application.user.not.found";
    public final static String APPLICATION_USER_PASSWORD_POLICY_NOT_FOUND = "val.application.user.password.policy.not.found";
    public final static String APPLICATION_USER_OTP_SESSION_NOT_FOUND = "val.application.user.otp.session.not.found";
    public final static String APPLICATION_USER_OTP_REQUEST_TRY_TO_AFTER_60S = "val.application.user.otp.session.60s";
    public final static String APPLICATION_USER_OTP_SEND_SUCCESS = "val.application.user.otp.send.success";
    public final static String APPLICATION_USER_OTP_EXCEED = "val.application.user.otp.exceed";
    public final static String OTP_SESSION_NOT_FOUND = "val.application.otp.not.found";
    public final static String OTP_INVALID_OR_SESSION_TIME_OUT = "val.application.otp.invalid.or.session.timeout";
    public final static String OTP_VALIDATION_SUCCESS = "val.application.otp.validation.success";
}
