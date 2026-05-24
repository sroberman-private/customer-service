package com.company.customerservice.web.dto;

import com.company.customerservice.domain.enums.CustomerStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CustomerSummaryResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private CustomerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
