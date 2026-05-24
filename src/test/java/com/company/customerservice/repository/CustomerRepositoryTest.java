package com.company.customerservice.repository;

import com.company.customerservice.domain.Customer;
import com.company.customerservice.domain.enums.CustomerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CustomerRepository")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    // ─── findActiveById ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("findActiveById()")
    class FindActiveById {

        @Test
        @DisplayName("should return the customer when it exists and is not soft-deleted")
        void shouldReturnActiveCustomer() {
            Customer saved = customerRepository.save(buildCustomer());

            Optional<Customer> result = customerRepository.findActiveById(saved.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(saved.getId());
            assertThat(result.get().getFirstName()).isEqualTo("Jane");
        }

        @Test
        @DisplayName("should return empty when the customer is soft-deleted (deletedAt is set)")
        void shouldReturnEmptyForSoftDeletedCustomer() {
            Customer customer = buildCustomer();
            customer.softDelete();
            Customer saved = customerRepository.save(customer);

            Optional<Customer> result = customerRepository.findActiveById(saved.getId());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when no customer exists with the given id")
        void shouldReturnEmptyForUnknownId() {
            Optional<Customer> result = customerRepository.findActiveById(UUID.randomUUID());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return the customer when deletedAt is null even if other customers are soft-deleted")
        void shouldNotConfuseActiveAndDeletedCustomers() {
            Customer active = customerRepository.save(buildCustomer("Alice", "Smith"));

            Customer deleted = buildCustomer("Bob", "Jones");
            deleted.softDelete();
            customerRepository.save(deleted);

            Optional<Customer> result = customerRepository.findActiveById(active.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getFirstName()).isEqualTo("Alice");
        }
    }

    // ─── findAllActive ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAllActive()")
    class FindAllActive {

        @Test
        @DisplayName("should return only active (non-deleted) customers")
        void shouldExcludeSoftDeletedCustomers() {
            customerRepository.save(buildCustomer("Active", "One"));
            customerRepository.save(buildCustomer("Active", "Two"));

            Customer deleted = buildCustomer("Deleted", "User");
            deleted.softDelete();
            customerRepository.save(deleted);

            Pageable pageable = PageRequest.of(0, 20);
            Page<Customer> result = customerRepository.findAllActive(pageable);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent())
                    .extracting(Customer::getFirstName)
                    .doesNotContain("Deleted");
        }

        @Test
        @DisplayName("should return an empty page when all customers are soft-deleted")
        void shouldReturnEmptyPageWhenAllDeleted() {
            Customer c1 = buildCustomer("Gone", "A");
            c1.softDelete();
            Customer c2 = buildCustomer("Gone", "B");
            c2.softDelete();
            customerRepository.save(c1);
            customerRepository.save(c2);

            Pageable pageable = PageRequest.of(0, 20);
            Page<Customer> result = customerRepository.findAllActive(pageable);

            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return an empty page when no customers exist at all")
        void shouldReturnEmptyPageWhenNoneExist() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Customer> result = customerRepository.findAllActive(pageable);

            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("should respect page size and return only the requested number of records")
        void shouldRespectPageSize() {
            for (int i = 0; i < 5; i++) {
                customerRepository.save(buildCustomer("Customer" + i, "Last" + i));
            }

            Pageable pageable = PageRequest.of(0, 3);
            Page<Customer> result = customerRepository.findAllActive(pageable);

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return the second page correctly when paginating")
        void shouldReturnCorrectSecondPage() {
            for (int i = 0; i < 5; i++) {
                customerRepository.save(buildCustomer("Customer" + i, "Last" + i));
            }

            Pageable pageable = PageRequest.of(1, 3);
            Page<Customer> result = customerRepository.findAllActive(pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("should mix active and deleted customers and return only active ones in a page")
        void shouldFilterDeletedAcrossLargerDataset() {
            // 3 active, 2 deleted
            customerRepository.save(buildCustomer("Active", "A"));
            customerRepository.save(buildCustomer("Active", "B"));
            customerRepository.save(buildCustomer("Active", "C"));

            Customer d1 = buildCustomer("Deleted", "D");
            d1.softDelete();
            Customer d2 = buildCustomer("Deleted", "E");
            d2.softDelete();
            customerRepository.save(d1);
            customerRepository.save(d2);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Customer> result = customerRepository.findAllActive(pageable);

            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getContent())
                    .allMatch(c -> c.getDeletedAt() == null);
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Customer buildCustomer() {
        return buildCustomer("Jane", "Doe");
    }

    private Customer buildCustomer(String firstName, String lastName) {
        return Customer.builder()
                .firstName(firstName)
                .lastName(lastName)
                .dateOfBirth(LocalDate.of(1990, 6, 15))
                .nationality("British")
                .status(CustomerStatus.PENDING)
                .build();
    }
}
