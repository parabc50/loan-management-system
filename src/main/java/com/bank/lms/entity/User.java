package com.bank.lms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // "ROLE_CUSTOMER" or "ROLE_OFFICER"

    private Long customerId; // CIF number

    // Personal & Contact Info
    private String fullName;
    private String email;
    private String phone;
    private String address;

    // Assigned Banking Officer
    private String assignedOfficerName;
}