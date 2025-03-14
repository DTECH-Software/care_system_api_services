package com.dtech.claim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagingResult<T> {
    private List<T> content;
    private long totalElements;
    private Integer size;
}