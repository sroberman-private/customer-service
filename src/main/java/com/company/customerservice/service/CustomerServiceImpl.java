package com.company.customerservice.service;

import com.company.customerservice.domain.Address;
import com.company.customerservice.domain.ContactDetail;
import com.company.customerservice.domain.Customer;
import com.company.customerservice.domain.Kyc;
import com.company.customerservice.domain.enums.KycStatus;
import com.company.customerservice.exception.ResourceNotFoundException;
import com.company.customerservice.repository.AddressRepository;
import com.company.customerservice.repository.ContactDetailRepository;
import com.company.customerservice.repository.CustomerRepository;
import com.company.customerservice.repository.KycRepository;
import com.company.customerservice.web.dto.*;
import com.company.customerservice.web.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl {

    private final CustomerRepository customerRepository;
    private final KycRepository kycRepository;
    private final AddressRepository addressRepository;
    private final ContactDetailRepository contactDetailRepository;
    private final CustomerMapper customerMapper;

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        Customer customer = customerMapper.toEntity(request);
        Customer saved = customerRepository.save(customer);
        return customerMapper.toResponse(saved);
    }

    public CustomerResponse getCustomerById(UUID customerId) {
        Customer customer = findActiveCustomer(customerId);
        return customerMapper.toResponse(customer);
    }

    public Page<CustomerSummaryResponse> listCustomers(Pageable pageable) {
        return customerRepository.findAllActive(pageable)
                .map(customerMapper::toSummaryResponse);
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID customerId, CustomerRequest request) {
        Customer customer = findActiveCustomer(customerId);
        customerMapper.updateEntity(customer, request);
        Customer saved = customerRepository.save(customer);
        return customerMapper.toResponse(saved);
    }

    @Transactional
    public void deleteCustomer(UUID customerId) {
        Customer customer = findActiveCustomer(customerId);
        customer.softDelete();
        customerRepository.save(customer);
    }

    @Transactional
    public KycResponse submitKyc(UUID customerId, KycRequest request) {
        Customer customer = findActiveCustomer(customerId);
        Kyc kyc = customer.getKyc();

        if (kyc == null) {
            kyc = customerMapper.toKycEntity(request, customer);
        } else {
            customerMapper.updateKycEntity(kyc, request);
        }

        if (isKycBeingApproved(request) && kyc.getVerifiedAt() == null) {
            kyc.setVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
        }

        Kyc saved = kycRepository.save(kyc);
        return customerMapper.toKycResponse(saved);
    }

    public KycResponse getKyc(UUID customerId) {
        findActiveCustomer(customerId);
        Kyc kyc = kycRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC record", customerId));
        return customerMapper.toKycResponse(kyc);
    }

    @Transactional
    public AddressResponse addAddress(UUID customerId, AddressRequest request) {
        Customer customer = findActiveCustomer(customerId);
        Address address = customerMapper.toAddressEntity(request, customer);
        Address saved = addressRepository.save(address);
        return customerMapper.toAddressResponse(saved);
    }

    public List<AddressResponse> listAddresses(UUID customerId) {
        findActiveCustomer(customerId);
        return addressRepository.findAllByCustomerId(customerId).stream()
                .map(customerMapper::toAddressResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse updateAddress(UUID customerId, UUID addressId, AddressRequest request) {
        findActiveCustomer(customerId);
        Address address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        customerMapper.updateAddressEntity(address, request);
        Address saved = addressRepository.save(address);
        return customerMapper.toAddressResponse(saved);
    }

    @Transactional
    public void removeAddress(UUID customerId, UUID addressId) {
        findActiveCustomer(customerId);
        Address address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        addressRepository.delete(address);
    }

    @Transactional
    public ContactResponse addContact(UUID customerId, ContactRequest request) {
        Customer customer = findActiveCustomer(customerId);
        ContactDetail contact = customerMapper.toContactEntity(request, customer);
        ContactDetail saved = contactDetailRepository.save(contact);
        return customerMapper.toContactResponse(saved);
    }

    public List<ContactResponse> listContacts(UUID customerId) {
        findActiveCustomer(customerId);
        return contactDetailRepository.findAllByCustomerId(customerId).stream()
                .map(customerMapper::toContactResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContactResponse updateContact(UUID customerId, UUID contactId, ContactRequest request) {
        findActiveCustomer(customerId);
        ContactDetail contact = contactDetailRepository.findByIdAndCustomerId(contactId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", contactId));
        customerMapper.updateContactEntity(contact, request);
        ContactDetail saved = contactDetailRepository.save(contact);
        return customerMapper.toContactResponse(saved);
    }

    @Transactional
    public void removeContact(UUID customerId, UUID contactId) {
        findActiveCustomer(customerId);
        ContactDetail contact = contactDetailRepository.findByIdAndCustomerId(contactId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", contactId));
        contactDetailRepository.delete(contact);
    }

    private Customer findActiveCustomer(UUID customerId) {
        return customerRepository.findActiveById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    private boolean isKycBeingApproved(KycRequest request) {
        return KycStatus.APPROVED.equals(request.getStatus());
    }
}
