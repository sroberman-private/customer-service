package com.company.customerservice.web;

import com.company.customerservice.service.CustomerServiceImpl;
import com.company.customerservice.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerServiceImpl customerService;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CustomerSummaryResponse>> listCustomers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(customerService.listCustomers(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    // ─── KYC ─────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/kyc")
    public ResponseEntity<KycResponse> submitKyc(
            @PathVariable UUID id,
            @Valid @RequestBody KycRequest request) {
        KycResponse response = customerService.submitKyc(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/kyc")
    public ResponseEntity<KycResponse> getKyc(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.getKyc(id));
    }

    // ─── Addresses ───────────────────────────────────────────────────────────

    @PostMapping("/{id}/addresses")
    public ResponseEntity<AddressResponse> addAddress(
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = customerService.addAddress(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/addresses")
    public ResponseEntity<List<AddressResponse>> listAddresses(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.listAddresses(id));
    }

    @PutMapping("/{id}/addresses/{addrId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable UUID id,
            @PathVariable UUID addrId,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(customerService.updateAddress(id, addrId, request));
    }

    @DeleteMapping("/{id}/addresses/{addrId}")
    public ResponseEntity<Void> removeAddress(
            @PathVariable UUID id,
            @PathVariable UUID addrId) {
        customerService.removeAddress(id, addrId);
        return ResponseEntity.noContent().build();
    }

    // ─── Contact Details ─────────────────────────────────────────────────────

    @PostMapping("/{id}/contacts")
    public ResponseEntity<ContactResponse> addContact(
            @PathVariable UUID id,
            @Valid @RequestBody ContactRequest request) {
        ContactResponse response = customerService.addContact(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/contacts")
    public ResponseEntity<List<ContactResponse>> listContacts(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.listContacts(id));
    }

    @PutMapping("/{id}/contacts/{contactId}")
    public ResponseEntity<ContactResponse> updateContact(
            @PathVariable UUID id,
            @PathVariable UUID contactId,
            @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(customerService.updateContact(id, contactId, request));
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    public ResponseEntity<Void> removeContact(
            @PathVariable UUID id,
            @PathVariable UUID contactId) {
        customerService.removeContact(id, contactId);
        return ResponseEntity.noContent().build();
    }
}
