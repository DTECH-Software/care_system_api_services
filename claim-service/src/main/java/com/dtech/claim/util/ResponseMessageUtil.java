/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 10:26 AM
 * <p>
 */

package com.dtech.claim.util;

public class ResponseMessageUtil {
    /*Signup*/
    public final static String OTP_SESSION_NOT_FOUND = "val.application.otp.not.found";
    public final static String OTP_INVALID_OR_SESSION_TIME_OUT = "val.application.otp.invalid.or.session.timeout";
    public final static String OTP_VALIDATION_SUCCESS = "val.application.otp.validation.success";
    public final static String USERNAME_ALREADY_EXISTS = "val.application.username.already.exists";
    public final static String SIGNUP_PROCESS_SUCCESS= "val.application.signup.process.success";
    public final static String PASSWORD_POLICY_NOT_FOUND= "val.password.policy.notfound";
    public final static String USERNAME_POLICY_NOT_FOUND= "val.username.policy.notfound";
    public final static String PRIMARY_MOBILE_ALREADY_IN_USE= "val.primary.mobile.already.in.use";
    public final static String PRIMARY_EMAIL_ALREADY_IN_USE= "val.primary.email.already.in.use";

    /*profile*/
    public final static String APPLICATION_USER_NOT_FOUND = "val.application.user.not.found";
    public final static String APPLICATION_PROFILE_SUCCESS = "val.application.profile.success";
    public final static String CLAIM_DEPENDENT_MOTHER_FOUND = "val.claim.dependent.mother.found";
    public final static String CLAIM_DEPENDENT_FATHER_FOUND = "val.claim.dependent.father.found";
    public final static String CLAIM_DEPENDENT_ADDED_SUCCESS = "val.claim.dependent.added.success";
    public final static String CLAIM_DEPENDENT_WIFE_DOCUMENT_IS_EMPTY_OR_OUT_OF_RANGE = "val.claim.dependent.wife.document.is.empty.or.out.of.range";
    public final static String CLAIM_DEPENDENT_OTHER_RELATION_CATEGORY_DOCUMENT_IS_EMPTY_OR_OUT_OF_RANGE = "val.claim.dependent.other.relation.category.document.is.empty.or.out.of.range";
    public final static String CLAIM_DEPENDENT_LIST_VIEW_SUCCESS = "val.claim.dependent.list.view.success";
    public final static String PROFILE_IMAGE_UPDATE_SUCCESS = "val.profile.image.update.success";
    public final static String APPLICATION_USER_OTP_EXCEED = "val.application.user.otp.exceed";
    public final static String APPLICATION_USER_OTP_REQUEST_TRY_TO_AFTER_60S = "val.application.user.otp.session.60s";
    public final static String APPLICATION_USER_OTP_SESSION_NOT_FOUND = "val.application.user.otp.session.not.found";
    public final static String APPLICATION_USER_OTP_SEND_SUCCESS = "val.application.user.otp.send.success";
    public final static String APPLICATION_USER_DETAILS_NOT_CHANGE = "val.application.user.details.not.change";
    public final static String APPLICATION_USER_DETAILS_UPDATE_SUCCESS = "val.application.user.details.update.success";
    public final static String APPLICATION_USER_DETAILS_OTP_VERIFICATION_FAILED = "val.application.user.otp.verification.failed";

    /*Claim request*/
    public final static String CLAIM_DEPENDENT_NOT_FOUND = "val.claim.dependent.not.found";
    public final static String USER_NOT_ELIGIBLE_TO_CLAIM_REQUEST = "val.user.not.eligible.to.claim.request";
    public final static String DEPENDENT_NOT_ELIGIBLE_TO_CLAIM_REQUEST = "val.dependent.not.eligible.to.claim.request";
    public final static String INSURANCE_POLICY_NOT_FOUND = "val.insurance.policy.not.found";
    public final static String INSURANCE_PERIOD_NOT_FOUND = "val.insurance.period.not.found";
    public final static String TREATMENT_NOT_FOUND = "val.treatment.not.found";
    public final static String INSURANCE_CLAIM_DEFAULT_REQUEST_SUBMIT_SUCCESS = "val.insurance.claim.request.default.submit.success";
    public final static String INSURANCE_CLAIM_SPECIAL_SUBMIT_SUCCESS = "val.insurance.claim.request.special.submit.success";
    public final static String INSURANCE_CLAIM_REQUEST_VALIDATION_SUCCESS = "val.insurance.claim.request.validation.success";
    public final static String DEATH_CLAIM_REQUEST_SUBMIT_SUCCESS = "val.death.claim.request.submit.success";
    public final static String DEATH_CLAIM_REQUEST_VALIDATION_SUCCESS = "val.death.claim.request.validation.success";
    public final static String POLICY_TREATMENT_PERIOD_NOT_FOUND_OR_INACTIVE = "val.policy.treatment.period.not.found.or.inactive";
    public final static String POLICY_TREATMENT_CATEGORY_PERIOD_NOT_FOUND_OR_INACTIVE = "val.policy.treatment.category.period.not.found.or.inactive";
    public final static String POLICY_MONTH_CATEGORY_PERIOD_NOT_FOUND_OR_INACTIVE = "val.policy.month.category.period.not.found.or.inactive";
    public final static String INSURANCE_DETAILS_NOT_FOUND_OR_INACTIVE = "val.insurance.details.not.found.or.inactive";
    public final static String CLAIM_LIMIT_EXCEED_WITH_LIMIT = "val.claim.limit.exceed.with.limit";
    public final static String STAFF_CLAIM_LIMIT_OR_OUT_OF_EMPLOYEE_REQUEST_EXCEED = "val.claim.normal.staff.claim.limit.or.out.of.employee";
    public final static String COMMON_PARAM_NOT_FOUND = "val.common.param.not.found";
    public final static String OLDER_DATE_INSURANCE_CLAIM_REQUEST = "val.older.date.insurance.claim.request";
    public final static String OLDER_DATE_DEATH_CLAIM_REQUEST = "val.older.date.death.claim.request";
    public final static String CHILD_AGE_DEATH_CLAIM_REQUEST_INVALID = "val.child.age.date.death.claim.request";
    public final static String EMPLOYEE_OLDER_AGE_DATE_DEATH_CLAIM_REQUEST = "val.older.age.dependent.not.eligible.death.request";

    public static final String PERMANENT_DATE_TOO_OLD_MESSAGE = "val.permanent.date.too.old.request";
    public final static String INSURANCE_CLAIM_REQUEST_HISTORY_FILTER_LIST_SUCCESS = "val.insurance.claim.request.history.filter.list.success";
    public final static String INSURANCE_CLAIM_REQUEST_FIND_BY_ID_SUCCESS = "val.insurance.claim.request.find.by.id.success";
    public final static String DEATH_CLAIM_REQUEST_FIND_BY_ID_SUCCESS = "val.death.claim.request.find.by.id.success";
    public final static String DEATH_CLAIM_REQUEST_HISTORY_FILTER_LIST_SUCCESS = "val.death.claim.request.history.filter.list.success";
    public final static String CLAIM_DEPENDENT_NOT_FOUND_OR_FACILITY_NOT_ELIGIBLE = "val.claim.dependent.not.eligible.or.facility.not.eligible";
    public final static String CLAIM_DEPENDENT_DEATH_REQUEST_ALREADY_PROCEED = "val.claim.dependent.death.request.already.proceed";
    public final static String CLAIM_DEPENDENT_INSURANCE_REQUEST_PARENT_AGE_LIMIT_EXCEED = "val.claim.dependent.insurance.request.parent.age.limit.exceed";
    public final static String CLAIM_SENIOR_STAFF_AGE_LIMIT_EXCEED = "val.claim.senior.staff.age.limit.exceed";
    public final static String CLAIM_DEPENDENT_INSURANCE_REQUEST_CHILDREN_AGE_LIMIT_EXCEED = "val.claim.dependent.insurance.request.children.age.limit.exceed";
    public final static String BENEFICIARY_NOT_FOUND_OR_FACILITY_NOT_ELIGIBLE = "val.beneficiary.not.eligible.or.facility.not.eligible";
    public final static String INSURANCE_CLAIMS_REFERENCE_DETAILS_SUCCESS = "val.insurance.claims.reference.details.success";
    public final static String DEATH_CLAIMS_REFERENCE_DETAILS_SUCCESS = "val.death.claims.reference.details.success";
    public final static String INSURANCE_CLAIMS_DIAGNOSIS_MAX_IMAGE_INVALID  = "val.claims.diagnosis.max.image.invalid";
    public final static String INSURANCE_CLAIMS_DIAGNOSIS_MIN_IMAGE_INVALID  = "val.claims.diagnosis.min.image.invalid";
    public final static String INSURANCE_CLAIMS_TREATMENT_MAX_IMAGE_INVALID  = "val.claims.treatment.max.image.invalid";
    public final static String INSURANCE_CLAIMS_TREATMENT_MIN_IMAGE_INVALID  = "val.claims.treatment.min.image.invalid";
    public final static String DEATH_CLAIM_ALREADY_PAID_OR_UNDER_REVIEW  = "val.death.claim.already.paid.or.under.review";
    public final static String DEATH_CLAIMS_DEATH_MAX_IMAGE_INVALID  = "val.death.claims.death.max.image.invalid";
    public final static String DEATH_CLAIMS_DEATH_MIN_IMAGE_INVALID  = "val.death.claims.death.min.image.invalid";
    public final static String DASHBOARD_SUMMARY_SUCCESS  = "val.dashboard.summary.success";
    public final static String INSURANCE_CLAIMS_REQUEST_DETAILS_NOT_FOUND_BY_ID  = "val.insurance.claims.request.details.not.found.by.id";
    public final static String DEATH_CLAIMS_REQUEST_DETAILS_NOT_FOUND_BY_ID  = "val.death.claims.request.details.not.found.by.id";
    public final static String SENIOR_STAFF_CANT_REQUEST_UP_TO_60_AGE = "val.senior.staff.cant.request.up.to60.age";
    public final static String SENIOR_STAFF_CANT_REQUEST_UP_TO_70_AGE = "val.senior.staff.cant.request.up.to70.age";
}
