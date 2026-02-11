package com.msgr.tickets.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @NotBlank
    @Column(name = "password_hash", nullable = false, length = 512)
    private String passwordHash;

    @NotBlank
    @Column(nullable = false, length = 32)
    private String role = "USER";
}
