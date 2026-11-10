package com.casaverde.backend.capaAccesoADatos.repositories;

import java.math.BigDecimal;
import java.util.List;
import com.casaverde.backend.capaAccesoADatos.models.entitys.AbonoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AbonoCompraRepository extends JpaRepository<AbonoCompra, Long> {
    List<AbonoCompra> findByCompraIdOrderByCreadoEnAsc(Long compraId);
    List<AbonoCompra> findByCompraIdOrderByCreadoEnDesc(Long compraId);
    List<AbonoCompra> findByCompraIdIn(List<Long> compraIds);

    @Query("SELECT COALESCE(SUM(a.montoCop), 0) FROM AbonoCompra a WHERE a.compra.id = :compraId")
    BigDecimal sumMontoCopByCompraId(@Param("compraId") Long compraId);
}
