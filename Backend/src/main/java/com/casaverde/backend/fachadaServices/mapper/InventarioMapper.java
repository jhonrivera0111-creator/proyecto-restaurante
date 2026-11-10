package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Inventario;
import com.casaverde.backend.capaAccesoADatos.models.entitys.MovimientoInventario;
import com.casaverde.backend.fachadaServices.dto.InventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoInventarioDTO;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventarioMapper {
    InventarioDTO toDTO(Inventario i);

    MovimientoInventarioDTO toMovimientoDTO(MovimientoInventario m);
}
