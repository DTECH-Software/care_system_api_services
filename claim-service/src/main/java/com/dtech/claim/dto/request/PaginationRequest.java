package com.dtech.claim.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Sort;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationRequest<T> {
    @Builder.Default
    private Integer page = 1;
    @Builder.Default
    private Integer size = 10;
    @Builder.Default
    private String sortColumn = "lastModifiedDate";
    @Builder.Default
    private Sort.Direction sortDirection = Sort.Direction.DESC;
    private T search;
}