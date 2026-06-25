package com.dtech.auth.service;

import com.dtech.auth.dto.request.ChannelRequestDTO;
import com.dtech.auth.enums.Status;
import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.UserPersonalDetails;
import com.dtech.auth.repository.ApplicationUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssistedUserResolver {
    private final ApplicationUserRepository applicationUserRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Optional<ApplicationUser> resolve(ChannelRequestDTO request) {
        if (Boolean.TRUE.equals(request.getAssistedMode())) {
            return resolveAssisted(request.getUsername(), request.getActingEmployeeId());
        }
        if (request.getUsername() == null) {
            return Optional.empty();
        }
        return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(request.getUsername().trim(), Status.ACTIVE);
    }

    private Optional<ApplicationUser> resolveAssisted(String username, Long actingEmployeeId) {
        if (actingEmployeeId == null || !isHrUser(username)) {
            return Optional.empty();
        }
        return applicationUserRepository.findByIdAndUserPersonalDetails_UserStatus(actingEmployeeId, Status.ACTIVE)
                .filter(user -> hasCompanyAccess(username, getCompanyCode(user.getUserPersonalDetails())));
    }

    private boolean isHrUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM web_user wu
                JOIN web_user_role wur ON wu.user_role = wur.code
                WHERE wu.username = :username
                  AND wu.status = 'ACTIVE'
                  AND wu.login_status = 'ACTIVE'
                  AND UPPER(wur.code) IN ('HRADMIN', 'HR', 'SUPERADMIN')
                """)
                .setParameter("username", username.trim())
                .getSingleResult();
        return count.longValue() > 0;
    }

    private boolean hasCompanyAccess(String username, String companyCode) {
        if (username == null || companyCode == null) {
            return false;
        }
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM web_user wu
                WHERE wu.username = :username
                  AND wu.status = 'ACTIVE'
                  AND wu.login_status = 'ACTIVE'
                  AND (
                      NOT EXISTS (SELECT 1 FROM web_user_company wuc_all WHERE wuc_all.web_user_id = wu.id)
                      OR EXISTS (
                          SELECT 1
                          FROM web_user_company wuc
                          JOIN company_types ct ON ct.id = wuc.company_id
                          WHERE wuc.web_user_id = wu.id
                            AND ct.code = :companyCode
                      )
                  )
                """)
                .setParameter("username", username.trim())
                .setParameter("companyCode", companyCode)
                .getSingleResult();
        return count.longValue() > 0;
    }

    private String getCompanyCode(UserPersonalDetails userPersonalDetails) {
        if (userPersonalDetails == null
                || userPersonalDetails.getUserCompanyDetails() == null
                || userPersonalDetails.getUserCompanyDetails().getCompanyTypes() == null) {
            return null;
        }
        return userPersonalDetails.getUserCompanyDetails().getCompanyTypes().getCode();
    }
}
