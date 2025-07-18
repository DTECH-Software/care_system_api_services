package com.dtech.auth.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PolicyDocumentRequestDTO extends ChannelRequestDTO{
    private Boolean policy;
}
