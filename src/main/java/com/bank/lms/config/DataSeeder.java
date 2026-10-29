package com.bank.lms.config;

import com.bank.lms.entity.User;
import com.bank.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("officer1")) {
            userRepository.save(User.builder()
                    .username("officer1")
                    .password(passwordEncoder.encode("officer123"))
                    .role("ROLE_OFFICER")
                    .fullName("Vikram Malhotra (Senior Credit Manager)")
                    .email("v.malhotra@bank.internal")
                    .phone("+91 98201 11223")
                    .build());
        }

        if (!userRepository.existsByUsername("customer1")) {
            userRepository.save(User.builder()
                    .username("customer1")
                    .password(passwordEncoder.encode("customer123"))
                    .role("ROLE_CUSTOMER")
                    .customerId(101L)
                    .fullName("Aarav Sharma")
                    .email("aarav.sharma@example.com")
                    .phone("+91 98765 43210")
                    .address("Flat 402, Lotus Residency, Mumbai, MH")
                    .assignedOfficerName("Vikram Malhotra")
                    .build());
        }

        if (!userRepository.existsByUsername("customer2")) {
            userRepository.save(User.builder()
                    .username("customer2")
                    .password(passwordEncoder.encode("customer123"))
                    .role("ROLE_CUSTOMER")
                    .customerId(102L)
                    .fullName("Pooja Patel")
                    .email("pooja.patel@example.com")
                    .phone("+91 98123 77889")
                    .address("Plot 14, Sector 19, CBD Belapur, Navi Mumbai")
                    .assignedOfficerName("Vikram Malhotra")
                    .build());
        }
    }
}