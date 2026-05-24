package com.company.customerservice.service;

import com.company.customerservice.domain.Address;
import com.company.customerservice.domain.ContactDetail;
import com.company.customerservice.domain.Customer;
import com.company.customerservice.domain.Kyc;
import com.company.customerservice.domain.enums.AddressType;
import com.company.customerservice.domain.enums.ContactType;
import com.company.customerservice.domain.enums.CustomerStatus;
import com.company.customerservice.domain.enums.KycStatus;
import com.company.customerservice.exception.ResourceNotFoundException;
import com.company.customerservice.repository.AddressRepository;
import com.company.customerservice.repository.ContactDetailRepository;
import com.company.customerservice.repository.CustomerRepository;
import com.company.customerservice.repository.KycRepository;
import com.company.customerservice.web.dto.*;
import com.company.customerservice.web.mapper.CustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerServiceImpl")
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private KycRepository kycRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private ContactDetailRepository contactDetailRepository;
    @Mock
    private CustomerMapper customerMapper;

    private CustomerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerServiceImpl(
                customerRepository,
                kycRepository,
                addressRepository,
                contactDetailRepository,
                customerMapper);
    }

    // ─── createCustomer ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("createCustomer()")
    class CreateCustomer {

        @Test
        @DisplayName("should map request to entity, save it, and return mapped response")
        void shouldCreateAndReturnCustomer() {
            CustomerRequest request = buildCustomerRequest();
            Customer entity = buildCustomer(UUID.randomUUID());
            CustomerResponse response = buildCustomerResponse(entity);

            when(customerMapper.toEntity(request)).thenReturn(entity);
            when(customerRepository.save(entity)).thenReturn(entity);
            when(customerMapper.toResponse(entity)).thenReturn(response);

            CustomerResponse result = service.createCustomer(request);

            assertThat(result).isEqualTo(response);
            verify(customerMapper).toEntity(request);
            verify(customerRepository).save(entity);
            verify(customerMapper).toResponse(entity);
        }
    }

    // ─── getCustomerById ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCustomerById()")
    class GetCustomerById {

        @Test
        @DisplayName("should return mapped response for an active customer")
        void shouldReturnCustomerWhenActive() {
            UUID id = UUID.randomUUID();
            Customer customer = buildCustomer(id);
            CustomerResponse response = buildCustomerResponse(customer);

            when(customerRepository.findActiveById(id)).thenReturn(Optional.of(customer));
            when(customerMapper.toResponse(customer)).thenReturn(response);

            CustomerResponse result = service.getCustomerById(id);

            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when customer does not exist")
        void shouldThrowWhenCustomerNotFound() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findActiveById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCustomerById(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for soft-deleted customer")
        void shouldThrowForSoftDeletedCustomer() {
            UUID id = UUID.randomUUID();
            // findActiveById returns empty because deletedAt is set
            when(customerRepository.findActiveById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCustomerById(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── listCustomers ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listCustomers()")
    class ListCustomers {

        @Test
        @DisplayName("should return a page of CustomerSummaryResponses mapped from active customers")
        void shouldReturnPageOfSummaries() {
            Pageable pageable = PageRequest.of(0, 20);
            Customer customer = buildCustomer(UUID.randomUUID());
            CustomerSummaryResponse summary = buildSummaryResponse(customer);
            Page<Customer> customerPage = new PageImpl<>(List.of(customer));

            when(customerRepository.findAllActive(pageable)).thenReturn(customerPage);
            when(customerMapper.toSummaryResponse(customer)).thenReturn(summary);

            Page<CustomerSummaryResponse> result = service.listCustomers(pageable);

            assertThat(result.getContent()).containsExactly(summary);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return an empty page when no active customers exist")
        void shouldReturnEmptyPageWhenNoneActive() {
            Pageable pageable = PageRequest.of(0, 20);
            when(customerRepository.findAllActive(pageable)).thenReturn(Page.empty());

            Page<CustomerSummaryResponse> result = service.listCustomers(pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ─── updateCustomer ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateCustomer()")
    class UpdateCustomer {

        @Test
        @DisplayName("should find customer, update it, save, and return mapped response")
        void shouldUpdateAndReturnCustomer() {
            UUID id = UUID.randomUUID();
            Customer customer = buildCustomer(id);
            CustomerRequest request = buildCustomerRequest();
            CustomerResponse response = buildCustomerResponse(customer);

            when(customerRepository.findActiveById(id)).thenReturn(Optional.of(customer));
            when(customerRepository.save(customer)).thenReturn(customer);
            when(customerMapper.toResponse(customer)).thenReturn(response);

            CustomerResponse result = service.updateCustomer(id, request);

            assertThat(result).isEqualTo(response);
            verify(customerMapper).updateEntity(customer, request);
            verify(customerRepository).save(customer);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when updating a non-existent customer")
        void shouldThrowWhenCustomerNotFound() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findActiveById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCustomer(id, buildCustomerRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    // ─── deleteCustomer ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteCustomer()")
    class DeleteCustomer {

        @Test
        @DisplayName("should soft-delete the customer by setting deletedAt and saving")
        void shouldSoftDeleteCustomer() {
            UUID id = UUID.randomUUID();
            Customer customer = buildCustomer(id);

            when(customerRepository.findActiveById(id)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenReturn(customer);

            service.deleteCustomer(id);

            assertThat(customer.getDeletedAt()).isNotNull();
            verify(customerRepository).save(customer);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when trying to delete a non-existent customer")
        void shouldThrowWhenCustomerNotFound() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findActiveById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteCustomer(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── submitKyc ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitKyc()")
    class SubmitKyc {

        @Test
        @DisplayName("should create a new KYC entity when the customer has no existing KYC")
        void shouldCreateNewKycWhenNoneExists() {
            UUID customerId = UUID.randomUUID();
            Customer customer = buildCustomer(customerId);
            customer = spy(customer);
            when(customer.getKyc()).thenReturn(null);

            KycRequest request = buildKycRequest(KycStatus.PENDING_REVIEW);
            Kyc newKyc = buildKyc(customer, KycStatus.PENDING_REVIEW, null);
            KycResponse response = buildKycResponse(newKyc);

            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(customer));
            when(customerMapper.toKycEntity(request, customer)).thenReturn(newKyc);
            when(kycRepository.save(newKyc)).thenReturn(newKyc);
            when(customerMapper.toKycResponse(newKyc)).thenReturn(response);

            KycResponse result = service.submitKyc(customerId, request);

            assertThat(result).isEqualTo(response);
            verify(customerMapper).toKycEntity(request, customer);
            verify(customerMapper, never()).updateKycEntity(any(), any());
        }

        @Test
        @DisplayName("should update the existing KYC entity when the customer already has KYC")
        void shouldUpdateExistingKyc() {
            UUID customerId = UUID.randomUUID();
            Customer customer = buildCustomer(customerId);
            Kyc existingKyc = buildKyc(customer, KycStatus.PENDING_REVIEW, null);
            customer = spy(customer);
            when(customer.getKyc()).thenReturn(existingKyc);

            KycRequest request = buildKycRequest(KycStatus.PENDING_REVIEW);
            KycResponse response = buildKycResponse(existingKyc);

            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(customer));
            when(kycRepository.save(existingKyc)).thenReturn(existingKyc);
            when(customerMapper.toKycResponse(existingKyc)).thenReturn(response);

            service.submitKyc(customerId, request);

            verify(customerMapper).updateKycEntity(existingKyc, request);
            verify(customerMapper, never()).toKycEntity(any(), any());
        }

        @Test
        @DisplayName("should stamp verifiedAt when approving KYC for the first time (verifiedAt is null)")
        void shouldStampVerifiedAtOnFirstApproval() {
            UUID customerId = UUID.randomUUID();
            Customer customer = buildCustomer(customerId);
            Kyc kyc = buildKyc(customer, KycStatus.PENDING_REVIEW, null);
            Customer customerSpy = spy(customer);
            when(customerSpy.getKyc()).thenReturn(kyc);

            KycRequest request = buildKycRequest(KycStatus.APPROVED);
            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(customerSpy));
            when(kycRepository.save(any(Kyc.class))).thenAnswer(inv -> inv.getArgument(0));
            when(customerMapper.toKycResponse(any(Kyc.class))).thenReturn(buildKycResponse(kyc));

            service.submitKyc(customerId, request);

            assertThat(kyc.getVerifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("should NOT overwrite verifiedAt on re-approval when verifiedAt is already set")
        void shouldNotOverwriteVerifiedAtOnReApproval() {
            UUID customerId = UUID.randomUUID();
            Customer customer = buildCustomer(customerId);
            LocalDateTime originalTimestamp = LocalDateTime.of(2024, 1, 10, 9, 0);
            Kyc kyc = buildKyc(customer, KycStatus.APPROVED, originalTimestamp);
            Customer customerSpy = spy(customer);
            when(customerSpy.getKyc()).thenReturn(kyc);

            KycRequest request = buildKycRequest(KycStatus.APPROVED);
            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(customerSpy));
            when(kycRepository.save(any(Kyc.class))).thenAnswer(inv -> inv.getArgument(0));
            when(customerMapper.toKycResponse(any(Kyc.class))).thenReturn(buildKycResponse(kyc));

            service.submitKyc(customerId, request);

            // verifiedAt must remain exactly the original value
            assertThat(kyc.getVerifiedAt()).isEqualTo(originalTimestamp);
        }

        @Test
        @DisplayName("should NOT set verifiedAt when KYC status is not APPROVED")
        void shouldNotSetVerifiedAtForNonApprovedStatus() {
            UUID customerId = UUID.randomUUID();
            Customer customer = buildCustomer(customerId);
            Kyc kyc = buildKyc(customer, KycStatus.PENDING_REVIEW, null);
            Customer customerSpy = spy(customer);
            when(customerSpy.getKyc()).thenReturn(kyc);

            KycRequest request = buildKycRequest(KycStatus.REJECTED);
            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(customerSpy));
            when(kycRepository.save(any(Kyc.class))).thenAnswer(inv -> inv.getArgument(0));
            when(customerMapper.toKycResponse(any(Kyc.class))).thenReturn(buildKycResponse(kyc));

            service.submitKyc(customerId, request);

            assertThat(kyc.getVerifiedAt()).isNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when customer does not exist")
        void shouldThrowWhenCustomerNotFound() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findActiveById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.submitKyc(id, buildKycRequest(KycStatus.PENDING_REVIEW)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── getKyc ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getKyc()")
    class GetKyc {

        @Test
        @DisplayName("should return the KYC response for an active customer with an existing KYC record")
        void shouldReturnKycForActiveCustomer() {
            UUID customerId = UUID.randomUUID();
            Customer customer = buildCustomer(customerId);
            Kyc kyc = buildKyc(customer, KycStatus.APPROVED, LocalDateTime.now());
            KycResponse response = buildKycResponse(kyc);

            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(customer));
            when(kycRepository.findByCustomerId(customerId)).thenReturn(Optional.of(kyc));
            when(customerMapper.toKycResponse(kyc)).thenReturn(response);

            KycResponse result = service.getKyc(customerId);

            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when the customer has no KYC record")
        void shouldThrowWhenKycNotFound() {
            UUID customerId = UUID.randomUUID();
            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(buildCustomer(customerId)));
            when(kycRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getKyc(customerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("KYC record");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when customer does not exist")
        void shouldThrowWhenCustomerNotFound() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findActiveById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getKyc(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── addAddress ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addAddress()")
    class AddAddress {

        @Test
        @DisplayName("should create address, save it, and return mapped response")
        void shouldAddAddress() {
            UUID customerId = UUID.randomUUID();
            Customer customer = buildCustomer(customerId);
            AddressRequest request = buildAddressRequest();
            Address address = buildAddressEntity(customer);
            AddressResponse response = buildAddressResponse(address);

            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(customer));
            when(customerMapper.toAddressEntity(request, customer)).thenReturn(address);
            when(addressRepository.save(address)).thenReturn(address);
            when(customerMapper.toAddressResponse(address)).thenReturn(response);

            AddressResponse result = service.addAddress(customerId, request);

            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when customer does not exist")
        void shouldThrowWhenCustomerNotFound() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findActiveById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addAddress(id, buildAddressRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── listAddresses ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listAddresses()")
    class ListAddresses {

        @Test
        @DisplayName("should return all addresses for an active customer")
        void shouldReturnAddresses() {
            UUID customerId = UUID.randomUUID();
            Customer customer = buildCustomer(customerId);
            Address address = buildAddressEntity(customer);
            AddressResponse response = buildAddressResponse(address);

            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(customer));
            when(addressRepository.findAllByCustomerId(customerId)).thenReturn(List.of(address));
            when(customerMapper.toAddressResponse(address)).thenReturn(response);

            List<AddressResponse> result = service.listAddresses(customerId);

            assertThat(result).containsExactly(response);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when customer does not exist")
        void shouldThrowWhenCustomerNotFound() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findActiveById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.listAddresses(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── updateAddress ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateAddress()")
    class UpdateAddress {

        @Test
        @DisplayName("should update the address and return the mapped response")
        void shouldUpdateAddress() {
            UUID customerId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            Customer customer = buildCustomer(customerId);
            Address address = buildAddressEntity(customer);
            AddressRequest request = buildAddressRequest();
            AddressResponse response = buildAddressResponse(address);

            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(customer));
            when(addressRepository.findByIdAndCustomerId(addressId, customerId)).thenReturn(Optional.of(address));
            when(addressRepository.save(address)).thenReturn(address);
            when(customerMapper.toAddressResponse(address)).thenReturn(response);

            AddressResponse result = service.updateAddress(customerId, addressId, request);

            assertThat(result).isEqualTo(response);
            verify(customerMapper).updateAddressEntity(address, request);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when address does not belong to customer")
        void shouldThrowWhenAddressNotFound() {
            UUID customerId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(buildCustomer(customerId)));
            when(addressRepository.findByIdAndCustomerId(addressId, customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAddress(customerId, addressId, buildAddressRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Address");
        }
    }

    // ─── removeAddress ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeAddress()")
    class RemoveAddress {

        @Test
        @DisplayName("should delete the address from the repository")
        void shouldRemoveAddress() {
            UUID customerId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            Customer customer = buildCustomer(customerId);
            Address address = buildAddressEntity(customer);

            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(customer));
            when(addressRepository.findByIdAndCustomerId(addressId, customerId)).thenReturn(Optional.of(address));

            service.removeAddress(customerId, addressId);

            verify(addressRepository).delete(address);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when address does not exist")
        void shouldThrowWhenAddressNotFound() {
            UUID customerId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(buildCustomer(customerId)));
            when(addressRepository.findByIdAndCustomerId(addressId, customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeAddress(customerId, addressId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── addContact ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addContact()")
    class AddContact {

        @Test
        @DisplayName("should create a contact with verified=false, save it, and return the mapped response")
        void shouldAddContactWithVerifiedFalse() {
            UUID customerId = UUID.randomUUID();
            Customer customer = buildCustomer(customerId);
            ContactRequest request = buildContactRequest();
            ContactDetail contact = buildContactEntity(customer, false);
            ContactResponse response = buildContactResponse(contact);

            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(customer));
            when(customerMapper.toContactEntity(request, customer)).thenReturn(contact);
            when(contactDetailRepository.save(contact)).thenReturn(contact);
            when(customerMapper.toContactResponse(contact)).thenReturn(response);

            ContactResponse result = service.addContact(customerId, request);

            assertThat(result).isEqualTo(response);
            assertThat(contact.isVerified()).isFalse();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when customer does not exist")
        void shouldThrowWhenCustomerNotFound() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findActiveById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addContact(id, buildContactRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── listContacts ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listContacts()")
    class ListContacts {

        @Test
        @DisplayName("should return all contact details for an active customer")
        void shouldReturnContacts() {
            UUID customerId = UUID.randomUUID();
            Customer customer = buildCustomer(customerId);
            ContactDetail contact = buildContactEntity(customer, false);
            ContactResponse response = buildContactResponse(contact);

            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(customer));
            when(contactDetailRepository.findAllByCustomerId(customerId)).thenReturn(List.of(contact));
            when(customerMapper.toContactResponse(contact)).thenReturn(response);

            List<ContactResponse> result = service.listContacts(customerId);

            assertThat(result).containsExactly(response);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when customer does not exist")
        void shouldThrowWhenCustomerNotFound() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findActiveById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.listContacts(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── updateContact ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateContact()")
    class UpdateContact {

        @Test
        @DisplayName("should update the contact and not change the verified flag")
        void shouldUpdateContactWithoutChangingVerified() {
            UUID customerId = UUID.randomUUID();
            UUID contactId = UUID.randomUUID();
            Customer customer = buildCustomer(customerId);
            ContactDetail contact = buildContactEntity(customer, true); // pre-verified
            contact.setId(contactId);
            ContactRequest request = buildContactRequest();
            ContactResponse response = buildContactResponse(contact);

            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(customer));
            when(contactDetailRepository.findByIdAndCustomerId(contactId, customerId)).thenReturn(Optional.of(contact));
            when(contactDetailRepository.save(contact)).thenReturn(contact);
            when(customerMapper.toContactResponse(contact)).thenReturn(response);

            service.updateContact(customerId, contactId, request);

            verify(customerMapper).updateContactEntity(contact, request);
            // verified remains true — the mapper never sets it
            assertThat(contact.isVerified()).isTrue();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when contact does not belong to customer")
        void shouldThrowWhenContactNotFound() {
            UUID customerId = UUID.randomUUID();
            UUID contactId = UUID.randomUUID();
            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(buildCustomer(customerId)));
            when(contactDetailRepository.findByIdAndCustomerId(contactId, customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateContact(customerId, contactId, buildContactRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Contact");
        }
    }

    // ─── removeContact ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeContact()")
    class RemoveContact {

        @Test
        @DisplayName("should delete the contact from the repository")
        void shouldRemoveContact() {
            UUID customerId = UUID.randomUUID();
            UUID contactId = UUID.randomUUID();
            Customer customer = buildCustomer(customerId);
            ContactDetail contact = buildContactEntity(customer, false);

            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(customer));
            when(contactDetailRepository.findByIdAndCustomerId(contactId, customerId)).thenReturn(Optional.of(contact));

            service.removeContact(customerId, contactId);

            verify(contactDetailRepository).delete(contact);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when contact does not exist")
        void shouldThrowWhenContactNotFound() {
            UUID customerId = UUID.randomUUID();
            UUID contactId = UUID.randomUUID();
            when(customerRepository.findActiveById(customerId)).thenReturn(Optional.of(buildCustomer(customerId)));
            when(contactDetailRepository.findByIdAndCustomerId(contactId, customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeContact(customerId, contactId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Customer buildCustomer(UUID id) {
        return Customer.builder()
                .id(id)
                .firstName("Jane")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 6, 15))
                .nationality("British")
                .status(CustomerStatus.PENDING)
                .build();
    }

    private CustomerRequest buildCustomerRequest() {
        CustomerRequest req = new CustomerRequest();
        req.setFirstName("Jane");
        req.setLastName("Doe");
        req.setDateOfBirth(LocalDate.of(1990, 6, 15));
        req.setNationality("British");
        return req;
    }

    private CustomerResponse buildCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .status(CustomerStatus.PENDING)
                .build();
    }

    private CustomerSummaryResponse buildSummaryResponse(Customer customer) {
        return CustomerSummaryResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .status(CustomerStatus.PENDING)
                .build();
    }

    private KycRequest buildKycRequest(KycStatus status) {
        KycRequest req = new KycRequest();
        req.setDocumentType("PASSPORT");
        req.setDocumentNumber("AB001234");
        req.setStatus(status);
        req.setVerifiedBy("officer@company.com");
        return req;
    }

    private Kyc buildKyc(Customer customer, KycStatus status, LocalDateTime verifiedAt) {
        return Kyc.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .documentType("PASSPORT")
                .documentNumber("AB001234")
                .status(status)
                .verifiedAt(verifiedAt)
                .build();
    }

    private KycResponse buildKycResponse(Kyc kyc) {
        return KycResponse.builder()
                .id(kyc.getId())
                .customerId(kyc.getCustomer().getId())
                .documentType(kyc.getDocumentType())
                .documentNumber("****1234")
                .status(kyc.getStatus())
                .build();
    }

    private AddressRequest buildAddressRequest() {
        AddressRequest req = new AddressRequest();
        req.setType(AddressType.HOME);
        req.setStreet("10 Downing Street");
        req.setCity("London");
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
                .postalCode("SW1A 2AA")
                .country("GB")
                .primary(true)
                .build();
    }

    private AddressResponse buildAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .customerId(address.getCustomer().getId())
                .type(AddressType.HOME)
                .street("10 Downing Street")
                .city("London")
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

    private ContactDetail buildContactEntity(Customer customer, boolean verified) {
        return ContactDetail.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .type(ContactType.EMAIL)
                .value("jane.doe@example.com")
                .primary(true)
                .verified(verified)
                .build();
    }

    private ContactResponse buildContactResponse(ContactDetail contact) {
        return ContactResponse.builder()
                .id(contact.getId())
                .customerId(contact.getCustomer().getId())
                .type(ContactType.EMAIL)
                .value("jane.doe@example.com")
                .primary(true)
                .verified(contact.isVerified())
                .build();
    }
}
