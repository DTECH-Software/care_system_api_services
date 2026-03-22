package com.dtech.auth.service.impl;

import com.dtech.auth.enums.Status;
import com.dtech.auth.enums.Workflow;
import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.ClaimsDependents;
import com.dtech.auth.repository.ApplicationUserRepository;
import com.dtech.auth.repository.ClaimDependentsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class RejoinProcessingService {

    private final ApplicationUserRepository applicationUserRepository;
    private final ClaimDependentsRepository claimDependentsRepository;

    @Transactional
    public void processOnSignup(ApplicationUser newApplicationUser) {
        if (newApplicationUser == null || newApplicationUser.getUserPersonalDetails() == null) {
            return;
        }

        String nic = normalize(newApplicationUser.getUserPersonalDetails().getNic());
        String newEpf = normalize(newApplicationUser.getUserPersonalDetails().getEpfNo());
        if (nic == null || newApplicationUser.getId() == null) {
            return;
        }

        Optional<ApplicationUser> previousUserOptional = applicationUserRepository
                .findTopByUserPersonalDetails_NicIgnoreCaseAndUserPersonalDetails_UserStatusAndIdNotOrderByIdDesc(
                        nic,
                        Status.INACTIVE,
                        newApplicationUser.getId()
                );

        if (previousUserOptional.isEmpty()) {
            log.info("No inactive rejoin profile found for NIC {}", nic);
            return;
        }

        ApplicationUser previousUser = previousUserOptional.get();
        String previousEpf = previousUser.getUserPersonalDetails() != null
                ? normalize(previousUser.getUserPersonalDetails().getEpfNo())
                : null;

        if (newEpf != null && newEpf.equalsIgnoreCase(previousEpf)) {
            log.info("Skipping automatic rejoin processing because EPF is unchanged. nic={}, epf={}", nic, newEpf);
            return;
        }

        List<ClaimsDependents> transferableDependents = claimDependentsRepository
                .findByApplicationUserAndStatusAndLiveStatus(previousUser, Workflow.APPROVED, true);

        if (transferableDependents.isEmpty()) {
            log.info("No approved live dependents found for previous profile. previousUserId={}, newUserId={}",
                    previousUser.getId(), newApplicationUser.getId());
            return;
        }

        for (ClaimsDependents dependent : transferableDependents) {
            dependent.setApplicationUser(newApplicationUser);
        }
        claimDependentsRepository.saveAll(transferableDependents);

        if (newApplicationUser.isExpectingDependentsRegister()) {
            newApplicationUser.setExpectingDependentsRegister(false);
            applicationUserRepository.saveAndFlush(newApplicationUser);
        }

        log.info("Automatic rejoin processing completed. previousUserId={}, newUserId={}, dependentsMoved={}",
                previousUser.getId(), newApplicationUser.getId(), transferableDependents.size());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
