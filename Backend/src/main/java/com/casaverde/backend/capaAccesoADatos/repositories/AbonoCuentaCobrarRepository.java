package com.casaverde.backend.capaAccesoADatos.repositories;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.casaverde.backend.capaAccesoADatos.models.entitys.AbonoCuentaCobrar;

public interface AbonoCuentaCobrarRepository extends JpaRepository<AbonoCuentaCobrar, Long> {
    List<AbonoCuentaCobrar> findByCuentaIdOrderByCreadoEnDesc(Long cuentaId);
    List<AbonoCuentaCobrar> findByCuentaIdIn(List<Long> cuentaIds);

    @Query("SELECT COALESCE(SUM(a.montoCop), 0) FROM AbonoCuentaCobrar a WHERE a.cuenta.id = :cuentaId")
    BigDecimal sumMontoCopByCuentaId(@Param("cuentaId") Long cuentaId);
}
