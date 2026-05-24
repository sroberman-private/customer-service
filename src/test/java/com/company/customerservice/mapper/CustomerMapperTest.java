package com.company.customerservice.mapper;

import com.company.customerservice.domain.Address;
import com.company.customerservice.domain.ContactDetail;
import com.company.customerservice.domain.Customer;
import com.company.customerservice.domain.Kyc;
import com.company.customerservice.domain.enums.AddressType;
import com.company.customerservice.domain.enums.ContactType;
import com.company.customerservice.domain.enums.CustomerStatus;
import com.company.customerservice.domain.enums.KycStatus;
import com.company.customerservice.web.dto.*;
import com.company.customerservice.web.mapper.CustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomerMapper")
class CustomerMapperTest {

    private CustomerMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CustomerMapper();
    }

    // ─── toEntity ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toEntity()")
    class ToEntity {

        @Test
        @DisplayName("should always force status to PENDING regardless of request status")
        void shouldForceStatusToPending() {
            CustomerRequest request = buildCustomerRequest();
            request.setStatus(CustomerStatus.ACTIVE);

            Customer entity = mapper.toEntity(request);

            assertThat(entity.getStatus()).isEqualTo(CustomerStatus.PENDING);
        }

        @Test
        @DisplayName("should force status to PENDING even when request status is null")
        void shouldForceStatusToPendingWhenRequestStatusIsNull() {
            CustomerRequest request = buildCustomerRequest();
            request.setStatus(null);

            Customer entity = mapper.toEntity(request);

            assertThat(entity.getStatus()).isEqualTo(CustomerStatus.PENDING);
        }

        @Test
        @DisplayName("should force status to PENDING when request contains SUSPENDED")
        void shouldForceStatusToPendingForSuspended() {
            CustomerRequest request = buildCustomerRequest();
            request.setStatus(CustomerStatus.SUSPENDED);

            Customer entity = mapper.toEntity(request);

            assertThat(entity.getStatus()).isEqualTo(CustomerStatus.PENDING);
        }

        @Test
        @DisplayName("should map all scalar fields from request to entity")
        void shouldMapScalarFields() {
            CustomerRequest request = buildCustomerRequest();

            Customer entity = mapper.toEntity(request);

            assertThat(entity.getFirstName()).isEqualTo("Jane");
            assertThat(entity.getLastName()).isEqualTo("Doe");
            assertThat(entity.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 6, 15));
            assertThat(entity.getNationality()).isEqualTo("British");
        }

        @Test
        @DisplayName("should map null nationality without error")
        void shouldHandleNullNationality() {
            CustomerRequest request = buildCustomerRequest();
            request.setNationality(null);

            Customer entity = mapper.toEntity(request);

            assertThat(entity.getNationality()).isNull();
        }
    }

    // ─── updateEntity ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateEntity()")
    class UpdateEntity {

        @Test
        @DisplayName("should update all scalar fields on the existing customer")
        void shouldUpdateScalarFields() {
            Customer customer = existingCustomer();
            CustomerRequest request = buildCustomerRequest();
            request.setFirstName("Updated");
            request.setLastName("Name");
            request.setStatus(CustomerStatus.ACTIVE);

            mapper.updateEntity(customer, request);

            assertThat(customer.getFirstName()).isEqualTo("Updated");
            assertThat(customer.getLastName()).isEqualTo("Name");
            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        }

        @Test
        @DisplayName("should not update status when request status is null")
        void shouldNotUpdateStatusWhenNull() {
            Customer customer = existingCustomer();
            customer.setStatus(CustomerStatus.ACTIVE);
            CustomerRequest request = buildCustomerRequest();
            request.setStatus(null);

            mapper.updateEntity(customer, request);

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        }

        @Test
        @DisplayName("should update dateOfBirth and nationality from request")
        void shouldUpdateDateOfBirthAndNationality() {
            Customer customer = existingCustomer();
            CustomerRequest request = buildCustomerRequest();
            request.setDateOfBirth(LocalDate.of(1985, 1, 1));
            request.setNationality("German");

            mapper.updateEntity(customer, request);

            assertThat(customer.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 1, 1));
            assertThat(customer.getNationality()).isEqualTo("German");
        }
    }

    // ─── toResponse ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toResponse()")
    class ToResponse {

        @Test
        @DisplayName("should map all customer fields to CustomerResponse")
        void shouldMapAllFieldsToResponse() {
            Customer customer = existingCustomer();
            LocalDateTime now = LocalDateTime.now();
            customer.setCreatedAt(now);
            customer.setUpdatedAt(now);

            CustomerResponse response = mapper.toResponse(customer);

            assertThat(response.getId()).isEqualTo(customer.getId());
            assertThat(response.getFirstName()).isEqualTo("Jane");
            assertThat(response.getLastName()).isEqualTo("Doe");
            assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 6, 15));
            assertThat(response.getNationality()).isEqualTo("British");
            assertThat(response.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(response.getCreatedAt()).isEqualTo(now);
            assertThat(response.getUpdatedAt()).isEqualTo(now);
        }
    }

    // ─── toSummaryResponse ────────────────────────────────────────────────────

    @Nested
    @DisplayName("toSummaryResponse()")
    class ToSummaryResponse {

        @Test
        @DisplayName("should map id, name, status, timestamps — and omit dateOfBirth and nationality")
        void shouldMapSummaryFieldsOnly() {
            Customer customer = existingCustomer();
            LocalDateTime now = LocalDateTime.now();
            customer.setCreatedAt(now);
            customer.setUpdatedAt(now);

            CustomerSummaryResponse summary = mapper.toSummaryResponse(customer);

            assertThat(summary.getId()).isEqualTo(customer.getId());
            assertThat(summary.getFirstName()).isEqualTo("Jane");
            assertThat(summary.getLastName()).isEqualTo("Doe");
            assertThat(summary.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(summary.getCreatedAt()).isEqualTo(now);
            assertThat(summary.getUpdatedAt()).isEqualTo(now);
        }
    }

    // ─── toKycEntity / updateKycEntity ────────────────────────────────────────

    @Nested
    @DisplayName("toKycEntity()")
    class ToKycEntity {

        @Test
        @DisplayName("should map all KYC request fields and associate the customer")
        void shouldMapAllKycFields() {
            Customer customer = existingCustomer();
            KycRequest request = buildKycRequest();

            Kyc kyc = mapper.toKycEntity(request, customer);

            assertThat(kyc.getCustomer()).isSameAs(customer);
            assertThat(kyc.getDocumentType()).isEqualTo("PASSPORT");
            assertThat(kyc.getDocumentNumber()).isEqualTo("AB001234");
            assertThat(kyc.getIssueDate()).isEqualTo(LocalDate.of(2018, 3, 10));
            assertThat(kyc.getExpiryDate()).isEqualTo(LocalDate.of(2028, 3, 10));
            assertThat(kyc.getStatus()).isEqualTo(KycStatus.PENDING_REVIEW);
            assertThat(kyc.getVerifiedBy()).isEqualTo("officer@company.com");
        }
    }

    @Nested
    @DisplayName("updateKycEntity()")
    class UpdateKycEntity {

        @Test
        @DisplayName("should update all mutable KYC fields in place")
        void shouldUpdateKycFields() {
            Kyc kyc = existingKyc(existingCustomer());
            KycRequest request = buildKycRequest();
            request.setDocumentNumber("XY009999");
            request.setStatus(KycStatus.APPROVED);

            mapper.updateKycEntity(kyc, request);

            assertThat(kyc.getDocumentNumber()).isEqualTo("XY009999");
            assertThat(kyc.getStatus()).isEqualTo(KycStatus.APPROVED);
        }
    }

    // ─── toKycResponse — masking ───────────────────────────────────────────────

    @Nested
    @DisplayName("toKycResponse() — document number masking")
    class ToKycResponse {

        @Test
        @DisplayName("should mask document number showing only last 4 characters")
        void shouldMaskDocumentNumberShowingLast4() {
            Kyc kyc = existingKyc(existingCustomer());
            kyc.setDocumentNumber("AB001234");

            KycResponse response = mapper.toKycResponse(kyc);

            assertThat(response.getDocumentNumber()).isEqualTo("****1234");
        }

        @Test
        @DisplayName("should mask a longer document number correctly")
        void shouldMaskLongerDocumentNumber() {
            Kyc kyc = existingKyc(existingCustomer());
            kyc.setDocumentNumber("GBPASSPORT9999");

            KycResponse response = mapper.toKycResponse(kyc);

            assertThat(response.getDocumentNumber()).isEqualTo("****9999");
        }

        @Test
        @DisplayName("should return exactly 4 stars when document number is exactly 4 characters")
        void shouldReturn4StarsForExactly4CharDocNumber() {
            Kyc kyc = existingKyc(existingCustomer());
            kyc.setDocumentNumber("1234");

            KycResponse response = mapper.toKycResponse(kyc);

            // length == 4 falls into the <=4 branch → "****"
            assertThat(response.getDocumentNumber()).isEqualTo("****");
        }

        @Test
        @DisplayName("should return 4 stars when document number has fewer than 4 characters")
        void shouldReturn4StarsForShortDocNumber() {
            Kyc kyc = existingKyc(existingCustomer());
            kyc.setDocumentNumber("AB");

            KycResponse response = mapper.toKycResponse(kyc);

            assertThat(response.getDocumentNumber()).isEqualTo("****");
        }

        @Test
        @DisplayName("should return 4 stars when document number is null")
        void shouldReturn4StarsForNullDocNumber() {
            Kyc kyc = existingKyc(existingCustomer());
            kyc.setDocumentNumber(null);

            KycResponse response = mapper.toKycResponse(kyc);

            assertThat(response.getDocumentNumber()).isEqualTo("****");
        }

        @Test
        @DisplayName("should map all remaining KYC response fields correctly")
        void shouldMapRemainingKycResponseFields() {
            Customer customer = existingCustomer();
            Kyc kyc = existingKyc(customer);
            LocalDateTime verifiedAt = LocalDateTime.of(2024, 1, 10, 12, 0);
            kyc.setVerifiedAt(verifiedAt);

            KycResponse response = mapper.toKycResponse(kyc);

            assertThat(response.getId()).isEqualTo(kyc.getId());
            assertThat(response.getCustomerId()).isEqualTo(customer.getId());
            assertThat(response.getDocumentType()).isEqualTo("PASSPORT");
            assertThat(response.getStatus()).isEqualTo(KycStatus.PENDING_REVIEW);
            assertThat(response.getVerifiedBy()).isEqualTo("officer@company.com");
            assertThat(response.getVerifiedAt()).isEqualTo(verifiedAt);
        }
    }

    // ─── toAddressEntity / updateAddressEntity / toAddressResponse ────────────

    @Nested
    @DisplayName("toAddressEntity()")
    class ToAddressEntity {

        @Test
        @DisplayName("should map all address request fields and associate the customer")
        void shouldMapAllAddressFields() {
            Customer customer = existingCustomer();
            AddressRequest request = buildAddressRequest();

            Address address = mapper.toAddressEntity(request, customer);

            assertThat(address.getCustomer()).isSameAs(customer);
            assertThat(address.getType()).isEqualTo(AddressType.HOME);
            assertThat(address.getStreet()).isEqualTo("10 Downing Street");
            assertThat(address.getCity()).isEqualTo("London");
            assertThat(address.getState()).isEqualTo("Greater London");
            assertThat(address.getPostalCode()).isEqualTo("SW1A 2AA");
            assertThat(address.getCountry()).isEqualTo("GB");
            assertThat(address.isPrimary()).isTrue();
        }
    }

    @Nested
    @DisplayName("updateAddressEntity()")
    class UpdateAddressEntity {

        @Test
        @DisplayName("should update all address fields in place")
        void shouldUpdateAddressFields() {
            Customer customer = existingCustomer();
            Address address = mapper.toAddressEntity(buildAddressRequest(), customer);
            AddressRequest updated = buildAddressRequest();
            updated.setCity("Manchester");
            updated.setPrimary(false);

            mapper.updateAddressEntity(address, updated);

            assertThat(address.getCity()).isEqualTo("Manchester");
            assertThat(address.isPrimary()).isFalse();
        }
    }

    @Nested
    @DisplayName("toAddressResponse()")
    class ToAddressResponse {

        @Test
        @DisplayName("should map all address entity fields to AddressResponse")
        void shouldMapAllAddressResponseFields() {
            Customer customer = existingCustomer();
            Address address = buildAddressEntity(customer);

            AddressResponse response = mapper.toAddressResponse(address);

            assertThat(response.getId()).isEqualTo(address.getId());
            assertThat(response.getCustomerId()).isEqualTo(customer.getId());
            assertThat(response.getType()).isEqualTo(AddressType.HOME);
            assertThat(response.getStreet()).isEqualTo("10 Downing Street");
            assertThat(response.getCity()).isEqualTo("London");
            assertThat(response.getCountry()).isEqualTo("GB");
            assertThat(response.isPrimary()).isTrue();
        }
    }

    // ─── toContactEntity / updateContactEntity / toContactResponse ────────────

    @Nested
    @DisplayName("toContactEntity()")
    class ToContactEntity {

        @Test
        @DisplayName("should always set verified to false regardless of any external state")
        void shouldAlwaysSetVerifiedToFalse() {
            Customer customer = existingCustomer();
            ContactRequest request = buildContactRequest();

            ContactDetail contact = mapper.toContactEntity(request, customer);

            assertThat(contact.isVerified()).isFalse();
        }

        @Test
        @DisplayName("should map type, value, primary, and customer from the request")
        void shouldMapContactFields() {
            Customer customer = existingCustomer();
            ContactRequest request = buildContactRequest();

            ContactDetail contact = mapper.toContactEntity(request, customer);

            assertThat(contact.getCustomer()).isSameAs(customer);
            assertThat(contact.getType()).isEqualTo(ContactType.EMAIL);
            assertThat(contact.getValue()).isEqualTo("jane.doe@example.com");
            assertThat(contact.isPrimary()).isTrue();
        }
    }

    @Nested
    @DisplayName("updateContactEntity()")
    class UpdateContactEntity {

        @Test
        @DisplayName("should update type, value, primary — but never touch verified")
        void shouldNeverTouchVerifiedOnUpdate() {
            Customer customer = existingCustomer();
            // Start with a verified contact
            ContactDetail contact = ContactDetail.builder()
                    .id(UUID.randomUUID())
                    .customer(customer)
                    .type(ContactType.EMAIL)
                    .value("old@example.com")
                    .primary(false)
                    .verified(true)
                    .build();

            ContactRequest request = buildContactRequest();
            request.setValue("new@example.com");

            mapper.updateContactEntity(contact, request);

            // verified must remain unchanged
            assertThat(contact.isVerified()).isTrue();
            assertThat(contact.getValue()).isEqualTo("new@example.com");
        }

        @Test
        @DisplayName("should not promote verified from false to true during update")
        void shouldNotPromoteVerifiedDuringUpdate() {
            Customer customer = existingCustomer();
            ContactDetail contact = ContactDetail.builder()
                    .id(UUID.randomUUID())
                    .customer(customer)
                    .type(ContactType.PHONE)
                    .value("+44 7700 900000")
                    .primary(false)
                    .verified(false)
                    .build();

            ContactRequest request = buildContactRequest();

            mapper.updateContactEntity(contact, request);

            assertThat(contact.isVerified()).isFalse();
        }

        @Test
        @DisplayName("should update all mutable contact fields")
        void shouldUpdateMutableContactFields() {
            Customer customer = existingCustomer();
            ContactDetail contact = ContactDetail.builder()
                    .id(UUID.randomUUID())
                    .customer(customer)
                    .type(ContactType.EMAIL)
                    .value("old@example.com")
                    .primary(false)
                    .verified(false)
                    .build();
            ContactRequest request = buildContactRequest();
            request.setType(ContactType.MOBILE);
            request.setValue("+44 7700 900001");
            request.setPrimary(true);

            mapper.updateContactEntity(contact, request);

            assertThat(contact.getType()).isEqualTo(ContactType.MOBILE);
            assertThat(contact.getValue()).isEqualTo("+44 7700 900001");
            assertThat(contact.isPrimary()).isTrue();
        }
    }

    @Nested
    @DisplayName("toContactResponse()")
    class ToContactResponse {

        @Test
        @DisplayName("should map all contact entity fields to ContactResponse")
        void shouldMapAllContactResponseFields() {
            Customer customer = existingCustomer();
            ContactDetail contact = ContactDetail.builder()
                    .id(UUID.randomUUID())
                    .customer(customer)
                    .type(ContactType.EMAIL)
                    .value("jane.doe@example.com")
                    .primary(true)
                    .verified(false)
                    .build();

            ContactResponse response = mapper.toContactResponse(contact);

            assertThat(response.getId()).isEqualTo(contact.getId());
            assertThat(response.getCustomerId()).isEqualTo(customer.getId());
            assertThat(response.getType()).isEqualTo(ContactType.EMAIL);
            assertThat(response.getValue()).isEqualTo("jane.doe@example.com");
            assertThat(response.isPrimary()).isTrue();
            assertThat(response.isVerified()).isFalse();
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private CustomerRequest buildCustomerRequest() {
        CustomerRequest req = new CustomerRequest();
        req.setFirstName("Jane");
        req.setLastName("Doe");
        req.setDateOfBirth(LocalDate.of(1990, 6, 15));
        req.setNationality("British");
        return req;
    }

    private Customer existingCustomer() {
        return Customer.builder()
                .id(UUID.randomUUID())
                .firstName("Jane")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 6, 15))
                .nationality("British")
                .status(CustomerStatus.ACTIVE)
                .build();
    }

    private KycRequest buildKycRequest() {
        KycRequest req = new KycRequest();
        req.setDocumentType("PASSPORT");
        req.setDocumentNumber("AB001234");
        req.setIssueDate(LocalDate.of(2018, 3, 10));
        req.setExpiryDate(LocalDate.of(2028, 3, 10));
        req.setStatus(KycStatus.PENDING_REVIEW);
        req.setVerifiedBy("officer@company.com");
        return req;
    }

    private Kyc existingKyc(Customer customer) {
        return Kyc.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .documentType("PASSPORT")
                .documentNumber("AB001234")
                .issueDate(LocalDate.of(2018, 3, 10))
                .expiryDate(LocalDate.of(2028, 3, 10))
                .status(KycStatus.PENDING_REVIEW)
                .verifiedBy("officer@company.com")
                .build();
    }

    private AddressRequest buildAddressRequest() {
        AddressRequest req = new AddressRequest();
        req.setType(AddressType.HOME);
        req.setStreet("10 Downing Street");
        req.setCity("London");
        req.setState("Greater London");
        req.setPostalCode("SW1A 2AA");
        req.setCountry("GB");
        req.setPrimary(true);
        return req;
    }

    private Address buildAddressEntity(Customer customer) {
        return Address.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .type(AddressType.HOME)
                .street("10 Downing Street")
                .city("London")
                .state("Greater London")
                .postalCode("SW1A 2AA")
                .country("GB")
                .primary(true)
                .build();
    }

    private ContactRequest buildContactRequest() {
        ContactRequest req = new ContactRequest();
        req.setType(ContactType.EMAIL);
        req.setValue("jane.doe@example.com");
        req.setPrimary(true);
        return req;
    }
}
