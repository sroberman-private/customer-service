package com.company.customerservice.web.dto;

import com.company.customerservice.domain.enums.ContactType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ContactResponse {

    private UUID id;
    private UUID customerId;
    private ContactType type;
    private String value;
    private boolean primary;
    private boolean verified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
