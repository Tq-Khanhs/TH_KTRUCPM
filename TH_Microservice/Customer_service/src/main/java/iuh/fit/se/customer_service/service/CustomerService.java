package iuh.fit.se.customer_service.service;

import com.example.customerservice.model.Customer;
import com.example.customerservice.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + id));
    }

    public Customer createCustomer(Customer customer) {
        customerRepository.findByEmail(customer.getEmail())
                .ifPresent(c -> {
                    throw new IllegalStateException("Email already in use: " + customer.getEmail());
                });
        
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Long id, Customer customerDetails) {
        Customer customer = getCustomerById(id);
        
        // Check if email is being changed and if it's already in use
        if (!customer.getEmail().equals(customerDetails.getEmail())) {
            customerRepository.findByEmail(customerDetails.getEmail())
                    .ifPresent(c -> {
                        throw new IllegalStateException("Email already in use: " + customerDetails.getEmail());
                    });
        }
        
        customer.setName(customerDetails.getName());
        customer.setEmail(customerDetails.getEmail());
        customer.setPhone(customerDetails.getPhone());
        customer.setAddress(customerDetails.getAddress());
        
        return customerRepository.save(customer);
    }

    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        customerRepository.delete(customer);
    }
}
