package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cierre_caja", uniqueConstraints = @UniqueConstraint(columnNames = "fecha"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CierreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nombre", length = 150)
    private String usuarioNombre;

    @Column(name = "esperado_efectivo", nullable = false, precision = 19, scale = 0)
    private BigDecimal esperadoEfectivo;

    @Column(name = "esperado_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal esperadoUsd;

    @Column(name = "esperado_nequi", nullable = false, precision = 19, scale = 0)
    private BigDecimal esperadoNequi;

    @Column(name = "contado_efectivo", nullable = false, precision = 19, scale = 0)
    private BigDecimal contadoEfectivo;

    @Column(name = "contado_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal contadoUsd;

    @Column(name = "contado_nequi", nullable = false, precision = 19, scale = 0)
    private BigDecimal contadoNequi;

    @Column(name = "diferencia_efectivo", nullable = false, precision = 19, scale = 0)
    private BigDecimal diferenciaEfectivo;

    @Column(name = "diferencia_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal diferenciaUsd;

    @Column(name = "diferencia_nequi", nullable = false, precision = 19, scale = 0)
    private BigDecimal diferenciaNequi;

    @Column(name = "cobrado_total_cop", nullable = false, precision = 19, scale = 0)
    private BigDecimal cobradoTotalCop;

    @Column(name = "gastos_total_cop", nullable = false, precision = 19, scale = 0)
    private BigDecimal gastosTotalCop;

    @Column(name = "balance_neto", nullable = false, precision = 19, scale = 0)
    private BigDecimal balanceNeto;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "backup_path", length = 500)
    private String backupPath;

    @Column(name = "cerrado_en", nullable = false)
    private LocalDateTime cerradoEn;

    // ── Denominaciones arqueo Efectivo ───────────────────────────────────────
    @Column(name = "base_efectivo", precision = 19, scale = 0)
    private BigDecimal baseEfectivo;

    @Column(name = "billetes_100k")
    private Integer billetes100k;

    @Column(name = "billetes_50k")
    private Integer billetes50k;

    @Column(name = "billetes_20k")
    private Integer billetes20k;

    @Column(name = "billetes_10k")
    private Integer billetes10k;

    @Column(name = "billetes_5k")
    private Integer billetes5k;

    @Column(name = "billetes_2k")
    private Integer billetes2k;

    @Column(name = "monedas_efectivo", precision = 19, scale = 0)
    private BigDecimal monedasEfectivo;

    @PrePersist
    void onCreate() {
        cerradoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
