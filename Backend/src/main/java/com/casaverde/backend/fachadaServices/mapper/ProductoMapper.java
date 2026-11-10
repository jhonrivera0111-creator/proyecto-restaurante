package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Producto;
import com.casaverde.backend.capaAccesoADatos.models.entitys.ProductoComposicionInventario;
import com.casaverde.backend.capaAccesoADatos.models.entitys.ProductoPrecioHistorial;
import com.casaverde.backend.fachadaServices.dto.ComposicionDTO;
import com.casaverde.backend.fachadaServices.dto.ProductoDTO;
import com.casaverde.backend.fachadaServices.dto.ProductoPrecioHistorialDTO;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @Mapping(target = "categoriaId", source = "categoria.id")
    @Mapping(target = "categoriaNombre", source = "categoria.nombre")
    ProductoDTO toDTO(Producto p);

    ComposicionDTO toComposicionDTO(ProductoComposicionInventario c);

    @Mapping(target = "productoId", source = "producto.id")
    ProductoPrecioHistorialDTO toPrecioHistorialDTO(ProductoPrecioHistorial h);
}
