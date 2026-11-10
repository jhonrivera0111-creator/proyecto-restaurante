package com.casaverde.backend.fachadaServices.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.casaverde.backend.capaAccesoADatos.models.entitys.AdelantoProveedor;
import com.casaverde.backend.fachadaServices.dto.AdelantoProveedorDTO;

@Mapper(componentModel = "spring")
public interface AdelantoProveedorMapper {

    @Mapping(source = "proveedor.id", target = "proveedorId")
    @Mapping(target = "saldoUsado", ignore = true)
    AdelantoProveedorDTO toDTO(AdelantoProveedor a);
}
