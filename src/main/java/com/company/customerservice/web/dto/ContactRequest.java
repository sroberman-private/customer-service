package com.company.customerservice.web.dto;

import com.company.customerservice.domain.enums.ContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContactRequest {

    @NotNull(message = "Contact type is required")
    private ContactType type;

    @NotBlank(message = "Contact value is required")
    @Size(max = 255, message = "Contact value must not exceed 255 characters")
    private String value;

    private boolean primary;
}
