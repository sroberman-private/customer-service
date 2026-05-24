package com.company.customerservice.web.mapper;

import com.company.customerservice.domain.Address;
import com.company.customerservice.domain.ContactDetail;
import com.company.customerservice.domain.Customer;
import com.company.customerservice.domain.Kyc;
import com.company.customerservice.domain.enums.CustomerStatus;
import com.company.customerservice.web.dto.*;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequest request) {
        return Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .nationality(request.getNationality())
                .status(CustomerStatus.PENDING)
                .build();
    }

    public void updateEntity(Customer customer, CustomerRequest request) {
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setNationality(request.getNationality());
        if (request.getStatus() != null) {
            customer.setStatus(request.getStatus());
        }
    }

    public CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .dateOfBirth(customer.getDateOfBirth())
                .nationality(customer.getNationality())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    public CustomerSummaryResponse toSummaryResponse(Customer customer) {
        return CustomerSummaryResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    public Kyc toKycEntity(KycRequest request, Customer customer) {
        return Kyc.builder()
                .customer(customer)
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
                .status(request.getStatus())
                .verifiedBy(request.getVerifiedBy())
                .build();
    }

    public void updateKycEntity(Kyc kyc, KycRequest request) {
        kyc.setDocumentType(request.getDocumentType());
        kyc.setDocumentNumber(request.getDocumentNumber());
        kyc.setIssueDate(request.getIssueDate());
        kyc.setExpiryDate(request.getExpiryDate());
        kyc.setStatus(request.getStatus());
        kyc.setVerifiedBy(request.getVerifiedBy());
    }

    public KycResponse toKycResponse(Kyc kyc) {
        return KycResponse.builder()
                .id(kyc.getId())
                .customerId(kyc.getCustomer().getId())
                .documentType(kyc.getDocumentType())
                .documentNumber(maskDocumentNumber(kyc.getDocumentNumber()))
                .issueDate(kyc.getIssueDate())
                .expiryDate(kyc.getExpiryDate())
                .status(kyc.getStatus())
                .verifiedBy(kyc.getVerifiedBy())
                .verifiedAt(kyc.getVerifiedAt())
                .createdAt(kyc.getCreatedAt())
                .updatedAt(kyc.getUpdatedAt())
                .build();
    }

    public Address toAddressEntity(AddressRequest request, Customer customer) {
        return Address.builder()
                .customer(customer)
                .type(request.getType())
                .street(request.getStreet())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .primary(request.isPrimary())
                .build();
    }

    public void updateAddressEntity(Address address, AddressRequest request) {
        address.setType(request.getType());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setPrimary(request.isPrimary());
    }

    public AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .customerId(address.getCustomer().getId())
                .type(address.getType())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .primary(address.isPrimary())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    public ContactDetail toContactEntity(ContactRequest request, Customer customer) {
        return ContactDetail.builder()
                .customer(customer)
                .type(request.getType())
                .value(request.getValue())
                .primary(request.isPrimary())
                .verified(false)
                .build();
    }

    public void updateContactEntity(ContactDetail contact, ContactRequest request) {
        contact.setType(request.getType());
        contact.setValue(request.getValue());
        contact.setPrimary(request.isPrimary());
    }

    public ContactResponse toContactResponse(ContactDetail contact) {
        return ContactResponse.builder()
                .id(contact.getId())
                .customerId(contact.getCustomer().getId())
                .type(contact.getType())
                .value(contact.getValue())
                .primary(contact.isPrimary())
                .verified(contact.isVerified())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .build();
    }

    private String maskDocumentNumber(String documentNumber) {
        if (documentNumber == null || documentNumber.length() <= 4) {
            return "****";
        }
        return "****" + documentNumber.substring(documentNumber.length() - 4);
    }
}
