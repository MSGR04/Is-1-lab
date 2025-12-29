package com.msgr.tickets.domain.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class Location implements Serializable {

    @NotNull
    @Column(nullable = false)
    private Float x;

    @Column(nullable = false)
    private float y;

    @NotNull
    @Column(nullable = false)
    private Float z;
}
