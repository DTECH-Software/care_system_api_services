package com.dtech.claim.dto.request;

import com.dtech.claim.dto.request.validator.ChannelRequestValidatorDTO;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Sort;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginationRequest<T> extends ChannelRequestValidatorDTO {
    @Min(value = 0, message = "Page size must be zero or a positive number")
    private Integer page = 0;
    @Min(value = 10, message = "Page size must be ten or a positive number")
    private Integer size = 10;
    private String sortColumn = "lastModifiedDate";
    private Sort.Direction sortDirection = Sort.Direction.DESC;
    private T search;
}