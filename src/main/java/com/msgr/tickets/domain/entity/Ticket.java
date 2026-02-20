package com.msgr.tickets.domain.entity;

import com.msgr.tickets.domain.embeddable.Coordinates;
import com.msgr.tickets.domain.enums.TicketType;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Valid
    @Embedded
    private Coordinates coordinates;

    @NotNull
    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "person_id")
    private Person person;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Positive
    @Column(nullable = false)
    private double price;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketType type;

    @Min(1)
    @Max(100)
    @Column(nullable = false)
    private long discount;


    @Column(nullable = false)
    @Positive
    private long number;

    @NotNull
    @Column(nullable = false, columnDefinition = "text")
    private String comment;

    @Column(nullable = false)
    private boolean refundable;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @PrePersist
    public void prePersist() {
        if (creationDate == null) {
            creationDate = LocalDate.now();
        }
    }
}
