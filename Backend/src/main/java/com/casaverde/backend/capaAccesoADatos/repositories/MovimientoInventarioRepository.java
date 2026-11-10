package com.casaverde.backend.capaAccesoADatos.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.MovimientoInventario;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
        List<MovimientoInventario> findByInsumoOrderByRegistradoEnDesc(String insumo);

        Page<MovimientoInventario> findByInsumoOrderByRegistradoEnDesc(String insumo, Pageable pageable);

        List<MovimientoInventario> findAllByOrderByRegistradoEnDesc();

        Page<MovimientoInventario> findAllByOrderByRegistradoEnDesc(Pageable pageable);

        List<MovimientoInventario> findByRegistradoEnBetweenOrderByRegistradoEnDesc(
                        LocalDateTime desde, LocalDateTime hasta);

        List<MovimientoInventario> findByInsumoAndRegistradoEnBetweenOrderByRegistradoEnDesc(
                        String insumo, LocalDateTime desde, LocalDateTime hasta);

        List<MovimientoInventario> findByTipoAndReferenciaTipoAndRegistradoEnBetween(
                        String tipo, String referenciaTipo, LocalDateTime inicio, LocalDateTime fin);

        List<MovimientoInventario> findByReferenciaTipoAndReferenciaId(String referenciaTipo, Long referenciaId);
}
