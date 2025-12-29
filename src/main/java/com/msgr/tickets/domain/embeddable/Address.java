package com.msgr.tickets.domain.embeddable;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class Address implements Serializable {

    @Size(min = 7)
    @Column(name = "address_zip_code")
    private String zipCode; // может быть null

    @Valid
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "x", column = @Column(name = "town_x", nullable = true)),
            @AttributeOverride(name = "y", column = @Column(name = "town_y", nullable = true)),
            @AttributeOverride(name = "z", column = @Column(name = "town_z", nullable = true))
    })
    private Location town; // может быть null
}
