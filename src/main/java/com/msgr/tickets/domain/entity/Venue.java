package com.msgr.tickets.domain.entity;

import com.msgr.tickets.domain.embeddable.Address;
import com.msgr.tickets.domain.enums.VenueType;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Positive
    @Column(nullable = false)
    private int capacity;

    @NotNull
    @Enumerated(EnumType.STRING)
    private VenueType type;

    @NotNull
    @Valid
    @Embedded
    private Address address;
}
