package com.msgr.tickets.domain.entity;
import com.msgr.tickets.domain.embeddable.Location;
import com.msgr.tickets.domain.enums.Color;
import com.msgr.tickets.domain.enums.Country;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "persons")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Color eyeColor;

    @Enumerated(EnumType.STRING)
    private Color hairColor;

    @NotNull
    @Valid
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "x", column = @Column(name = "location_x", nullable = false)),
            @AttributeOverride(name = "y", column = @Column(name = "location_y", nullable = false)),
            @AttributeOverride(name = "z", column = @Column(name = "location_z", nullable = false))
    })
    private Location location;

    @NotNull
    @Size(max = 35)
    @Column(nullable = false, length = 35)
    private String passportID;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Country nationality;
}
