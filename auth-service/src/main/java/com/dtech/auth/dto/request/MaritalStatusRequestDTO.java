package com.dtech.auth.dto.request;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class MaritalStatusRequestDTO extends ChannelRequestDTO{
    private String requestType;
    private List<SupportingDocumentDTO> documents;
}
