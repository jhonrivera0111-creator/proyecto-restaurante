package com.casaverde.backend.capaAccesoADatos.models.entitys;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "salon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    private Integer aforo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean eliminado = false;
}
