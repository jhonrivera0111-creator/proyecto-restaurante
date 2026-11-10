package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Compra;
import com.casaverde.backend.fachadaServices.dto.CompraDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompraMapper {

    @Mapping(source = "proveedor.id", target = "proveedorId")
    @Mapping(target = "totalAbonado", ignore = true)
    @Mapping(target = "saldoPendiente", ignore = true)
    @Mapping(target = "abonos", ignore = true)
    CompraDTO toDTO(Compra compra);
}
