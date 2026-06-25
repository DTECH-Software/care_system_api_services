package com.dtech.claim.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AssistedEmployeeSelectRequestDTO extends ChannelRequestDTO {
    private String epfNo;
    private String company;
}
