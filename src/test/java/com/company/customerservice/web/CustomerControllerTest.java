package com.company.customerservice.web;

import com.company.customerservice.domain.enums.AddressType;
import com.company.customerservice.domain.enums.ContactType;
import com.company.customerservice.domain.enums.CustomerStatus;
import com.company.customerservice.domain.enums.KycStatus;
import com.company.customerservice.exception.ResourceNotFoundException;
import com.company.customerservice.service.CustomerServiceImpl;
import com.company.customerservice.web.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerController.class)
@DisplayName("CustomerController")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerServiceImpl customerService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/customers";

    // ─── POST /api/v1/customers ───────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/customers")
    class CreateCustomer {

        @Test
        @DisplayName("should return 201 with body when request is valid")
        void shouldReturn201ForValidRequest() throws Exception {
            CustomerRequest request = buildCustomerRequest();
            CustomerResponse response = buildCustomerResponse(UUID.randomUUID());

            when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(response.getId().toString()))
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("should return 422 when firstName is blank")
        void shouldReturn422WhenFirstNameIsBlank() throws Exception {
            CustomerRequest request = buildCustomerRequest();
            request.setFirstName("");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.title").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors.firstName").exists());
        }

        @Test
        @DisplayName("should return 422 when lastName is blank")
        void shouldReturn422WhenLastNameIsBlank() throws Exception {
            CustomerRequest request = buildCustomerRequest();
            request.setLastName("");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors.lastName").exists());
        }

        @Test
        @DisplayName("should return 422 when dateOfBirth is missing")
        void shouldReturn422WhenDateOfBirthIsMissing() throws Exception {
            CustomerRequest request = buildCustomerRequest();
            request.setDateOfBirth(null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors.dateOfBirth").exists());
        }

        @Test
        @DisplayName("should return 422 when dateOfBirth is in the future")
        void shouldReturn422WhenDateOfBirthIsInTheFuture() throws Exception {
            CustomerRequest request = buildCustomerRequest();
            request.setDateOfBirth(LocalDate.now().plusDays(1));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors.dateOfBirth").exists());
        }

        @Test
        @DisplayName("should return 422 when firstName exceeds 100 characters")
        void shouldReturn422WhenFirstNameExceedsMaxLength() throws Exception {
            CustomerRequest request = buildCustomerRequest();
            request.setFirstName("A".repeat(101));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors.firstName").exists());
        }

        @Test
        @DisplayName("should return 400 when body contains an invalid enum value for status")
        void shouldReturn400ForInvalidEnumValue() throws Exception {
            String invalidJson = """
                    {
                      "firstName": "Jane",
                      "lastName": "Doe",
                      "dateOfBirth": "1990-06-15",
                      "status": "INVALID_STATUS"
                    }
                    """;

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"));
        }

        @Test
        @DisplayName("should return 400 when body is malformed JSON")
        void shouldReturn400ForMalformedJson() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not valid json }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"));
        }

        @Test
        @DisplayName("should accept request without status field (status is optional)")
        void shouldAcceptRequestWithoutStatus() throws Exception {
            String jsonWithoutStatus = """
                    {
                      "firstName": "Jane",
                      "lastName": "Doe",
                      "dateOfBirth": "1990-06-15"
                    }
                    """;
            CustomerResponse response = buildCustomerResponse(UUID.randomUUID());
            when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithoutStatus))
                    .andExpect(status().isCreated());
        }
    }

    // ─── GET /api/v1/customers/{id} ───────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/customers/{id}")
    class GetCustomer {

        @Test
        @DisplayName("should return 200 with CustomerResponse body for an existing customer")
        void shouldReturn200ForExistingCustomer() throws Exception {
            UUID id = UUID.randomUUID();
            CustomerResponse response = buildCustomerResponse(id);

            when(customerService.getCustomerById(id)).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Doe"));
        }

        @Test
        @DisplayName("should return 404 when customer does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(customerService.getCustomerById(id))
                    .thenThrow(new ResourceNotFoundException("Customer", id));

            mockMvc.perform(get(BASE_URL + "/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"))
                    .andExpect(jsonPath("$.type").value("https://api.customer-service.com/errors/not-found"));
        }
    }

    // ─── GET /api/v1/customers ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/customers")
    class ListCustomers {

        @Test
        @DisplayName("should return 200 with a page of CustomerSummaryResponses")
        void shouldReturn200WithPage() throws Exception {
            UUID id = UUID.randomUUID();
            CustomerSummaryResponse summary = CustomerSummaryResponse.builder()
                    .id(id)
                    .firstName("Jane")
                    .lastName("Doe")
                    .status(CustomerStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            Page<CustomerSummaryResponse> page = new PageImpl<>(List.of(summary));

            when(customerService.listCustomers(any())).thenReturn(page);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                    .andExpect(jsonPath("$.content[0].firstName").value("Jane"))
                    .andExpect(jsonPath("$.content[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("should return 200 with an empty page when no customers exist")
        void shouldReturn200WithEmptyPage() throws Exception {
            when(customerService.listCustomers(any())).thenReturn(Page.empty());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        @DisplayName("should not include dateOfBirth or nationality in the list response")
        void shouldNotIncludeDateOfBirthOrNationality() throws Exception {
            CustomerSummaryResponse summary = CustomerSummaryResponse.builder()
                    .id(UUID.randomUUID())
                    .firstName("Jane")
                    .lastName("Doe")
                    .status(CustomerStatus.PENDING)
                    .build();
            when(customerService.listCustomers(any())).thenReturn(new PageImpl<>(List.of(summary)));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].dateOfBirth").doesNotExist())
                    .andExpect(jsonPath("$.content[0].nationality").doesNotExist());
        }
    }

    // ─── PUT /api/v1/customers/{id} ───────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/v1/customers/{id}")
    class UpdateCustomer {

        @Test
        @DisplayName("should return 200 with updated CustomerResponse")
        void shouldReturn200WhenUpdated() throws Exception {
            UUID id = UUID.randomUUID();
            CustomerRequest request = buildCustomerRequest();
            CustomerResponse response = buildCustomerResponse(id);

            when(customerService.updateCustomer(eq(id), any(CustomerRequest.class))).thenReturn(response);

            mockMvc.perform(put(BASE_URL + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()));
        }

        @Test
        @DisplayName("should return 404 when customer does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(customerService.updateCustomer(eq(id), any(CustomerRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Customer", id));

            mockMvc.perform(put(BASE_URL + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCustomerRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 422 when update request fails validation")
        void shouldReturn422WhenValidationFails() throws Exception {
            UUID id = UUID.randomUUID();
            CustomerRequest request = buildCustomerRequest();
            request.setFirstName("");

            mockMvc.perform(put(BASE_URL + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors.firstName").exists());
        }
    }

    // ─── DELETE /api/v1/customers/{id} ────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/customers/{id}")
    class DeleteCustomer {

        @Test
        @DisplayName("should return 204 when customer is soft-deleted successfully")
        void shouldReturn204WhenDeleted() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(customerService).deleteCustomer(id);

            mockMvc.perform(delete(BASE_URL + "/{id}", id))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when customer to delete does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Customer", id))
                    .when(customerService).deleteCustomer(id);

            mockMvc.perform(delete(BASE_URL + "/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── POST /api/v1/customers/{id}/kyc ─────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/customers/{id}/kyc")
    class SubmitKyc {

        @Test
        @DisplayName("should return 201 with KycResponse when request is valid")
        void shouldReturn201ForValidKyc() throws Exception {
            UUID customerId = UUID.randomUUID();
            KycRequest request = buildKycRequest();
            KycResponse response = buildKycResponse(customerId);

            when(customerService.submitKyc(eq(customerId), any(KycRequest.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/{id}/kyc", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                    .andExpect(jsonPath("$.documentNumber").value("****1234"))
                    .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        }

        @Test
        @DisplayName("should return 422 when documentType is blank")
        void shouldReturn422WhenDocumentTypeIsBlank() throws Exception {
            UUID customerId = UUID.randomUUID();
            KycRequest request = buildKycRequest();
            request.setDocumentType("");

            mockMvc.perform(post(BASE_URL + "/{id}/kyc", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors.documentType").exists());
        }

        @Test
        @DisplayName("should return 422 when documentNumber is blank")
        void shouldReturn422WhenDocumentNumberIsBlank() throws Exception {
            UUID customerId = UUID.randomUUID();
            KycRequest request = buildKycRequest();
            request.setDocumentNumber("");

            mockMvc.perform(post(BASE_URL + "/{id}/kyc", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors.documentNumber").exists());
        }

        @Test
        @DisplayName("should return 422 when status is null")
        void shouldReturn422WhenKycStatusIsNull() throws Exception {
            UUID customerId = UUID.randomUUID();
            KycRequest request = buildKycRequest();
            request.setStatus(null);

            mockMvc.perform(post(BASE_URL + "/{id}/kyc", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors.status").exists());
        }

        @Test
        @DisplayName("should return 400 when an invalid KYC status enum value is sent")
        void shouldReturn400ForInvalidKycStatusEnum() throws Exception {
            UUID customerId = UUID.randomUUID();
            String invalidJson = """
                    {
                      "documentType": "PASSPORT",
                      "documentNumber": "AB001234",
                      "status": "NOT_A_REAL_STATUS"
                    }
                    """;

            mockMvc.perform(post(BASE_URL + "/{id}/kyc", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"));
        }

        @Test
        @DisplayName("should return 404 when customer does not exist")
        void shouldReturn404WhenCustomerNotFound() throws Exception {
            UUID customerId = UUID.randomUUID();
            when(customerService.submitKyc(eq(customerId), any(KycRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Customer", customerId));

            mockMvc.perform(post(BASE_URL + "/{id}/kyc", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildKycRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── GET /api/v1/customers/{id}/kyc ──────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/customers/{id}/kyc")
    class GetKyc {

        @Test
        @DisplayName("should return 200 with KycResponse when KYC record exists")
        void shouldReturn200WithKycResponse() throws Exception {
            UUID customerId = UUID.randomUUID();
            KycResponse response = buildKycResponse(customerId);

            when(customerService.getKyc(customerId)).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/{id}/kyc", customerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.documentNumber").value("****1234"));
        }

        @Test
        @DisplayName("should return 404 when KYC record does not exist")
        void shouldReturn404WhenKycNotFound() throws Exception {
            UUID customerId = UUID.randomUUID();
            when(customerService.getKyc(customerId))
                    .thenThrow(new ResourceNotFoundException("KYC record", customerId));

            mockMvc.perform(get(BASE_URL + "/{id}/kyc", customerId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(containsString("KYC record")));
        }
    }

    // ─── POST /api/v1/customers/{id}/addresses ────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/customers/{id}/addresses")
    class AddAddress {

        @Test
        @DisplayName("should return 201 with AddressResponse when request is valid")
        void shouldReturn201ForValidAddress() throws Exception {
            UUID customerId = UUID.randomUUID();
            AddressRequest request = buildAddressRequest();
            AddressResponse response = buildAddressResponse(customerId);

            when(customerService.addAddress(eq(customerId), any(AddressRequest.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/{id}/addresses", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                    .andExpect(jsonPath("$.city").value("London"));
        }

        @Test
        @DisplayName("should return 422 when street is blank")
        void shouldReturn422WhenStreetIsBlank() throws Exception {
            UUID customerId = UUID.randomUUID();
            AddressRequest request = buildAddressRequest();
            request.setStreet("");

            mockMvc.perform(post(BASE_URL + "/{id}/addresses", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors.street").exists());
        }

        @Test
        @DisplayName("should return 422 when address type is null")
        void shouldReturn422WhenAddressTypeIsNull() throws Exception {
            UUID customerId = UUID.randomUUID();
            AddressRequest request = buildAddressRequest();
            request.setType(null);

            mockMvc.perform(post(BASE_URL + "/{id}/addresses", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors.type").exists());
        }

        @Test
        @DisplayName("should return 400 when address type enum value is invalid")
        void shouldReturn400ForInvalidAddressTypeEnum() throws Exception {
            UUID customerId = UUID.randomUUID();
            String invalidJson = """
                    {
                      "type": "ROOFTOP",
                      "street": "10 Downing Street",
                      "city": "London",
                      "postalCode": "SW1A 2AA",
                      "country": "GB"
                    }
                    """;

            mockMvc.perform(post(BASE_URL + "/{id}/addresses", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"));
        }

        @Test
        @DisplayName("should return 404 when customer does not exist")
        void shouldReturn404WhenCustomerNotFound() throws Exception {
            UUID customerId = UUID.randomUUID();
            when(customerService.addAddress(eq(customerId), any(AddressRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Customer", customerId));

            mockMvc.perform(post(BASE_URL + "/{id}/addresses", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildAddressRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── GET /api/v1/customers/{id}/addresses ────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/customers/{id}/addresses")
    class ListAddresses {

        @Test
        @DisplayName("should return 200 with list of addresses")
        void shouldReturn200WithAddressList() throws Exception {
            UUID customerId = UUID.randomUUID();
            AddressResponse response = buildAddressResponse(customerId);

            when(customerService.listAddresses(customerId)).thenReturn(List.of(response));

            mockMvc.perform(get(BASE_URL + "/{id}/addresses", customerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].city").value("London"));
        }

        @Test
        @DisplayName("should return 404 when customer does not exist")
        void shouldReturn404WhenCustomerNotFound() throws Exception {
            UUID customerId = UUID.randomUUID();
            when(customerService.listAddresses(customerId))
                    .thenThrow(new ResourceNotFoundException("Customer", customerId));

            mockMvc.perform(get(BASE_URL + "/{id}/addresses", customerId))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── PUT /api/v1/customers/{id}/addresses/{addrId} ───────────────────────

    @Nested
    @DisplayName("PUT /api/v1/customers/{id}/addresses/{addrId}")
    class UpdateAddress {

        @Test
        @DisplayName("should return 200 with updated AddressResponse")
        void shouldReturn200WhenUpdated() throws Exception {
            UUID customerId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            AddressResponse response = buildAddressResponse(customerId);

            when(customerService.updateAddress(eq(customerId), eq(addressId), any(AddressRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(put(BASE_URL + "/{id}/addresses/{addrId}", customerId, addressId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildAddressRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.city").value("London"));
        }

        @Test
        @DisplayName("should return 404 when address does not exist")
        void shouldReturn404WhenAddressNotFound() throws Exception {
            UUID customerId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            when(customerService.updateAddress(eq(customerId), eq(addressId), any(AddressRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Address", addressId));

            mockMvc.perform(put(BASE_URL + "/{id}/addresses/{addrId}", customerId, addressId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildAddressRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── DELETE /api/v1/customers/{id}/addresses/{addrId} ────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/customers/{id}/addresses/{addrId}")
    class RemoveAddress {

        @Test
        @DisplayName("should return 204 when address is removed successfully")
        void shouldReturn204WhenRemoved() throws Exception {
            UUID customerId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            doNothing().when(customerService).removeAddress(customerId, addressId);

            mockMvc.perform(delete(BASE_URL + "/{id}/addresses/{addrId}", customerId, addressId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when address does not exist")
        void shouldReturn404WhenAddressNotFound() throws Exception {
            UUID customerId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Address", addressId))
                    .when(customerService).removeAddress(customerId, addressId);

            mockMvc.perform(delete(BASE_URL + "/{id}/addresses/{addrId}", customerId, addressId))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── POST /api/v1/customers/{id}/contacts ────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/customers/{id}/contacts")
    class AddContact {

        @Test
        @DisplayName("should return 201 with ContactResponse and verified=false")
        void shouldReturn201WithVerifiedFalse() throws Exception {
            UUID customerId = UUID.randomUUID();
            ContactRequest request = buildContactRequest();
            ContactResponse response = buildContactResponse(customerId, false);

            when(customerService.addContact(eq(customerId), any(ContactRequest.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/{id}/contacts", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.verified").value(false))
                    .andExpect(jsonPath("$.value").value("jane.doe@example.com"));
        }

        @Test
        @DisplayName("should return 422 when contact value is blank")
        void shouldReturn422WhenContactValueIsBlank() throws Exception {
            UUID customerId = UUID.randomUUID();
            ContactRequest request = buildContactRequest();
            request.setValue("");

            mockMvc.perform(post(BASE_URL + "/{id}/contacts", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors.value").exists());
        }

        @Test
        @DisplayName("should return 422 when contact type is null")
        void shouldReturn422WhenContactTypeIsNull() throws Exception {
            UUID customerId = UUID.randomUUID();
            ContactRequest request = buildContactRequest();
            request.setType(null);

            mockMvc.perform(post(BASE_URL + "/{id}/contacts", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.fieldErrors.type").exists());
        }

        @Test
        @DisplayName("should return 400 for invalid contact type enum value")
        void shouldReturn400ForInvalidContactTypeEnum() throws Exception {
            UUID customerId = UUID.randomUUID();
            String invalidJson = """
                    {
                      "type": "FAX",
                      "value": "jane.doe@example.com"
                    }
                    """;

            mockMvc.perform(post(BASE_URL + "/{id}/contacts", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"));
        }

        @Test
        @DisplayName("should return 404 when customer does not exist")
        void shouldReturn404WhenCustomerNotFound() throws Exception {
            UUID customerId = UUID.randomUUID();
            when(customerService.addContact(eq(customerId), any(ContactRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Customer", customerId));

            mockMvc.perform(post(BASE_URL + "/{id}/contacts", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildContactRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── GET /api/v1/customers/{id}/contacts ─────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/customers/{id}/contacts")
    class ListContacts {

        @Test
        @DisplayName("should return 200 with list of contact details")
        void shouldReturn200WithContactList() throws Exception {
            UUID customerId = UUID.randomUUID();
            ContactResponse response = buildContactResponse(customerId, false);

            when(customerService.listContacts(customerId)).thenReturn(List.of(response));

            mockMvc.perform(get(BASE_URL + "/{id}/contacts", customerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].type").value("EMAIL"));
        }

        @Test
        @DisplayName("should return 404 when customer does not exist")
        void shouldReturn404WhenCustomerNotFound() throws Exception {
            UUID customerId = UUID.randomUUID();
            when(customerService.listContacts(customerId))
                    .thenThrow(new ResourceNotFoundException("Customer", customerId));

            mockMvc.perform(get(BASE_URL + "/{id}/contacts", customerId))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── PUT /api/v1/customers/{id}/contacts/{contactId} ─────────────────────

    @Nested
    @DisplayName("PUT /api/v1/customers/{id}/contacts/{contactId}")
    class UpdateContact {

        @Test
        @DisplayName("should return 200 with updated ContactResponse")
        void shouldReturn200WhenUpdated() throws Exception {
            UUID customerId = UUID.randomUUID();
            UUID contactId = UUID.randomUUID();
            ContactResponse response = buildContactResponse(customerId, false);

            when(customerService.updateContact(eq(customerId), eq(contactId), any(ContactRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(put(BASE_URL + "/{id}/contacts/{contactId}", customerId, contactId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildContactRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.value").value("jane.doe@example.com"));
        }

        @Test
        @DisplayName("should return 404 when contact does not exist")
        void shouldReturn404WhenContactNotFound() throws Exception {
            UUID customerId = UUID.randomUUID();
            UUID contactId = UUID.randomUUID();
            when(customerService.updateContact(eq(customerId), eq(contactId), any(ContactRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Contact", contactId));

            mockMvc.perform(put(BASE_URL + "/{id}/contacts/{contactId}", customerId, contactId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildContactRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── DELETE /api/v1/customers/{id}/contacts/{contactId} ──────────────────

    @Nested
    @DisplayName("DELETE /api/v1/customers/{id}/contacts/{contactId}")
    class RemoveContact {

        @Test
        @DisplayName("should return 204 when contact is removed successfully")
        void shouldReturn204WhenRemoved() throws Exception {
            UUID customerId = UUID.randomUUID();
            UUID contactId = UUID.randomUUID();
            doNothing().when(customerService).removeContact(customerId, contactId);

            mockMvc.perform(delete(BASE_URL + "/{id}/contacts/{contactId}", customerId, contactId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when contact does not exist")
        void shouldReturn404WhenContactNotFound() throws Exception {
            UUID customerId = UUID.randomUUID();
            UUID contactId = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Contact", contactId))
                    .when(customerService).removeContact(customerId, contactId);

            mockMvc.perform(delete(BASE_URL + "/{id}/contacts/{contactId}", customerId, contactId))
                    .andExpect(status().isNotFound());
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

    private CustomerResponse buildCustomerResponse(UUID id) {
        return CustomerResponse.builder()
                .id(id)
                .firstName("Jane")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 6, 15))
                .nationality("British")
                .status(CustomerStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private KycRequest buildKycRequest() {
        KycRequest req = new KycRequest();
        req.setDocumentType("PASSPORT");
        req.setDocumentNumber("AB001234");
        req.setStatus(KycStatus.PENDING_REVIEW);
        req.setVerifiedBy("officer@company.com");
        return req;
    }

    private KycResponse buildKycResponse(UUID customerId) {
        return KycResponse.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .documentType("PASSPORT")
                .documentNumber("****1234")
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

    private AddressResponse buildAddressResponse(UUID customerId) {
        return AddressResponse.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
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

    private ContactResponse buildContactResponse(UUID customerId, boolean verified) {
        return ContactResponse.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .type(ContactType.EMAIL)
                .value("jane.doe@example.com")
                .primary(true)
                .verified(verified)
                .build();
    }
}
