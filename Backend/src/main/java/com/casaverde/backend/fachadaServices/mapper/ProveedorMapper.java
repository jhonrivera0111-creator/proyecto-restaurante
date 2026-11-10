package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor;
import com.casaverde.backend.fachadaServices.dto.ProveedorDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProveedorMapper {
    ProveedorDTO toDTO(Proveedor proveedor);
}
