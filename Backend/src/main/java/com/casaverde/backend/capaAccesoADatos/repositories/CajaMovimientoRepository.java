package com.casaverde.backend.capaAccesoADatos.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CajaMovimiento;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;

import java.math.BigDecimal;

public interface CajaMovimientoRepository extends JpaRepository<CajaMovimiento, Long> {
       List<CajaMovimiento> findAllByOrderByRegistradoEnDesc();

       Page<CajaMovimiento> findAllByOrderByRegistradoEnDesc(Pageable pageable);

       List<CajaMovimiento> findByRegistradoEnBetweenOrderByRegistradoEnDesc(LocalDateTime inicio, LocalDateTime fin);

       Page<CajaMovimiento> findByRegistradoEnBetweenOrderByRegistradoEnDesc(
                     LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

       boolean existsByReferenciaTipoAndReferenciaIdAndTipo(String referenciaTipo, Long referenciaId, String tipo);

       List<CajaMovimiento> findByReferenciaTipoAndReferenciaId(String referenciaTipo, Long referenciaId);

       /**
        * Historial paginado con filtros opcionales — todos los parámetros son nullable.
        * Cuando un parámetro es null, ese filtro no se aplica.
        */
       @Query("SELECT c FROM CajaMovimiento c WHERE " +
                     "(:moneda IS NULL OR c.moneda = :moneda) AND " +
                     "(:tipo IS NULL OR c.tipo = :tipo) AND " +
                     "(:inicio IS NULL OR c.registradoEn >= :inicio) AND " +
                     "(:fin IS NULL OR c.registradoEn < :fin) " +
                     "ORDER BY c.registradoEn DESC")
       Page<CajaMovimiento> findConFiltros(
                     @Param("moneda") String moneda,
                     @Param("tipo") CajaMovimientoTipo tipo,
                     @Param("inicio") LocalDateTime inicio,
                     @Param("fin") LocalDateTime fin,
                     Pageable pageable);

       @Query("SELECT c FROM CajaMovimiento c WHERE " +
                     "(:moneda IS NULL OR c.moneda = :moneda) AND " +
                     "(:tipo IS NULL OR c.tipo = :tipo) AND " +
                     "(:inicio IS NULL OR c.registradoEn >= :inicio) AND " +
                     "(:fin IS NULL OR c.registradoEn < :fin) " +
                     "ORDER BY c.registradoEn DESC")
       List<CajaMovimiento> findConFiltrosSinPaginar(
                     @Param("moneda") String moneda,
                     @Param("tipo") CajaMovimientoTipo tipo,
                     @Param("inicio") LocalDateTime inicio,
                     @Param("fin") LocalDateTime fin);

       /**
        * Suma el valor absoluto de todas las salidas (monto &lt; 0) de un fondo en el
        * rango dado.
        */
       @Query("SELECT COALESCE(SUM(ABS(c.monto)), 0) FROM CajaMovimiento c " +
                     "WHERE c.moneda = :fondoMoneda AND c.monto < 0 " +
                     "AND c.registradoEn >= :inicio AND c.registradoEn < :fin")
       BigDecimal sumSalidasFondoEnMes(@Param("fondoMoneda") String fondoMoneda,
                     @Param("inicio") LocalDateTime inicio,
                     @Param("fin") LocalDateTime fin);
}
