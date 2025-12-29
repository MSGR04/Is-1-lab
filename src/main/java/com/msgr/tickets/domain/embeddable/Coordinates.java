package com.msgr.tickets.domain.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class Coordinates implements Serializable {

    @Max(265)
    @Column(name = "coordinates_x")
    private int x; // max 265

    @Column(name = "coordinates_y")
    private long y;
}
