package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CategoriaProducto;
import com.casaverde.backend.fachadaServices.dto.CategoriaProductoDTO;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoriaProductoMapper {
    CategoriaProductoDTO toDTO(CategoriaProducto e);
}
