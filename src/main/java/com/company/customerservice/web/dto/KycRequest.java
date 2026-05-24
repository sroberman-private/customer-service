package com.company.customerservice.web.dto;

import com.company.customerservice.domain.enums.KycStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class KycRequest {

    @NotBlank(message = "Document type is required")
    @Size(max = 50, message = "Document type must not exceed 50 characters")
    private String documentType;

    @NotBlank(message = "Document number is required")
    @Size(max = 100, message = "Document number must not exceed 100 characters")
    private String documentNumber;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    @NotNull(message = "KYC status is required")
    private KycStatus status;

    @Size(max = 200, message = "Verified-by must not exceed 200 characters")
    private String verifiedBy;
}
