/**
 * User: Himal_J
 * Date: 4/1/2025
 * Time: 8:41 AM
 * <p>
 */

package com.dtech.notification.service.impl;


import com.dtech.notification.dto.request.NotificationHistory;
import com.dtech.notification.dto.request.PaginationRequest;
import com.dtech.notification.dto.response.ApiResponse;
import com.dtech.notification.dto.response.NotificationHistoryResponseDTO;
import com.dtech.notification.dto.response.PagingResult;
import com.dtech.notification.enums.NotificationTitle;
import com.dtech.notification.enums.Status;
import com.dtech.notification.repository.ApplicationUserRepository;
import com.dtech.notification.repository.NotificationHistoryRepository;
import com.dtech.notification.service.InAppNotificationService;
import com.dtech.notification.specifications.NotificationHistorySpecification;
import com.dtech.notification.util.DateTimeUtil;
import com.dtech.notification.util.PaginationUtil;
import com.dtech.notification.util.ResponseMessageUtil;
import com.dtech.notification.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Log4j2
@Service
@RequiredArgsConstructor
public class InAppNotificationServiceImpl implements InAppNotificationService {

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final NotificationHistoryRepository notificationHistoryRepository;

    @Autowired
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> inAppNotificationHistory(PaginationRequest<NotificationHistory> paginationRequest, Locale locale) {
        try {
            log.info("In App Notification Service filter list {}",paginationRequest);
            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(paginationRequest.getUsername().trim(), Status.ACTIVE).map((user) -> {

                Pageable pageable = PaginationUtil.getPageable(paginationRequest);

                Page<com.dtech.notification.model.NotificationHistory> notificationHistories = Objects.nonNull(paginationRequest.getSearch()) ?
                        notificationHistoryRepository.findAll(NotificationHistorySpecification.getSpecification(paginationRequest.getSearch(), user.getId()), pageable) :
                        notificationHistoryRepository.findAll(NotificationHistorySpecification.getSpecification(user.getId()), pageable);
                log.info("Filter records notification {}", notificationHistories);
                long totalElements = Objects.nonNull(paginationRequest.getSearch()) ?
                        notificationHistoryRepository.count(NotificationHistorySpecification.getSpecification(paginationRequest.getSearch(), user.getId())) :
                        notificationHistoryRepository.count(NotificationHistorySpecification.getSpecification(user.getId()));
                log.info("Total elements count records notification {}", totalElements);
                log.info("Filter list data fetching death success");
                List<NotificationHistoryResponseDTO> collectList = notificationHistories.stream()
                        .map(val ->  {
                            NotificationHistoryResponseDTO map = modelMapper.map(val, NotificationHistoryResponseDTO.class);
                            map.setTitleDescription(NotificationTitle.valueOf(map.getTitle()).getDescription());
                            log.info("Call days count");
                            long daysDifference = DateTimeUtil.getDaysDifference(val.getCreatedDate());
                            map.setAgoDays(daysDifference);
                            return map;
                        }).toList();
                log.info("Filter list notification {} success", collectList);
                return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<NotificationHistoryResponseDTO>(collectList, collectList.size(), totalElements),
                        messageSource.getMessage(ResponseMessageUtil.NOTIFICATION_FILTER_LIST_SUCCESS,
                                null, locale)));

            }).orElseGet(() -> {
                log.info("User notification filter request user not found {} ", paginationRequest);
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
            });
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
