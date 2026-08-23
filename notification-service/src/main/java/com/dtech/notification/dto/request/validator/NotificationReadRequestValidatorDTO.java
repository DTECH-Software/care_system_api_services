/**
 * User: Himal_J
 * Date: 4/1/2025
 * Time: 1:11 PM
 * <p>
 */

package com.dtech.notification.dto.request.validator;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class NotificationReadRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotNull(message = "Notification id is required.")
    @Positive(message = "Id must be a positive value.")
    private Long id;
}
