# Tier 4 — Módulo de Empleados Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar el módulo completo de Empleados: alta/baja de empleados (QUINCENAL y POR_DIA), registro de adelantos (debita fondo inmediatamente), registro de días trabajados, liquidación de quincena/cobro de días, y registro informativo de vacaciones.

**Architecture:** Backend Spring Boot con 5 entidades JPA, un servicio central `IEmpleadoService`, y un único mapper `EmpleadoMapper`. Frontend con una página dedicada `/empleados` y tres modales: `ModalEmpleado` (crear/editar), `ModalDetalleEmpleado` (4 sub-tabs: Adelantos, Días, Pagos, Vacaciones), y `ModalLiquidar` (preview + confirmación de pago).

**Tech Stack:** Java 17 + Spring Boot 3.4.7 + MapStruct + Lombok + Spring Data JPA + SQLite / React 19 + TanStack Query v5 + Axios + lucide-react + Tailwind CSS v4

---

## File Structure

### Archivos nuevos — Backend
| Ruta | Descripción |
|---|---|
| `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/enums/TipoEmpleado.java` | Enum QUINCENAL \| POR_DIA |
| `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/enums/TipoPagoEmpleado.java` | Enum QUINCENA \| COBRO_DIAS |
| `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/Empleado.java` | Entidad empleado |
| `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/Adelanto.java` | Entidad adelanto de salario |
| `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/TrabajoDia.java` | Entidad día trabajado (solo POR_DIA) |
| `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/PagoEmpleado.java` | Entidad liquidación/pago |
| `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/Vacacion.java` | Entidad vacación (informativa) |
| `Backend/.../repositories/EmpleadoRepository.java` | — |
| `Backend/.../repositories/AdelantoRepository.java` | — |
| `Backend/.../repositories/TrabajoDiaRepository.java` | — |
| `Backend/.../repositories/PagoEmpleadoRepository.java` | — |
| `Backend/.../repositories/VacacionRepository.java` | — |
| `Backend/.../fachadaServices/dto/EmpleadoDTO.java` | — |
| `Backend/.../fachadaServices/dto/EmpleadoRequest.java` | — |
| `Backend/.../fachadaServices/dto/AdelantoDTO.java` | — |
| `Backend/.../fachadaServices/dto/AdelantoRequest.java` | — |
| `Backend/.../fachadaServices/dto/TrabajoDiaDTO.java` | — |
| `Backend/.../fachadaServices/dto/TrabajoDiaRequest.java` | — |
| `Backend/.../fachadaServices/dto/PagoEmpleadoDTO.java` | — |
| `Backend/.../fachadaServices/dto/PagoEmpleadoRequest.java` | — |
| `Backend/.../fachadaServices/dto/VacacionDTO.java` | — |
| `Backend/.../fachadaServices/dto/VacacionRequest.java` | — |
| `Backend/.../fachadaServices/dto/EmpleadoResumenDTO.java` | DTO compuesto para preview de pago |
| `Backend/.../fachadaServices/mapper/EmpleadoMapper.java` | MapStruct: 5 entidades → DTOs |
| `Backend/.../fachadaServices/services/empleadoService/IEmpleadoService.java` | — |
| `Backend/.../fachadaServices/services/empleadoService/EmpleadoServiceImpl.java` | Toda la lógica del módulo |
| `Backend/.../capaControladores/EmpleadoController.java` | `/api/v1/empleados` |

### Archivos nuevos — Frontend
| Ruta | Descripción |
|---|---|
| `Frontend/src/services/empleadoService.js` | Todas las llamadas a la API de empleados |
| `Frontend/src/components/empleados/ModalEmpleado.jsx` | Modal crear/editar empleado |
| `Frontend/src/components/empleados/ModalDetalleEmpleado.jsx` | Modal detalle con 4 sub-tabs |
| `Frontend/src/components/empleados/ModalLiquidar.jsx` | Modal preview + confirmar pago |
| `Frontend/src/pages/empleados/EmpleadosPage.jsx` | Página principal `/empleados` |

### Archivos modificados
| Ruta | Qué cambia |
|---|---|
| `Backend/.../models/enums/CajaMovimientoTipo.java` | Agregar `ADELANTO_EMPLEADO`, `PAGO_EMPLEADO` |
| `Backend/.../fachadaServices/mapper/FondoMapper.java` | Agregar 2 casos al switch exhaustivo |
| `Frontend/src/App.jsx` | Agregar ruta `/empleados` |
| `Frontend/src/components/layout/Sidebar.jsx` | Agregar ítem "Empleados" con ícono `Users` |
| `CLAUDE.md` | Actualizar documentación: entidades, endpoints, servicios, pantallas |

---

## Task 1: Rama git + Enums + CajaMovimientoTipo + FondoMapper

**Files:**
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/enums/TipoEmpleado.java`
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/enums/TipoPagoEmpleado.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/enums/CajaMovimientoTipo.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/mapper/FondoMapper.java`

- [ ] **Step 1: Crear rama feature**

```bash
git checkout develop
git checkout -b feature/tier4-empleados
```

- [ ] **Step 2: Crear `TipoEmpleado.java`**

Ruta: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/enums/TipoEmpleado.java`

```java
package com.casaverde.backend.capaAccesoADatos.models.enums;

public enum TipoEmpleado { QUINCENAL, POR_DIA }
```

- [ ] **Step 3: Crear `TipoPagoEmpleado.java`**

Ruta: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/enums/TipoPagoEmpleado.java`

```java
package com.casaverde.backend.capaAccesoADatos.models.enums;

public enum TipoPagoEmpleado { QUINCENA, COBRO_DIAS }
```

- [ ] **Step 4: Modificar `CajaMovimientoTipo.java`**

Agregar al final (antes del cierre del enum):

```java
COMPRA_PAGO,
ADELANTO_EMPLEADO,
PAGO_EMPLEADO
```

El archivo completo queda:

```java
package com.casaverde.backend.capaAccesoADatos.models.enums;

public enum CajaMovimientoTipo {
    COBRO_PEDIDO,
    REVERSO_PAGO,
    GASTO,
    REVERSO_GASTO,
    AJUSTE_FONDO,
    ENTRADA_MANUAL,
    TRANSFERENCIA_SALIDA,
    TRANSFERENCIA_ENTRADA,
    COMPRA_PAGO,
    ADELANTO_EMPLEADO,
    PAGO_EMPLEADO
}
```

- [ ] **Step 5: Modificar `FondoMapper.java`**

El switch en `resolveTipo` es exhaustivo — agregar los dos nuevos casos o el código no compilará. Reemplazar el método completo:

```java
@Named("resolveTipo")
default String resolveTipo(String tipoStr) {
    return switch (CajaMovimientoTipo.valueOf(tipoStr)) {
        case COBRO_PEDIDO -> "cobro";
        case GASTO -> "salida";
        case REVERSO_PAGO, REVERSO_GASTO -> "reverso";
        case AJUSTE_FONDO -> "ajuste";
        case ENTRADA_MANUAL -> "entrada";
        case TRANSFERENCIA_SALIDA -> "salida";
        case TRANSFERENCIA_ENTRADA -> "entrada";
        case COMPRA_PAGO -> "salida";
        case ADELANTO_EMPLEADO -> "salida";
        case PAGO_EMPLEADO -> "salida";
    };
}
```

- [ ] **Step 6: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/enums/ \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/mapper/FondoMapper.java
git commit -m "feat(empleados): enums TipoEmpleado, TipoPagoEmpleado; ADELANTO_EMPLEADO y PAGO_EMPLEADO en CajaMovimientoTipo"
```

---

## Task 2: JPA Entities

**Files:**
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/Empleado.java`
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/Adelanto.java`
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/TrabajoDia.java`
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/PagoEmpleado.java`
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/Vacacion.java`

- [ ] **Step 1: Crear `Empleado.java`**

```java
package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoEmpleado;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "empleado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEmpleado tipo;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal salario;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
```

- [ ] **Step 2: Crear `Adelanto.java`**

```java
package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "adelanto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adelanto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private String moneda;

    @Column(name = "monto_cop", nullable = false, precision = 19, scale = 0)
    private BigDecimal montoCop;

    @Column(name = "tasa_usada", precision = 19, scale = 2)
    private BigDecimal tasaUsada;

    @Column(name = "fondo_debitado", nullable = false)
    private String fondoDebitado;

    @Column(nullable = false)
    private LocalDate fecha;

    private String observacion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pagado = false;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
```

- [ ] **Step 3: Crear `TrabajoDia.java`**

```java
package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trabajo_dia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrabajoDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(nullable = false)
    private LocalDate fecha;

    private String observacion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pagado = false;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
```

- [ ] **Step 4: Crear `PagoEmpleado.java`**

```java
package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoPagoEmpleado;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pago_empleado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPagoEmpleado tipo;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fin", nullable = false)
    private LocalDate periodoFin;

    @Column(name = "dias_trabajados")
    private Integer diasTrabajados;

    @Column(name = "salario_bruto", nullable = false, precision = 19, scale = 0)
    private BigDecimal salarioBruto;

    @Column(name = "total_adelantos", nullable = false, precision = 19, scale = 0)
    private BigDecimal totalAdelantos;

    @Column(name = "total_neto", nullable = false, precision = 19, scale = 0)
    private BigDecimal totalNeto;

    @Column(nullable = false)
    private String moneda;

    @Column(name = "monto_pagado", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "monto_cop", nullable = false, precision = 19, scale = 0)
    private BigDecimal montoCop;

    @Column(name = "tasa_usada", precision = 19, scale = 2)
    private BigDecimal tasaUsada;

    @Column(name = "fondo_debitado", nullable = false)
    private String fondoDebitado;

    private String observacion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
```

- [ ] **Step 5: Crear `Vacacion.java`**

```java
package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vacacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vacacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    private String observacion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/
git commit -m "feat(empleados): entidades JPA Empleado, Adelanto, TrabajoDia, PagoEmpleado, Vacacion"
```

---

## Task 3: Repositories

**Files:** 5 interfaces en `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/`

- [ ] **Step 1: Crear `EmpleadoRepository.java`**

```java
package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Empleado;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    List<Empleado> findByActivoTrueOrderByNombreAsc();
    List<Empleado> findAllByOrderByNombreAsc();
}
```

- [ ] **Step 2: Crear `AdelantoRepository.java`**

```java
package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Adelanto;

public interface AdelantoRepository extends JpaRepository<Adelanto, Long> {
    List<Adelanto> findByEmpleadoIdOrderByCreadoEnDesc(Long empleadoId);
    List<Adelanto> findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(Long empleadoId);
}
```

- [ ] **Step 3: Crear `TrabajoDiaRepository.java`**

```java
package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.TrabajoDia;

public interface TrabajoDiaRepository extends JpaRepository<TrabajoDia, Long> {
    List<TrabajoDia> findByEmpleadoIdOrderByCreadoEnDesc(Long empleadoId);
    List<TrabajoDia> findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(Long empleadoId);
}
```

- [ ] **Step 4: Crear `PagoEmpleadoRepository.java`**

```java
package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.PagoEmpleado;

public interface PagoEmpleadoRepository extends JpaRepository<PagoEmpleado, Long> {
    List<PagoEmpleado> findByEmpleadoIdOrderByCreadoEnDesc(Long empleadoId);
}
```

- [ ] **Step 5: Crear `VacacionRepository.java`**

```java
package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Vacacion;

public interface VacacionRepository extends JpaRepository<Vacacion, Long> {
    List<Vacacion> findByEmpleadoIdOrderByCreadoEnDesc(Long empleadoId);
}
```

- [ ] **Step 6: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/Empleado*.java \
        Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/Adelanto*.java \
        Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/TrabajoDia*.java \
        Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/PagoEmpleado*.java \
        Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/Vacacion*.java
git commit -m "feat(empleados): repositorios JPA con métodos de consulta por empleado y estado pagado"
```

---

## Task 4: DTOs y Requests (11 archivos)

**Files:** En `Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/`

- [ ] **Step 1: Crear `EmpleadoDTO.java`**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoEmpleado;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmpleadoDTO {
    private Long id;
    private String nombre;
    private TipoEmpleado tipo;
    private BigDecimal salario;
    private Boolean activo;
    private LocalDateTime creadoEn;
}
```

- [ ] **Step 2: Crear `EmpleadoRequest.java`**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoEmpleado;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EmpleadoRequest {
    private String nombre;
    private TipoEmpleado tipo;
    private BigDecimal salario;
}
```

- [ ] **Step 3: Crear `AdelantoDTO.java`**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdelantoDTO {
    private Long id;
    private Long empleadoId;
    private BigDecimal monto;
    private String moneda;
    private BigDecimal montoCop;
    private BigDecimal tasaUsada;
    private String fondoDebitado;
    private LocalDate fecha;
    private String observacion;
    private Boolean pagado;
    private LocalDateTime creadoEn;
}
```

- [ ] **Step 4: Crear `AdelantoRequest.java`**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AdelantoRequest {
    private BigDecimal monto;
    private String moneda;
    private LocalDate fecha;
    private String observacion;
}
```

- [ ] **Step 5: Crear `TrabajoDiaDTO.java`**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrabajoDiaDTO {
    private Long id;
    private Long empleadoId;
    private LocalDate fecha;
    private String observacion;
    private Boolean pagado;
    private LocalDateTime creadoEn;
}
```

- [ ] **Step 6: Crear `TrabajoDiaRequest.java`**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDate;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TrabajoDiaRequest {
    private LocalDate fecha;
    private String observacion;
}
```

- [ ] **Step 7: Crear `PagoEmpleadoDTO.java`**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoPagoEmpleado;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PagoEmpleadoDTO {
    private Long id;
    private Long empleadoId;
    private TipoPagoEmpleado tipo;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private Integer diasTrabajados;
    private BigDecimal salarioBruto;
    private BigDecimal totalAdelantos;
    private BigDecimal totalNeto;
    private String moneda;
    private BigDecimal montoPagado;
    private BigDecimal montoCop;
    private BigDecimal tasaUsada;
    private String fondoDebitado;
    private String observacion;
    private LocalDateTime creadoEn;
}
```

- [ ] **Step 8: Crear `PagoEmpleadoRequest.java`**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDate;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PagoEmpleadoRequest {
    private String moneda;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private String observacion;
}
```

- [ ] **Step 9: Crear `VacacionDTO.java`**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VacacionDTO {
    private Long id;
    private Long empleadoId;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String observacion;
    private LocalDateTime creadoEn;
}
```

- [ ] **Step 10: Crear `VacacionRequest.java`**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDate;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VacacionRequest {
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String observacion;
}
```

- [ ] **Step 11: Crear `EmpleadoResumenDTO.java`**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmpleadoResumenDTO {
    private EmpleadoDTO empleado;
    private List<AdelantoDTO> adelantosPendientes;
    private List<TrabajoDiaDTO> diasPendientes;
    private BigDecimal totalAdelantosCop;
    private Integer totalDiasPendientes;
    private BigDecimal salarioBrutoEstimado;
    private BigDecimal netoEstimado;
}
```

- [ ] **Step 12: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/Empleado*.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/Adelanto*.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/TrabajoDia*.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/PagoEmpleado*.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/Vacacion*.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/EmpleadoResumenDTO.java
git commit -m "feat(empleados): DTOs y Requests (11 archivos)"
```

---

## Task 5: EmpleadoMapper

**Files:**
- Create: `Backend/src/main/java/com/casaverde/backend/fachadaServices/mapper/EmpleadoMapper.java`

- [ ] **Step 1: Crear `EmpleadoMapper.java`**

Un único mapper agrupa los 5 tipos del módulo. El campo `empleadoId` en los DTOs de sub-entidades se mapea desde `empleado.id`.

```java
package com.casaverde.backend.fachadaServices.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.casaverde.backend.capaAccesoADatos.models.entitys.*;
import com.casaverde.backend.fachadaServices.dto.*;

@Mapper(componentModel = "spring")
public interface EmpleadoMapper {

    EmpleadoDTO toDTO(Empleado e);

    @Mapping(source = "empleado.id", target = "empleadoId")
    AdelantoDTO toAdelantoDTO(Adelanto a);

    @Mapping(source = "empleado.id", target = "empleadoId")
    TrabajoDiaDTO toTrabajoDiaDTO(TrabajoDia t);

    @Mapping(source = "empleado.id", target = "empleadoId")
    PagoEmpleadoDTO toPagoDTO(PagoEmpleado p);

    @Mapping(source = "empleado.id", target = "empleadoId")
    VacacionDTO toVacacionDTO(Vacacion v);
}
```

- [ ] **Step 2: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/mapper/EmpleadoMapper.java
git commit -m "feat(empleados): EmpleadoMapper — mapeos para las 5 entidades del módulo"
```

---

## Task 6: IEmpleadoService + EmpleadoServiceImpl

**Files:**
- Create: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/empleadoService/IEmpleadoService.java`
- Create: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/empleadoService/EmpleadoServiceImpl.java`

- [ ] **Step 1: Crear `IEmpleadoService.java`**

```java
package com.casaverde.backend.fachadaServices.services.empleadoService;

import java.util.List;

import com.casaverde.backend.fachadaServices.dto.*;

public interface IEmpleadoService {

    List<EmpleadoDTO> listar(boolean soloActivos);
    EmpleadoDTO crear(EmpleadoRequest req);
    EmpleadoDTO actualizar(Long id, EmpleadoRequest req);
    EmpleadoDTO toggle(Long id);

    List<AdelantoDTO> listarAdelantos(Long empleadoId);
    AdelantoDTO registrarAdelanto(Long empleadoId, AdelantoRequest req);

    List<TrabajoDiaDTO> listarDias(Long empleadoId, boolean soloNoPagados);
    TrabajoDiaDTO registrarDia(Long empleadoId, TrabajoDiaRequest req);
    void eliminarDia(Long empleadoId, Long diaId);

    List<PagoEmpleadoDTO> listarPagos(Long empleadoId);
    EmpleadoResumenDTO previewPago(Long empleadoId);
    PagoEmpleadoDTO liquidar(Long empleadoId, PagoEmpleadoRequest req);

    List<VacacionDTO> listarVacaciones(Long empleadoId);
    VacacionDTO registrarVacacion(Long empleadoId, VacacionRequest req);
    void eliminarVacacion(Long empleadoId, Long vacId);
}
```

- [ ] **Step 2: Crear `EmpleadoServiceImpl.java`**

Notas críticas de implementación:
- `registrarAdelanto`: debita el fondo **inmediatamente** usando `ADELANTO_EMPLEADO` con monto negativo.
- `liquidar`: calcula bruto según tipo (QUINCENAL → `salario`; POR_DIA → `salario × días_pendientes`), valida `neto >= 0`, convierte a moneda elegida si es USD, debita fondo con `PAGO_EMPLEADO`, marca adelantos y días como `pagado = true`.
- `previewPago`: idéntico al cálculo de `liquidar` pero sin ejecutar ninguna escritura (read-only).
- La firma de `cajaMovimientoService.registrar` es: `(tipo, moneda, monto, montoCop, refTipo, refId, concepto, observacion)`. Los montos son **negativos** para débitos.
- Para USD: `montoPagado = neto / tasaUsd` (redondeo HALF_UP, scale 2). `montoCop = neto` (siempre COP).

```java
package com.casaverde.backend.fachadaServices.services.empleadoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.*;
import com.casaverde.backend.capaAccesoADatos.models.enums.*;
import com.casaverde.backend.capaAccesoADatos.repositories.*;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.*;
import com.casaverde.backend.fachadaServices.mapper.EmpleadoMapper;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;
import com.casaverde.backend.fachadaServices.services.pagoService.ICajaMovimientoService;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmpleadoServiceImpl implements IEmpleadoService {

    private final EmpleadoRepository empleadoRepo;
    private final AdelantoRepository adelantoRepo;
    private final TrabajoDiaRepository trabajoDiaRepo;
    private final PagoEmpleadoRepository pagoRepo;
    private final VacacionRepository vacacionRepo;
    private final ICajaMovimientoService cajaMovimientoService;
    private final ITasaCambioService tasaCambioService;
    private final IAuditoriaService auditoriaService;
    private final EmpleadoMapper empleadoMapper;

    // ─── Empleados ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<EmpleadoDTO> listar(boolean soloActivos) {
        List<Empleado> lista = soloActivos
                ? empleadoRepo.findByActivoTrueOrderByNombreAsc()
                : empleadoRepo.findAllByOrderByNombreAsc();
        return lista.stream().map(empleadoMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EmpleadoDTO crear(EmpleadoRequest req) {
        if (req.getNombre() == null || req.getNombre().isBlank())
            throw new ValidacionException("El nombre es obligatorio");
        if (req.getSalario() == null || req.getSalario().compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidacionException("El salario debe ser mayor a 0");
        Empleado emp = empleadoRepo.save(Empleado.builder()
                .nombre(req.getNombre().trim())
                .tipo(req.getTipo())
                .salario(req.getSalario().setScale(0, RoundingMode.HALF_UP))
                .build());
        auditoriaService.registrar("Empleado", emp.getId(), "CREAR",
                "Empleado " + emp.getNombre() + " tipo " + emp.getTipo() + " creado");
        return empleadoMapper.toDTO(emp);
    }

    @Override
    @Transactional
    public EmpleadoDTO actualizar(Long id, EmpleadoRequest req) {
        Empleado emp = findEmpleado(id);
        if (req.getNombre() != null && !req.getNombre().isBlank())
            emp.setNombre(req.getNombre().trim());
        if (req.getSalario() != null) {
            if (req.getSalario().compareTo(BigDecimal.ZERO) <= 0)
                throw new ValidacionException("El salario debe ser mayor a 0");
            emp.setSalario(req.getSalario().setScale(0, RoundingMode.HALF_UP));
        }
        auditoriaService.registrar("Empleado", id, "ACTUALIZAR", "Empleado #" + id + " actualizado");
        return empleadoMapper.toDTO(empleadoRepo.save(emp));
    }

    @Override
    @Transactional
    public EmpleadoDTO toggle(Long id) {
        Empleado emp = findEmpleado(id);
        emp.setActivo(!emp.getActivo());
        auditoriaService.registrar("Empleado", id,
                emp.getActivo() ? "ACTIVAR" : "DESACTIVAR",
                "Empleado #" + id + " " + (emp.getActivo() ? "activado" : "desactivado"));
        return empleadoMapper.toDTO(empleadoRepo.save(emp));
    }

    // ─── Adelantos ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AdelantoDTO> listarAdelantos(Long empleadoId) {
        findEmpleado(empleadoId);
        return adelantoRepo.findByEmpleadoIdOrderByCreadoEnDesc(empleadoId)
                .stream().map(empleadoMapper::toAdelantoDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdelantoDTO registrarAdelanto(Long empleadoId, AdelantoRequest req) {
        Empleado emp = findEmpleado(empleadoId);
        String moneda = req.getMoneda().trim().toUpperCase();
        BigDecimal tasaUsd = tasaCambioService.getValorVigente();
        BigDecimal montoCop = "USD".equals(moneda)
                ? req.getMonto().multiply(tasaUsd).setScale(0, RoundingMode.HALF_UP)
                : req.getMonto().setScale(0, RoundingMode.HALF_UP);
        BigDecimal tasaUsada = "USD".equals(moneda) ? tasaUsd : null;

        cajaMovimientoService.registrar(
                CajaMovimientoTipo.ADELANTO_EMPLEADO,
                moneda,
                req.getMonto().negate(),
                montoCop.negate(),
                "empleado",
                empleadoId,
                "Adelanto a " + emp.getNombre(),
                req.getObservacion());

        Adelanto adelanto = adelantoRepo.save(Adelanto.builder()
                .empleado(emp)
                .monto(req.getMonto())
                .moneda(moneda)
                .montoCop(montoCop)
                .tasaUsada(tasaUsada)
                .fondoDebitado(moneda)
                .fecha(req.getFecha())
                .observacion(req.getObservacion())
                .build());

        auditoriaService.registrar("Adelanto", adelanto.getId(), "REGISTRAR",
                "Adelanto $" + req.getMonto() + " " + moneda + " a " + emp.getNombre());
        return empleadoMapper.toAdelantoDTO(adelanto);
    }

    // ─── Días trabajados ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TrabajoDiaDTO> listarDias(Long empleadoId, boolean soloNoPagados) {
        findEmpleado(empleadoId);
        List<TrabajoDia> dias = soloNoPagados
                ? trabajoDiaRepo.findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(empleadoId)
                : trabajoDiaRepo.findByEmpleadoIdOrderByCreadoEnDesc(empleadoId);
        return dias.stream().map(empleadoMapper::toTrabajoDiaDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TrabajoDiaDTO registrarDia(Long empleadoId, TrabajoDiaRequest req) {
        Empleado emp = findEmpleado(empleadoId);
        if (emp.getTipo() != TipoEmpleado.POR_DIA)
            throw new ValidacionException("Solo se registran días para empleados de tipo POR_DIA");
        TrabajoDia dia = trabajoDiaRepo.save(TrabajoDia.builder()
                .empleado(emp)
                .fecha(req.getFecha())
                .observacion(req.getObservacion())
                .build());
        auditoriaService.registrar("TrabajoDia", dia.getId(), "REGISTRAR",
                "Día " + dia.getFecha() + " para " + emp.getNombre());
        return empleadoMapper.toTrabajoDiaDTO(dia);
    }

    @Override
    @Transactional
    public void eliminarDia(Long empleadoId, Long diaId) {
        TrabajoDia dia = trabajoDiaRepo.findById(diaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Día no encontrado: " + diaId));
        if (!dia.getEmpleado().getId().equals(empleadoId))
            throw new ValidacionException("El día no pertenece al empleado indicado");
        if (dia.getPagado())
            throw new ValidacionException("No se puede eliminar un día ya pagado");
        trabajoDiaRepo.delete(dia);
        auditoriaService.registrar("TrabajoDia", diaId, "ELIMINAR",
                "Día " + dia.getFecha() + " de empleado #" + empleadoId + " eliminado");
    }

    // ─── Pagos / Liquidación ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PagoEmpleadoDTO> listarPagos(Long empleadoId) {
        findEmpleado(empleadoId);
        return pagoRepo.findByEmpleadoIdOrderByCreadoEnDesc(empleadoId)
                .stream().map(empleadoMapper::toPagoDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmpleadoResumenDTO previewPago(Long empleadoId) {
        Empleado emp = findEmpleado(empleadoId);
        List<Adelanto> adelantosPend = adelantoRepo.findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(empleadoId);
        List<TrabajoDia> diasPend = emp.getTipo() == TipoEmpleado.POR_DIA
                ? trabajoDiaRepo.findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(empleadoId)
                : List.of();

        BigDecimal totalAdelantos = adelantosPend.stream()
                .map(Adelanto::getMontoCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);

        int diasCount = diasPend.size();
        BigDecimal bruto = emp.getTipo() == TipoEmpleado.QUINCENAL
                ? emp.getSalario()
                : emp.getSalario().multiply(BigDecimal.valueOf(diasCount));
        BigDecimal neto = bruto.subtract(totalAdelantos);

        return EmpleadoResumenDTO.builder()
                .empleado(empleadoMapper.toDTO(emp))
                .adelantosPendientes(adelantosPend.stream()
                        .map(empleadoMapper::toAdelantoDTO).collect(Collectors.toList()))
                .diasPendientes(diasPend.stream()
                        .map(empleadoMapper::toTrabajoDiaDTO).collect(Collectors.toList()))
                .totalAdelantosCop(totalAdelantos)
                .totalDiasPendientes(diasCount)
                .salarioBrutoEstimado(bruto)
                .netoEstimado(neto)
                .build();
    }

    @Override
    @Transactional
    public PagoEmpleadoDTO liquidar(Long empleadoId, PagoEmpleadoRequest req) {
        Empleado emp = findEmpleado(empleadoId);

        List<Adelanto> adelantosPend = adelantoRepo.findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(empleadoId);
        BigDecimal totalAdelantos = adelantosPend.stream()
                .map(Adelanto::getMontoCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal bruto;
        Integer diasCount = null;
        TipoPagoEmpleado tipoPago;
        List<TrabajoDia> diasPend = null;

        if (emp.getTipo() == TipoEmpleado.QUINCENAL) {
            bruto = emp.getSalario();
            tipoPago = TipoPagoEmpleado.QUINCENA;
        } else {
            diasPend = trabajoDiaRepo.findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(empleadoId);
            if (diasPend.isEmpty())
                throw new ValidacionException("El empleado no tiene días pendientes de cobro");
            diasCount = diasPend.size();
            bruto = emp.getSalario().multiply(BigDecimal.valueOf(diasCount));
            tipoPago = TipoPagoEmpleado.COBRO_DIAS;
        }

        BigDecimal neto = bruto.subtract(totalAdelantos);
        if (neto.compareTo(BigDecimal.ZERO) < 0)
            throw new ValidacionException(
                    "Los adelantos ($" + totalAdelantos + ") superan el salario bruto ($" + bruto + ")");

        String moneda = req.getMoneda().trim().toUpperCase();
        BigDecimal tasaUsd = tasaCambioService.getValorVigente();
        BigDecimal montoPagado;
        BigDecimal tasaUsada = null;

        if ("USD".equals(moneda)) {
            tasaUsada = tasaUsd;
            montoPagado = neto.divide(tasaUsd, 2, RoundingMode.HALF_UP);
        } else {
            montoPagado = neto;
        }

        cajaMovimientoService.registrar(
                CajaMovimientoTipo.PAGO_EMPLEADO,
                moneda,
                montoPagado.negate(),
                neto.negate(),
                "empleado",
                empleadoId,
                "Pago " + tipoPago.name() + " " + emp.getNombre(),
                req.getObservacion());

        adelantosPend.forEach(a -> a.setPagado(true));
        adelantoRepo.saveAll(adelantosPend);

        if (diasPend != null) {
            diasPend.forEach(d -> d.setPagado(true));
            trabajoDiaRepo.saveAll(diasPend);
        }

        PagoEmpleado pago = pagoRepo.save(PagoEmpleado.builder()
                .empleado(emp)
                .tipo(tipoPago)
                .periodoInicio(req.getPeriodoInicio())
                .periodoFin(req.getPeriodoFin())
                .diasTrabajados(diasCount)
                .salarioBruto(bruto)
                .totalAdelantos(totalAdelantos)
                .totalNeto(neto)
                .moneda(moneda)
                .montoPagado(montoPagado)
                .montoCop(neto)
                .tasaUsada(tasaUsada)
                .fondoDebitado(moneda)
                .observacion(req.getObservacion())
                .build());

        auditoriaService.registrar("PagoEmpleado", pago.getId(), "LIQUIDAR",
                "Pago " + tipoPago + " a " + emp.getNombre() + " neto: $" + neto);
        return empleadoMapper.toPagoDTO(pago);
    }

    // ─── Vacaciones ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<VacacionDTO> listarVacaciones(Long empleadoId) {
        findEmpleado(empleadoId);
        return vacacionRepo.findByEmpleadoIdOrderByCreadoEnDesc(empleadoId)
                .stream().map(empleadoMapper::toVacacionDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VacacionDTO registrarVacacion(Long empleadoId, VacacionRequest req) {
        Empleado emp = findEmpleado(empleadoId);
        Vacacion vac = vacacionRepo.save(Vacacion.builder()
                .empleado(emp)
                .fechaInicio(req.getFechaInicio())
                .fechaFin(req.getFechaFin())
                .observacion(req.getObservacion())
                .build());
        auditoriaService.registrar("Vacacion", vac.getId(), "REGISTRAR",
                "Vacación " + req.getFechaInicio() + " - " + req.getFechaFin() + " para " + emp.getNombre());
        return empleadoMapper.toVacacionDTO(vac);
    }

    @Override
    @Transactional
    public void eliminarVacacion(Long empleadoId, Long vacId) {
        Vacacion vac = vacacionRepo.findById(vacId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vacación no encontrada: " + vacId));
        if (!vac.getEmpleado().getId().equals(empleadoId))
            throw new ValidacionException("La vacación no pertenece al empleado indicado");
        vacacionRepo.delete(vac);
        auditoriaService.registrar("Vacacion", vacId, "ELIMINAR",
                "Vacación de empleado #" + empleadoId + " eliminada");
    }

    // ─── Helper ─────────────────────────────────────────────────────────────

    private Empleado findEmpleado(Long id) {
        return empleadoRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empleado no encontrado: " + id));
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/services/empleadoService/
git commit -m "feat(empleados): IEmpleadoService + EmpleadoServiceImpl — lógica completa de empleados, adelantos, días, liquidación y vacaciones"
```

---

## Task 7: EmpleadoController + verificar compilación

**Files:**
- Create: `Backend/src/main/java/com/casaverde/backend/capaControladores/EmpleadoController.java`

- [ ] **Step 1: Crear `EmpleadoController.java`**

```java
package com.casaverde.backend.capaControladores;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.*;
import com.casaverde.backend.fachadaServices.services.empleadoService.IEmpleadoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/empleados")
@RequiredArgsConstructor
public class EmpleadoController {

    private final IEmpleadoService empleadoService;

    @GetMapping
    public List<EmpleadoDTO> listar(
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        return empleadoService.listar(soloActivos);
    }

    @PostMapping
    public ResponseEntity<EmpleadoDTO> crear(@RequestBody EmpleadoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empleadoService.crear(req));
    }

    @PutMapping("/{id}")
    public EmpleadoDTO actualizar(@PathVariable Long id, @RequestBody EmpleadoRequest req) {
        return empleadoService.actualizar(id, req);
    }

    @PutMapping("/{id}/toggle")
    public EmpleadoDTO toggle(@PathVariable Long id) {
        return empleadoService.toggle(id);
    }

    // ─── Adelantos ────────────────────────────────────────────────────────

    @GetMapping("/{id}/adelantos")
    public List<AdelantoDTO> listarAdelantos(@PathVariable Long id) {
        return empleadoService.listarAdelantos(id);
    }

    @PostMapping("/{id}/adelantos")
    public ResponseEntity<AdelantoDTO> registrarAdelanto(
            @PathVariable Long id, @RequestBody AdelantoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(empleadoService.registrarAdelanto(id, req));
    }

    // ─── Días trabajados ──────────────────────────────────────────────────

    @GetMapping("/{id}/dias")
    public List<TrabajoDiaDTO> listarDias(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean soloNoPagados) {
        return empleadoService.listarDias(id, soloNoPagados);
    }

    @PostMapping("/{id}/dias")
    public ResponseEntity<TrabajoDiaDTO> registrarDia(
            @PathVariable Long id, @RequestBody TrabajoDiaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(empleadoService.registrarDia(id, req));
    }

    @DeleteMapping("/{id}/dias/{diaId}")
    public ResponseEntity<Void> eliminarDia(
            @PathVariable Long id, @PathVariable Long diaId) {
        empleadoService.eliminarDia(id, diaId);
        return ResponseEntity.noContent().build();
    }

    // ─── Pagos ────────────────────────────────────────────────────────────

    @GetMapping("/{id}/pagos")
    public List<PagoEmpleadoDTO> listarPagos(@PathVariable Long id) {
        return empleadoService.listarPagos(id);
    }

    @GetMapping("/{id}/pagos/preview")
    public EmpleadoResumenDTO previewPago(@PathVariable Long id) {
        return empleadoService.previewPago(id);
    }

    @PostMapping("/{id}/pagos")
    public ResponseEntity<PagoEmpleadoDTO> liquidar(
            @PathVariable Long id, @RequestBody PagoEmpleadoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(empleadoService.liquidar(id, req));
    }

    // ─── Vacaciones ───────────────────────────────────────────────────────

    @GetMapping("/{id}/vacaciones")
    public List<VacacionDTO> listarVacaciones(@PathVariable Long id) {
        return empleadoService.listarVacaciones(id);
    }

    @PostMapping("/{id}/vacaciones")
    public ResponseEntity<VacacionDTO> registrarVacacion(
            @PathVariable Long id, @RequestBody VacacionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(empleadoService.registrarVacacion(id, req));
    }

    @DeleteMapping("/{id}/vacaciones/{vacId}")
    public ResponseEntity<Void> eliminarVacacion(
            @PathVariable Long id, @PathVariable Long vacId) {
        empleadoService.eliminarVacacion(id, vacId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Verificar compilación**

```bash
cd Backend && mvn compile -q
```

Expected: BUILD SUCCESS sin errores. Si falla por un campo mal nombrado o tipo incorrecto, corregir antes de continuar.

- [ ] **Step 3: Correr tests existentes**

```bash
mvn test -q
```

Expected: BUILD SUCCESS. Los tests existentes no deben romperse.

- [ ] **Step 4: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/capaControladores/EmpleadoController.java
git commit -m "feat(empleados): EmpleadoController — 14 endpoints bajo /api/v1/empleados"
```

---

## Task 8: Frontend — empleadoService.js

**Files:**
- Create: `Frontend/src/services/empleadoService.js`

- [ ] **Step 1: Crear `empleadoService.js`**

```js
import api from "./api";

export const empleadoService = {
  listar: (soloActivos = true) =>
    api.get("/empleados", { params: { soloActivos } }).then((r) => r.data),
  crear: (data) => api.post("/empleados", data).then((r) => r.data),
  actualizar: (id, data) => api.put(`/empleados/${id}`, data).then((r) => r.data),
  toggle: (id) => api.put(`/empleados/${id}/toggle`).then((r) => r.data),

  listarAdelantos: (id) =>
    api.get(`/empleados/${id}/adelantos`).then((r) => r.data),
  registrarAdelanto: (id, data) =>
    api.post(`/empleados/${id}/adelantos`, data).then((r) => r.data),

  listarDias: (id, soloNoPagados = false) =>
    api.get(`/empleados/${id}/dias`, { params: { soloNoPagados } }).then((r) => r.data),
  registrarDia: (id, data) =>
    api.post(`/empleados/${id}/dias`, data).then((r) => r.data),
  eliminarDia: (id, diaId) =>
    api.delete(`/empleados/${id}/dias/${diaId}`).then((r) => r.data),

  listarPagos: (id) =>
    api.get(`/empleados/${id}/pagos`).then((r) => r.data),
  previewPago: (id) =>
    api.get(`/empleados/${id}/pagos/preview`).then((r) => r.data),
  liquidar: (id, data) =>
    api.post(`/empleados/${id}/pagos`, data).then((r) => r.data),

  listarVacaciones: (id) =>
    api.get(`/empleados/${id}/vacaciones`).then((r) => r.data),
  registrarVacacion: (id, data) =>
    api.post(`/empleados/${id}/vacaciones`, data).then((r) => r.data),
  eliminarVacacion: (id, vacId) =>
    api.delete(`/empleados/${id}/vacaciones/${vacId}`).then((r) => r.data),
};
```

- [ ] **Step 2: Commit**

```bash
git add Frontend/src/services/empleadoService.js
git commit -m "feat(empleados): empleadoService.js — servicio frontend con todos los endpoints del módulo"
```

---

## Task 9: ModalEmpleado.jsx + EmpleadosPage.jsx

**Files:**
- Create: `Frontend/src/components/empleados/ModalEmpleado.jsx`
- Create: `Frontend/src/pages/empleados/EmpleadosPage.jsx`

- [ ] **Step 1: Crear `ModalEmpleado.jsx`**

Ruta: `Frontend/src/components/empleados/ModalEmpleado.jsx`

El label del campo salario cambia dinámicamente según el tipo seleccionado.

```jsx
import { useState } from "react";
import { X } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

export default function ModalEmpleado({ empleado, onClose, onSave }) {
  const [form, setForm] = useState({
    nombre: empleado?.nombre ?? "",
    tipo: empleado?.tipo ?? "QUINCENAL",
    salario: empleado?.salario ?? "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.nombre.trim()) { setError("El nombre es obligatorio"); return; }
    if (!form.salario || Number(form.salario) <= 0) { setError("El salario debe ser mayor a 0"); return; }
    setSaving(true);
    try {
      await onSave({ nombre: form.nombre.trim(), tipo: form.tipo, salario: Number(form.salario) });
      onClose();
    } catch (err) {
      setError(err.response?.data?.message ?? err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <span className="modal-title">
            {empleado ? "Editar empleado" : "Nuevo empleado"}
          </span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group">
              <label className="form-label">Nombre *</label>
              <input
                className="form-control"
                value={form.nombre}
                onChange={(e) => setForm((f) => ({ ...f, nombre: e.target.value }))}
                placeholder="Nombre completo"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Tipo *</label>
              <select
                className="form-control"
                value={form.tipo}
                onChange={(e) => setForm((f) => ({ ...f, tipo: e.target.value }))}
              >
                <option value="QUINCENAL">Quincenal (salario fijo)</option>
                <option value="POR_DIA">Por día (tarifa diaria)</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">
                {form.tipo === "QUINCENAL" ? "Salario quincenal (COP) *" : "Tarifa por día (COP) *"}
              </label>
              <input
                type="number"
                className="form-control"
                value={form.salario}
                onChange={(e) => setForm((f) => ({ ...f, salario: e.target.value }))}
                placeholder="0"
                min="1"
                step="1"
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Guardar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Crear `EmpleadosPage.jsx`**

Ruta: `Frontend/src/pages/empleados/EmpleadosPage.jsx`

```jsx
import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Power, Eye } from "lucide-react";
import { empleadoService } from "../../services/empleadoService";
import { formatCOP } from "../../utils/format";
import ModalEmpleado from "../../components/empleados/ModalEmpleado";
import ModalDetalleEmpleado from "../../components/empleados/ModalDetalleEmpleado";

const TIPO_BADGE = {
  QUINCENAL: "badge-info",
  POR_DIA:   "badge-warning",
};

export default function EmpleadosPage() {
  const qc = useQueryClient();
  const [soloActivos, setSoloActivos] = useState(true);
  const [creando, setCreando] = useState(false);
  const [editando, setEditando] = useState(null);
  const [detalle, setDetalle] = useState(null);

  const { data: empleados = [], isLoading } = useQuery({
    queryKey: ["empleados", soloActivos],
    queryFn: () => empleadoService.listar(soloActivos),
  });

  const mutCreate = useMutation({
    mutationFn: empleadoService.crear,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["empleados"] }),
  });
  const mutUpdate = useMutation({
    mutationFn: ({ id, data }) => empleadoService.actualizar(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["empleados"] }),
  });
  const mutToggle = useMutation({
    mutationFn: empleadoService.toggle,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["empleados"] }),
  });

  const handleSave = async (data) => {
    if (editando) await mutUpdate.mutateAsync({ id: editando.id, data });
    else await mutCreate.mutateAsync(data);
  };

  return (
    <div className="page-content">
      <div className="page-header">
        <h1 className="page-title">Empleados</h1>
        <div className="flex items-center gap-2">
          <button
            className={`btn btn-sm ${soloActivos ? "btn-primary" : "btn-secondary"}`}
            onClick={() => setSoloActivos((v) => !v)}
          >
            {soloActivos ? "Activos" : "Todos"}
          </button>
          <button className="btn btn-primary" onClick={() => setCreando(true)}>
            <Plus size={15} /> Nuevo empleado
          </button>
        </div>
      </div>

      <div className="card">
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : empleados.length === 0 ? (
            <div className="empty-state"><p>Sin empleados registrados</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Nombre</th><th>Tipo</th><th>Salario</th><th>Estado</th><th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {empleados.map((emp) => (
                  <tr key={emp.id}>
                    <td className="font-medium">{emp.nombre}</td>
                    <td>
                      <span className={`badge ${TIPO_BADGE[emp.tipo] ?? "badge-gray"}`}>
                        {emp.tipo === "QUINCENAL" ? "Quincenal" : "Por día"}
                      </span>
                    </td>
                    <td>{formatCOP(emp.salario)}</td>
                    <td>
                      <span className={`badge ${emp.activo ? "badge-success" : "badge-danger"}`}>
                        {emp.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-1">
                        <button className="btn-icon" title="Editar" onClick={() => setEditando(emp)}>
                          <Pencil size={15} />
                        </button>
                        <button
                          className="btn-icon"
                          title={emp.activo ? "Desactivar" : "Activar"}
                          onClick={() => mutToggle.mutate(emp.id)}
                        >
                          <Power size={15} className={emp.activo ? "text-green-600" : "text-red-600"} />
                        </button>
                        <button className="btn-icon" title="Ver detalle" onClick={() => setDetalle(emp)}>
                          <Eye size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {(creando || editando) && (
        <ModalEmpleado
          empleado={editando}
          onClose={() => { setCreando(false); setEditando(null); }}
          onSave={handleSave}
        />
      )}
      {detalle && (
        <ModalDetalleEmpleado
          empleado={detalle}
          onClose={() => setDetalle(null)}
        />
      )}
    </div>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add Frontend/src/components/empleados/ModalEmpleado.jsx \
        Frontend/src/pages/empleados/EmpleadosPage.jsx
git commit -m "feat(empleados): ModalEmpleado y EmpleadosPage con tabla y acciones"
```

---

## Task 10: ModalDetalleEmpleado.jsx

**Files:**
- Create: `Frontend/src/components/empleados/ModalDetalleEmpleado.jsx`

- [ ] **Step 1: Crear `ModalDetalleEmpleado.jsx`**

Modal lg con 4 sub-tabs. El tab "Días trabajados" solo aparece si `empleado.tipo === "POR_DIA"`. El contador de días pendientes y el estimado se calculan en el cliente desde la lista completa. El botón "Registrar pago" abre `ModalLiquidar` en un portal sobre este modal.

```jsx
import { useState } from "react";
import { X, Trash2 } from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { empleadoService } from "../../services/empleadoService";
import { fondoService } from "../../services/fondoService";
import { formatCOP, formatCreadoEn } from "../../utils/format";
import ModalLiquidar from "./ModalLiquidar";

const hoy = () => new Date().toLocaleDateString("en-CA");

const calcDias = (inicio, fin) =>
  Math.round((new Date(fin) - new Date(inicio)) / 86400000) + 1;

export default function ModalDetalleEmpleado({ empleado, onClose }) {
  const qc = useQueryClient();
  const [tab, setTab] = useState("adelantos");
  const [liquidando, setLiquidando] = useState(false);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const [adelantoForm, setAdelantoForm] = useState({
    monto: "", moneda: "COP", fecha: hoy(), observacion: "",
  });
  const [diaForm, setDiaForm] = useState({ fecha: hoy(), observacion: "" });
  const [vacForm, setVacForm] = useState({
    fechaInicio: hoy(), fechaFin: hoy(), observacion: "",
  });

  useModalKeyboard({ onEscape: onClose });

  const { data: fondos = [] } = useQuery({
    queryKey: ["fondos"],
    queryFn: fondoService.listar,
  });

  const { data: adelantos = [], isLoading: loadAdelantos } = useQuery({
    queryKey: ["empleado-adelantos", empleado.id],
    queryFn: () => empleadoService.listarAdelantos(empleado.id),
  });

  const { data: dias = [], isLoading: loadDias } = useQuery({
    queryKey: ["empleado-dias", empleado.id],
    queryFn: () => empleadoService.listarDias(empleado.id),
    enabled: empleado.tipo === "POR_DIA",
  });

  const { data: pagos = [], isLoading: loadPagos } = useQuery({
    queryKey: ["empleado-pagos", empleado.id],
    queryFn: () => empleadoService.listarPagos(empleado.id),
  });

  const { data: vacaciones = [], isLoading: loadVacaciones } = useQuery({
    queryKey: ["empleado-vacaciones", empleado.id],
    queryFn: () => empleadoService.listarVacaciones(empleado.id),
  });

  const mutAdelanto = useMutation({
    mutationFn: (data) => empleadoService.registrarAdelanto(empleado.id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["empleado-adelantos", empleado.id] });
      setAdelantoForm({ monto: "", moneda: "COP", fecha: hoy(), observacion: "" });
      setError("");
    },
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const mutRegistrarDia = useMutation({
    mutationFn: (data) => empleadoService.registrarDia(empleado.id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["empleado-dias", empleado.id] });
      setDiaForm({ fecha: hoy(), observacion: "" });
      setError("");
    },
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const mutEliminarDia = useMutation({
    mutationFn: (diaId) => empleadoService.eliminarDia(empleado.id, diaId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["empleado-dias", empleado.id] }),
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const mutVacacion = useMutation({
    mutationFn: (data) => empleadoService.registrarVacacion(empleado.id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["empleado-vacaciones", empleado.id] });
      setVacForm({ fechaInicio: hoy(), fechaFin: hoy(), observacion: "" });
      setError("");
    },
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const mutEliminarVac = useMutation({
    mutationFn: (vacId) => empleadoService.eliminarVacacion(empleado.id, vacId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["empleado-vacaciones", empleado.id] }),
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const handleRegistrarAdelanto = async (e) => {
    e.preventDefault();
    if (!adelantoForm.monto || Number(adelantoForm.monto) <= 0) {
      setError("El monto debe ser mayor a 0"); return;
    }
    setSaving(true);
    try {
      await mutAdelanto.mutateAsync({
        monto: Number(adelantoForm.monto),
        moneda: adelantoForm.moneda,
        fecha: adelantoForm.fecha,
        observacion: adelantoForm.observacion || null,
      });
    } finally { setSaving(false); }
  };

  const handleRegistrarDia = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await mutRegistrarDia.mutateAsync({
        fecha: diaForm.fecha,
        observacion: diaForm.observacion || null,
      });
    } finally { setSaving(false); }
  };

  const handleRegistrarVacacion = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await mutVacacion.mutateAsync({
        fechaInicio: vacForm.fechaInicio,
        fechaFin: vacForm.fechaFin,
        observacion: vacForm.observacion || null,
      });
    } finally { setSaving(false); }
  };

  const diasPendientes = dias.filter((d) => !d.pagado);
  const estimadoDias = diasPendientes.length * Number(empleado.salario);

  const TABS = [
    { id: "adelantos", label: "Adelantos" },
    ...(empleado.tipo === "POR_DIA" ? [{ id: "dias", label: "Días trabajados" }] : []),
    { id: "pagos", label: "Pagos" },
    { id: "vacaciones", label: "Vacaciones" },
  ];

  return (
    <div className="modal-overlay">
      <div className="modal modal-lg">
        <div className="modal-header">
          <span className="modal-title">{empleado.nombre}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="modal-body">
          {error && <div className="alert alert-error mb-3">{error}</div>}

          <div className="tabs mb-4">
            {TABS.map((t) => (
              <button
                key={t.id}
                className={`tab ${tab === t.id ? "activo" : ""}`}
                onClick={() => { setTab(t.id); setError(""); }}
              >
                {t.label}
              </button>
            ))}
          </div>

          {/* ── Tab Adelantos ── */}
          {tab === "adelantos" && (
            <div>
              {loadAdelantos ? (
                <div className="loading-center"><div className="spinner" /></div>
              ) : adelantos.length > 0 ? (
                <div className="table-wrap mb-4">
                  <table>
                    <thead>
                      <tr><th>Fecha</th><th>Monto</th><th>Moneda</th><th>Fondo</th><th>Estado</th></tr>
                    </thead>
                    <tbody>
                      {adelantos.map((a) => (
                        <tr key={a.id}>
                          <td>{a.fecha}</td>
                          <td>{Number(a.monto).toLocaleString("es-CO")}</td>
                          <td>{a.moneda}</td>
                          <td>{a.fondoDebitado}</td>
                          <td>
                            <span className={`badge ${a.pagado ? "badge-success" : "badge-warning"}`}>
                              {a.pagado ? "Pagado" : "Pendiente"}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
              <form onSubmit={handleRegistrarAdelanto} className="border-t pt-4">
                <p className="text-sm font-medium mb-3">Registrar adelanto</p>
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Monto *</label>
                    <input
                      type="number" className="form-control" min="1" step="1"
                      value={adelantoForm.monto}
                      onChange={(e) => setAdelantoForm((f) => ({ ...f, monto: e.target.value }))}
                      placeholder="0"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Fondo *</label>
                    <select
                      className="form-control"
                      value={adelantoForm.moneda}
                      onChange={(e) => setAdelantoForm((f) => ({ ...f, moneda: e.target.value }))}
                    >
                      {fondos.map((f) => (
                        <option key={f.moneda} value={f.moneda}>{f.alias ?? f.moneda}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Fecha *</label>
                    <input
                      type="date" className="form-control"
                      value={adelantoForm.fecha}
                      onChange={(e) => setAdelantoForm((f) => ({ ...f, fecha: e.target.value }))}
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label className="form-label">Observación</label>
                  <input
                    className="form-control"
                    value={adelantoForm.observacion}
                    onChange={(e) => setAdelantoForm((f) => ({ ...f, observacion: e.target.value }))}
                    placeholder="Opcional"
                  />
                </div>
                <div className="flex justify-end mt-2">
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? "Registrando…" : "Registrar adelanto"}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* ── Tab Días trabajados ── */}
          {tab === "dias" && (
            <div>
              <div className="flex items-center gap-4 mb-3 text-sm bg-gray-50 rounded p-3">
                <span>
                  <strong>{diasPendientes.length}</strong> días pendientes —{" "}
                  <strong>{formatCOP(estimadoDias)}</strong> estimado
                </span>
              </div>
              {loadDias ? (
                <div className="loading-center"><div className="spinner" /></div>
              ) : dias.length > 0 ? (
                <div className="table-wrap mb-4">
                  <table>
                    <thead>
                      <tr><th>Fecha</th><th>Observación</th><th>Estado</th><th></th></tr>
                    </thead>
                    <tbody>
                      {dias.map((d) => (
                        <tr key={d.id}>
                          <td>{d.fecha}</td>
                          <td>{d.observacion ?? "—"}</td>
                          <td>
                            <span className={`badge ${d.pagado ? "badge-success" : "badge-warning"}`}>
                              {d.pagado ? "Pagado" : "Pendiente"}
                            </span>
                          </td>
                          <td>
                            {!d.pagado && (
                              <button
                                className="btn-icon"
                                title="Eliminar"
                                onClick={() => mutEliminarDia.mutate(d.id)}
                              >
                                <Trash2 size={14} className="text-red-500" />
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
              <form onSubmit={handleRegistrarDia} className="border-t pt-4">
                <p className="text-sm font-medium mb-3">Registrar día trabajado</p>
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Fecha *</label>
                    <input
                      type="date" className="form-control"
                      value={diaForm.fecha}
                      onChange={(e) => setDiaForm((f) => ({ ...f, fecha: e.target.value }))}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Observación</label>
                    <input
                      className="form-control"
                      value={diaForm.observacion}
                      onChange={(e) => setDiaForm((f) => ({ ...f, observacion: e.target.value }))}
                      placeholder="Opcional"
                    />
                  </div>
                </div>
                <div className="flex justify-end mt-2">
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? "Registrando…" : "Registrar día"}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* ── Tab Pagos ── */}
          {tab === "pagos" && (
            <div>
              {loadPagos ? (
                <div className="loading-center"><div className="spinner" /></div>
              ) : pagos.length > 0 ? (
                <div className="table-wrap mb-4">
                  <table>
                    <thead>
                      <tr>
                        <th>Fecha</th><th>Periodo</th><th>Bruto</th>
                        <th>Adelantos</th><th>Neto</th><th>Moneda</th><th>Fondo</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pagos.map((p) => (
                        <tr key={p.id}>
                          <td className="text-xs">{p.creadoEn ? formatCreadoEn(p.creadoEn) : "—"}</td>
                          <td className="text-xs">{p.periodoInicio} / {p.periodoFin}</td>
                          <td>{formatCOP(p.salarioBruto)}</td>
                          <td>{formatCOP(p.totalAdelantos)}</td>
                          <td className="font-medium">{formatCOP(p.totalNeto)}</td>
                          <td>{p.moneda}</td>
                          <td>{p.fondoDebitado}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
              <div className="flex justify-end border-t pt-4">
                <button className="btn btn-primary" onClick={() => setLiquidando(true)}>
                  Registrar pago
                </button>
              </div>
            </div>
          )}

          {/* ── Tab Vacaciones ── */}
          {tab === "vacaciones" && (
            <div>
              {loadVacaciones ? (
                <div className="loading-center"><div className="spinner" /></div>
              ) : vacaciones.length > 0 ? (
                <div className="table-wrap mb-4">
                  <table>
                    <thead>
                      <tr><th>Inicio</th><th>Fin</th><th>Días</th><th>Observación</th><th></th></tr>
                    </thead>
                    <tbody>
                      {vacaciones.map((v) => (
                        <tr key={v.id}>
                          <td>{v.fechaInicio}</td>
                          <td>{v.fechaFin}</td>
                          <td>{calcDias(v.fechaInicio, v.fechaFin)}</td>
                          <td>{v.observacion ?? "—"}</td>
                          <td>
                            <button
                              className="btn-icon"
                              title="Eliminar"
                              onClick={() => mutEliminarVac.mutate(v.id)}
                            >
                              <Trash2 size={14} className="text-red-500" />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
              <form onSubmit={handleRegistrarVacacion} className="border-t pt-4">
                <p className="text-sm font-medium mb-3">Registrar periodo de vacaciones</p>
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Fecha inicio *</label>
                    <input
                      type="date" className="form-control"
                      value={vacForm.fechaInicio}
                      onChange={(e) => setVacForm((f) => ({ ...f, fechaInicio: e.target.value }))}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Fecha fin *</label>
                    <input
                      type="date" className="form-control"
                      value={vacForm.fechaFin}
                      onChange={(e) => setVacForm((f) => ({ ...f, fechaFin: e.target.value }))}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Observación</label>
                    <input
                      className="form-control"
                      value={vacForm.observacion}
                      onChange={(e) => setVacForm((f) => ({ ...f, observacion: e.target.value }))}
                      placeholder="Opcional"
                    />
                  </div>
                </div>
                <div className="flex justify-end mt-2">
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? "Registrando…" : "Registrar vacaciones"}
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>Cerrar</button>
        </div>
      </div>

      {liquidando && (
        <ModalLiquidar
          empleado={empleado}
          onClose={() => setLiquidando(false)}
          onSuccess={() => {
            qc.invalidateQueries({ queryKey: ["empleado-pagos", empleado.id] });
            qc.invalidateQueries({ queryKey: ["empleado-adelantos", empleado.id] });
            qc.invalidateQueries({ queryKey: ["empleado-dias", empleado.id] });
            setLiquidando(false);
          }}
        />
      )}
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add Frontend/src/components/empleados/ModalDetalleEmpleado.jsx
git commit -m "feat(empleados): ModalDetalleEmpleado — 4 sub-tabs (Adelantos, Días, Pagos, Vacaciones)"
```

---

## Task 11: ModalLiquidar.jsx

**Files:**
- Create: `Frontend/src/components/empleados/ModalLiquidar.jsx`

- [ ] **Step 1: Crear `ModalLiquidar.jsx`**

Al abrirse llama `previewPago` para mostrar el resumen calculado. El admin completa moneda (fondo), periodo y observación. Al confirmar se llama `liquidar` y en `onSuccess` se invalidan las queries del modal padre.

```jsx
import { useState } from "react";
import { X } from "lucide-react";
import { useQuery, useMutation } from "@tanstack/react-query";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { empleadoService } from "../../services/empleadoService";
import { fondoService } from "../../services/fondoService";
import { formatCOP } from "../../utils/format";

const hoy = () => new Date().toLocaleDateString("en-CA");

export default function ModalLiquidar({ empleado, onClose, onSuccess }) {
  const [form, setForm] = useState({
    moneda: "COP",
    periodoInicio: hoy(),
    periodoFin: hoy(),
    observacion: "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const { data: preview, isLoading: loadPreview } = useQuery({
    queryKey: ["empleado-preview", empleado.id],
    queryFn: () => empleadoService.previewPago(empleado.id),
  });

  const { data: fondos = [] } = useQuery({
    queryKey: ["fondos"],
    queryFn: fondoService.listar,
  });

  const mutLiquidar = useMutation({
    mutationFn: (data) => empleadoService.liquidar(empleado.id, data),
    onSuccess: () => onSuccess(),
    onError: (err) => {
      setError(err.response?.data?.message ?? err.message);
      setSaving(false);
    },
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.periodoInicio || !form.periodoFin) {
      setError("El periodo es obligatorio"); return;
    }
    setSaving(true);
    try {
      await mutLiquidar.mutateAsync({
        moneda: form.moneda,
        periodoInicio: form.periodoInicio,
        periodoFin: form.periodoFin,
        observacion: form.observacion || null,
      });
    } catch {
      // error handled in onError
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <span className="modal-title">Registrar pago — {empleado.nombre}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="modal-body">
          {error && <div className="alert alert-error mb-3">{error}</div>}

          {loadPreview ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : preview ? (
            <div className="bg-gray-50 rounded p-3 mb-4 text-sm">
              <p className="font-medium mb-2">Resumen del pago</p>
              <div className="grid grid-cols-2 gap-2">
                {empleado.tipo === "POR_DIA" && (
                  <div>
                    <span className="text-gray-500">Días pendientes:</span>{" "}
                    <strong>{preview.totalDiasPendientes}</strong>
                  </div>
                )}
                <div>
                  <span className="text-gray-500">Salario bruto:</span>{" "}
                  <strong>{formatCOP(preview.salarioBrutoEstimado)}</strong>
                </div>
                <div>
                  <span className="text-gray-500">Adelantos a descontar:</span>{" "}
                  <strong className="text-amber-700">{formatCOP(preview.totalAdelantosCop)}</strong>
                </div>
                <div>
                  <span className="text-gray-500">Neto a pagar:</span>{" "}
                  <strong className={Number(preview.netoEstimado) < 0 ? "text-red-600" : "text-green-700"}>
                    {formatCOP(preview.netoEstimado)}
                  </strong>
                </div>
              </div>
              {preview.adelantosPendientes.length > 0 && (
                <p className="text-xs text-gray-500 mt-2">
                  {preview.adelantosPendientes.length} adelanto(s) pendiente(s) se descontarán automáticamente
                </p>
              )}
            </div>
          ) : null}

          <form onSubmit={handleSubmit}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Periodo inicio *</label>
                <input
                  type="date" className="form-control"
                  value={form.periodoInicio}
                  onChange={(e) => setForm((f) => ({ ...f, periodoInicio: e.target.value }))}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Periodo fin *</label>
                <input
                  type="date" className="form-control"
                  value={form.periodoFin}
                  onChange={(e) => setForm((f) => ({ ...f, periodoFin: e.target.value }))}
                />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Pagar desde *</label>
              <select
                className="form-control"
                value={form.moneda}
                onChange={(e) => setForm((f) => ({ ...f, moneda: e.target.value }))}
              >
                {fondos.map((f) => (
                  <option key={f.moneda} value={f.moneda}>{f.alias ?? f.moneda}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Observación</label>
              <input
                className="form-control"
                value={form.observacion}
                onChange={(e) => setForm((f) => ({ ...f, observacion: e.target.value }))}
                placeholder="Opcional"
              />
            </div>
            <div className="flex justify-end gap-2 mt-4">
              <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={saving || loadPreview || (preview && Number(preview.netoEstimado) < 0)}
              >
                {saving ? "Procesando…" : "Confirmar pago"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add Frontend/src/components/empleados/ModalLiquidar.jsx
git commit -m "feat(empleados): ModalLiquidar — preview de pago y confirmación de liquidación"
```

---

## Task 12: App.jsx + Sidebar.jsx + CLAUDE.md + merge

**Files:**
- Modify: `Frontend/src/App.jsx`
- Modify: `Frontend/src/components/layout/Sidebar.jsx`
- Modify: `CLAUDE.md`

- [ ] **Step 1: Modificar `App.jsx`**

Agregar import después del import de `FondosPage`:

```js
import EmpleadosPage from "./pages/empleados/EmpleadosPage";
```

Agregar ruta antes de `{ path: "*", element: <NotFound /> }`:

```js
{ path: "empleados",    element: <EmpleadosPage /> },
```

El array `routes` completo queda:

```js
const routes = [
  {
    path: "/",
    element: <AppLayout />,
    errorElement: <ErrorPage />,
    children: [
      { index: true,           element: <Dashboard /> },
      { path: "pedidos",       element: <PedidosPage /> },
      { path: "resumen",       element: <ResumenPage /> },
      { path: "inventario",    element: <InventarioPage /> },
      { path: "fondos",        element: <FondosPage /> },
      { path: "configuracion", element: <ConfiguracionPage /> },
      { path: "empleados",     element: <EmpleadosPage /> },
      { path: "*",             element: <NotFound /> },
    ],
  },
];
```

- [ ] **Step 2: Modificar `Sidebar.jsx`**

Agregar `Users` a la lista de imports de lucide-react:

```js
import {
  LayoutDashboard, ClipboardList, CheckSquare,
  Package, Wallet, Settings, Users,
} from "lucide-react";
```

Agregar al array `links` antes del ítem de Configuración:

```js
{ to: "/empleados",     icon: Users,           label: "Empleados" },
```

El array `links` completo queda:

```js
const links = [
  { to: "/",              icon: LayoutDashboard, label: "Dashboard" },
  { to: "/pedidos",       icon: ClipboardList,   label: "Pedidos" },
  { to: "/resumen",       icon: CheckSquare,     label: "Resumen del día" },
  { to: "/inventario",    icon: Package,         label: "Inventario" },
  { to: "/fondos",        icon: Wallet,          label: "Fondos" },
  { to: "/empleados",     icon: Users,           label: "Empleados" },
  { to: "/configuracion", icon: Settings,        label: "Configuración" },
];
```

- [ ] **Step 3: Actualizar `CLAUDE.md`**

Cambios necesarios en CLAUDE.md:

**3a.** En la sección de estructura backend, agregar `empleadoService/` al bloque `services/`:

```
└── services/
    ├── configuracionService/   ← IConfiguracionGeneralService, ITasaCambioService
    ├── pagoService/            ← IFondoService, ICajaMovimientoService
    ├── pedidoService/          ← IPedidoService, ICierreCajaService, IResumenService, IComprobanteService
    ├── compraService/          ← IProveedorService, ICompraService
    └── productoService/        ← IProductoService, ICategoriaProductoService, ...
    └── empleadoService/        ← IEmpleadoService
```

**3b.** Agregar sección de API REST para `/api/v1/empleados` con los 14 endpoints.

**3c.** Agregar las 5 entidades nuevas a la sección "Entidades implementadas".

**3d.** En la sección de `CajaMovimientoTipo`, agregar `ADELANTO_EMPLEADO` y `PAGO_EMPLEADO`.

**3e.** En `Frontend/src/services/` agregar `empleadoService`.

**3f.** En la tabla de pantallas, agregar:

```
| Empleados | `/empleados` | ✅ Implementado |
```

- [ ] **Step 4: Verificar frontend compila**

```bash
cd Frontend && npm run build 2>&1 | tail -20
```

Expected: sin errores de TypeScript/JSX. Si hay errores de import, corregirlos.

- [ ] **Step 5: Commit final del tier**

```bash
git add Frontend/src/App.jsx \
        Frontend/src/components/layout/Sidebar.jsx \
        CLAUDE.md
git commit -m "feat(empleados): integrar ruta /empleados + ítem en sidebar + actualizar CLAUDE.md"
```

- [ ] **Step 6: Merge a develop**

```bash
git checkout develop
git merge --no-ff feature/tier4-empleados -m "feat: Tier 4 — Módulo de Empleados (salarios, adelantos, quincenas, días, vacaciones)"
git branch -d feature/tier4-empleados
```

---

## Self-Review

### Spec coverage

| Requisito del spec | Implementado en |
|---|---|
| Empleados QUINCENAL y POR_DIA | Task 1 (TipoEmpleado), Task 2 (Empleado), Task 6 (lógica en liquidar) |
| Adelantos debitan fondo inmediatamente | Task 6 (registrarAdelanto → cajaMovimientoService con ADELANTO_EMPLEADO) |
| Adelantos se marcan pagado=true al liquidar | Task 6 (liquidar → forEach setPagado(true) + saveAll) |
| TrabajoDia solo para POR_DIA | Task 6 (registrarDia valida tipo POR_DIA) |
| Eliminar día no pagado | Task 6 (eliminarDia valida pagado=false) |
| Liquidación QUINCENAL: bruto = salario | Task 6 (if QUINCENAL → bruto = salario) |
| Liquidación POR_DIA: bruto = días × salario | Task 6 (else → bruto = salario × diasCount) |
| totalAdelantos = suma montoCop pendientes | Task 6 (stream().reduce) |
| neto = bruto - totalAdelantos | Task 6 |
| Validar neto >= 0 | Task 6 (throw ValidacionException si negativo) |
| Convertir a moneda elegida (USD) | Task 6 (neto.divide(tasaUsd, 2)) |
| Debitar fondo con PAGO_EMPLEADO | Task 6 (cajaMovimientoService.registrar) |
| Marcar días pendientes como pagados | Task 6 (diasPend.forEach setPagado(true)) |
| Guardar PagoEmpleado con snapshot | Task 6 (pagoRepo.save con todos los campos) |
| Preview sin ejecutar | Task 6 (previewPago, @Transactional readOnly) |
| Vacaciones informativas | Task 6 (registrarVacacion/eliminarVacacion) |
| GET /pagos/preview antes de POST /pagos | Task 7 (controller: GetMapping "/{id}/pagos/preview" antes de PostMapping "/{id}/pagos") |
| Página /empleados con tabla | Task 9 (EmpleadosPage) |
| ModalDetalleEmpleado con 4 sub-tabs | Task 10 |
| Tab Días solo si POR_DIA | Task 10 (TABS array condicional) |
| Contador días pendientes + estimado | Task 10 (diasPendientes.filter + salario × count) |
| ModalLiquidar con preview | Task 11 (useQuery previewPago) |
| Switch exhaustivo FondoMapper | Task 1 (ADELANTO_EMPLEADO, PAGO_EMPLEADO → "salida") |
| Ruta sidebar /empleados | Task 12 |

### Consistencia de tipos

- `cajaMovimientoService.registrar(tipo, moneda, monto, montoCop, refTipo, refId, concepto, obs)` — firma usada igual que en CompraServiceImpl.
- `montoCop` en `Adelanto` tiene scale 0 (COP sin decimales) — consistente con el resto del sistema.
- `montoPagado` en `PagoEmpleado` tiene scale 2 (puede ser USD) — correcto.
- `TipoEmpleado` y `TipoPagoEmpleado` serializados como STRING por Jackson — el frontend puede comparar `emp.tipo === "POR_DIA"`.
