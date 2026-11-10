package com.casaverde.backend.fachadaServices.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaProductoDTO {
    private Long id;
    private String nombre;
    private Integer orden;
    private Boolean activo;
}
