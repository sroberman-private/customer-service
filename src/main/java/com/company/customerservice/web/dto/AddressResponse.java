package com.company.customerservice.web.dto;

import com.company.customerservice.domain.enums.AddressType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AddressResponse {

    private UUID id;
    private UUID customerId;
    private AddressType type;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private boolean primary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
