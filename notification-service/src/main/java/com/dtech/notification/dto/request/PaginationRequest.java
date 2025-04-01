package com.dtech.notification.dto.request;

import com.dtech.notification.dto.request.validator.ChannelRequestValidatorDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginationRequest<T> extends ChannelRequestValidatorDTO {
    private Integer page = 1;
    private Integer size = 10;
    private String sortColumn = "lastModifiedDate";
    private Sort.Direction sortDirection = Sort.Direction.ASC;
    private T search;
}