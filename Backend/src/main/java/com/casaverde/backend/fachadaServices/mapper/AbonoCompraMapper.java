package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.AbonoCompra;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AbonoCompraMapper {

    @Mapping(source = "compra.id", target = "compraId")
    @Mapping(source = "adelantoProveedor.id", target = "adelantoProveedorId")
    AbonoCompraDTO toDTO(AbonoCompra abono);
}
