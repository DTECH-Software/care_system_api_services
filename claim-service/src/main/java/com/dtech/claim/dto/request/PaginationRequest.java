package com.dtech.claim.dto.request;

import com.dtech.claim.dto.request.validator.ChannelRequestValidatorDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;
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