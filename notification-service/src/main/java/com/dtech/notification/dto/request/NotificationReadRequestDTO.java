/**
 * User: Himal_J
 * Date: 4/1/2025
 * Time: 12:52 PM
 * <p>
 */

package com.dtech.notification.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class NotificationReadRequestDTO extends ChannelRequestDTO{
    private Long id;
}
