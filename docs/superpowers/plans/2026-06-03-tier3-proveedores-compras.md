# Tier 3 — Proveedores y Compras: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Proveedores and Compras module — vendors, formal purchases that auto-increase inventory, and partial payments that debit funds.

**Architecture:** Backend-first then frontend. 3 new JPA entities (`Proveedor`, `Compra`, `AbonoCompra`), 3 repositories, 6 DTOs/Requests, 3 MapStruct mappers, 2 service pairs, 2 REST controllers. Frontend adds 2 tabs and 3 modals to `ConfiguracionPage`. `CajaMovimientoTipo.COMPRA_PAGO` added to the existing enum to wire fund debits into the existing book.

**Tech Stack:** Java 17 + Spring Boot 3.4.7 + MapStruct 1.6.3 + Lombok. React 19 + TanStack Query v5 + lucide-react. No new dependencies.

---

## File Map

### Backend — create
| File | Responsibility |
|---|---|
| `capaAccesoADatos/models/enums/TipoProveedor.java` | Enum: CUY, GALLINA, AMBOS, OTROS |
| `capaAccesoADatos/models/enums/EstadoCompra.java` | Enum: PENDIENTE, ABONADO_PARCIAL, PAGADO |
| `capaAccesoADatos/models/entitys/Proveedor.java` | JPA entity — proveedor |
| `capaAccesoADatos/models/entitys/Compra.java` | JPA entity — compra con snapshot + estado |
| `capaAccesoADatos/models/entitys/AbonoCompra.java` | JPA entity — abono con fondo debitado |
| `capaAccesoADatos/repositories/ProveedorRepository.java` | JpaRepository + findByActivoTrue |
| `capaAccesoADatos/repositories/CompraRepository.java` | JpaRepository + filtros por fecha/estado |
| `capaAccesoADatos/repositories/AbonoCompraRepository.java` | JpaRepository + sum montoCop |
| `fachadaServices/dto/ProveedorDTO.java` | DTO salida |
| `fachadaServices/dto/ProveedorRequest.java` | DTO entrada |
| `fachadaServices/dto/CompraDTO.java` | DTO salida con totalAbonado/saldoPendiente calculados |
| `fachadaServices/dto/CompraRequest.java` | DTO entrada |
| `fachadaServices/dto/AbonoCompraDTO.java` | DTO salida |
| `fachadaServices/dto/AbonoCompraRequest.java` | DTO entrada |
| `fachadaServices/mapper/ProveedorMapper.java` | MapStruct: Proveedor ↔ ProveedorDTO |
| `fachadaServices/mapper/CompraMapper.java` | MapStruct: Compra → CompraDTO (campos básicos) |
| `fachadaServices/mapper/AbonoCompraMapper.java` | MapStruct: AbonoCompra → AbonoCompraDTO |
| `fachadaServices/services/compraService/IProveedorService.java` | Interface |
| `fachadaServices/services/compraService/ProveedorServiceImpl.java` | CRUD de proveedores |
| `fachadaServices/services/compraService/ICompraService.java` | Interface |
| `fachadaServices/services/compraService/CompraServiceImpl.java` | Lógica de compras + abonos |
| `capaControladores/ProveedorController.java` | REST /api/v1/proveedores |
| `capaControladores/CompraController.java` | REST /api/v1/compras |

### Backend — modify
| File | Change |
|---|---|
| `capaAccesoADatos/models/enums/CajaMovimientoTipo.java` | Add `COMPRA_PAGO` |

### Frontend — create
| File | Responsibility |
|---|---|
| `Frontend/src/services/proveedorService.js` | HTTP CRUD proveedores |
| `Frontend/src/services/compraService.js` | HTTP compras + abonos |
| `Frontend/src/components/configuracion/ModalProveedor.jsx` | Modal crear/editar proveedor |
| `Frontend/src/components/configuracion/ModalRegistrarCompra.jsx` | Modal registrar compra |
| `Frontend/src/components/configuracion/ModalDetalleCompra.jsx` | Modal detalle + registrar abono |
| `Frontend/src/pages/configuracion/tabs/ProveedoresTab.jsx` | Tab gestión proveedores |
| `Frontend/src/pages/configuracion/tabs/ComprasTab.jsx` | Tab compras con filtros |

### Frontend — modify
| File | Change |
|---|---|
| `Frontend/src/pages/configuracion/ConfiguracionPage.jsx` | Add tabs 11 and 12 |

---

## Task 1: Git branch

- [ ] **Step 1: Create feature branch**

```bash
git checkout develop
git checkout -b feature/tier3-proveedores-compras
```

Expected: branch created, prompt shows `feature/tier3-proveedores-compras`.

---

## Task 2: Enums

**Files:**
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/enums/TipoProveedor.java`
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/enums/EstadoCompra.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/enums/CajaMovimientoTipo.java`

- [ ] **Step 1: Create TipoProveedor**

```java
package com.casaverde.backend.capaAccesoADatos.models.enums;

public enum TipoProveedor {
    CUY, GALLINA, AMBOS, OTROS
}
```

- [ ] **Step 2: Create EstadoCompra**

```java
package com.casaverde.backend.capaAccesoADatos.models.enums;

public enum EstadoCompra {
    PENDIENTE, ABONADO_PARCIAL, PAGADO
}
```

- [ ] **Step 3: Add COMPRA_PAGO to CajaMovimientoTipo**

Open `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/enums/CajaMovimientoTipo.java` and add the new value:

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
    COMPRA_PAGO
}
```

- [ ] **Step 4: Compile check**

```bash
cd Backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS, no errors.

- [ ] **Step 5: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/enums/
git commit -m "feat(tier3): enums TipoProveedor, EstadoCompra y COMPRA_PAGO en CajaMovimientoTipo"
```

---

## Task 3: JPA Entities

**Files:**
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/Proveedor.java`
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/Compra.java`
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/AbonoCompra.java`

- [ ] **Step 1: Create Proveedor entity**

```java
package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.time.LocalDateTime;
import java.time.ZoneId;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoProveedor tipo;

    private String telefono;

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

- [ ] **Step 2: Create Compra entity**

```java
package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(name = "proveedor_nombre", nullable = false)
    private String proveedorNombre;

    @Column(nullable = false)
    private String insumo;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", precision = 19, scale = 0, nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "total_cop", precision = 19, scale = 0, nullable = false)
    private BigDecimal totalCop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCompra estado;

    private String observacion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
```

- [ ] **Step 3: Create AbonoCompra entity**

```java
package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "abono_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private String moneda;

    @Column(name = "monto_cop", precision = 19, scale = 0, nullable = false)
    private BigDecimal montoCop;

    @Column(name = "tasa_usada", precision = 19, scale = 2)
    private BigDecimal tasaUsada;

    @Column(name = "fondo_debitado", nullable = false)
    private String fondoDebitado;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
```

- [ ] **Step 4: Compile check**

```bash
cd Backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/Proveedor.java \
        Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/Compra.java \
        Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/AbonoCompra.java
git commit -m "feat(tier3): entities Proveedor, Compra, AbonoCompra"
```

---

## Task 4: Repositories

**Files:**
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/ProveedorRepository.java`
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/CompraRepository.java`
- Create: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/AbonoCompraRepository.java`

- [ ] **Step 1: Create ProveedorRepository**

```java
package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    List<Proveedor> findByActivoTrue();
    List<Proveedor> findAllByOrderByNombreAsc();
}
```

- [ ] **Step 2: Create CompraRepository**

```java
package com.casaverde.backend.capaAccesoADatos.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Compra;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByFechaOrderByCreadoEnDesc(LocalDate fecha);
    List<Compra> findByEstadoOrderByCreadoEnDesc(EstadoCompra estado);
    List<Compra> findByFechaAndEstadoOrderByCreadoEnDesc(LocalDate fecha, EstadoCompra estado);
    List<Compra> findAllByOrderByCreadoEnDesc();
}
```

- [ ] **Step 3: Create AbonoCompraRepository**

```java
package com.casaverde.backend.capaAccesoADatos.repositories;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.casaverde.backend.capaAccesoADatos.models.entitys.AbonoCompra;

public interface AbonoCompraRepository extends JpaRepository<AbonoCompra, Long> {
    List<AbonoCompra> findByCompraIdOrderByCreadoEnDesc(Long compraId);

    @Query("SELECT COALESCE(SUM(a.montoCop), 0) FROM AbonoCompra a WHERE a.compra.id = :compraId")
    BigDecimal sumMontoCopByCompraId(@Param("compraId") Long compraId);
}
```

- [ ] **Step 4: Compile check**

```bash
cd Backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/ProveedorRepository.java \
        Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/CompraRepository.java \
        Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/AbonoCompraRepository.java
git commit -m "feat(tier3): repositories Proveedor, Compra, AbonoCompra"
```

---

## Task 5: DTOs and Requests

**Files:** 6 new files in `Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/`

- [ ] **Step 1: Create ProveedorDTO**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDateTime;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorDTO {
    private Long id;
    private String nombre;
    private TipoProveedor tipo;
    private String telefono;
    private Boolean activo;
    private LocalDateTime creadoEn;
}
```

- [ ] **Step 2: Create ProveedorRequest**

```java
package com.casaverde.backend.fachadaServices.dto;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "El tipo es obligatorio")
    private TipoProveedor tipo;

    private String telefono;
}
```

- [ ] **Step 3: Create CompraDTO**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraDTO {
    private Long id;
    private Long proveedorId;
    private String proveedorNombre;
    private String insumo;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal totalCop;
    private BigDecimal totalAbonado;
    private BigDecimal saldoPendiente;
    private EstadoCompra estado;
    private String observacion;
    private LocalDate fecha;
    private LocalDateTime creadoEn;
    private List<AbonoCompraDTO> abonos;
}
```

- [ ] **Step 4: Create CompraRequest**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraRequest {
    @NotNull(message = "El proveedor es obligatorio")
    private Long proveedorId;

    @NotBlank(message = "El insumo es obligatorio")
    private String insumo;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @Positive(message = "El precio unitario debe ser mayor a 0")
    private BigDecimal precioUnitario;

    private String observacion;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;
}
```

- [ ] **Step 5: Create AbonoCompraDTO**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonoCompraDTO {
    private Long id;
    private Long compraId;
    private BigDecimal monto;
    private String moneda;
    private BigDecimal montoCop;
    private BigDecimal tasaUsada;
    private String fondoDebitado;
    private LocalDate fecha;
    private LocalDateTime creadoEn;
}
```

- [ ] **Step 6: Create AbonoCompraRequest**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonoCompraRequest {
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotBlank(message = "La moneda es obligatoria")
    private String moneda;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;
}
```

- [ ] **Step 7: Compile check**

```bash
cd Backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/ProveedorDTO.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/ProveedorRequest.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/CompraDTO.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/CompraRequest.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/AbonoCompraDTO.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/AbonoCompraRequest.java
git commit -m "feat(tier3): DTOs y Requests para Proveedor, Compra, AbonoCompra"
```

---

## Task 6: MapStruct Mappers

**Files:** 3 new files in `Backend/src/main/java/com/casaverde/backend/fachadaServices/mapper/`

> Note: `CompraMapper` maps basic Compra fields only. `totalAbonado`, `saldoPendiente`, and `abonos` are set by the service after mapping.

- [ ] **Step 1: Create ProveedorMapper**

```java
package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor;
import com.casaverde.backend.fachadaServices.dto.ProveedorDTO;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProveedorMapper {
    ProveedorDTO toDTO(Proveedor proveedor);
}
```

- [ ] **Step 2: Create CompraMapper**

```java
package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Compra;
import com.casaverde.backend.fachadaServices.dto.CompraDTO;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompraMapper {

    @Mapping(source = "proveedor.id", target = "proveedorId")
    @Mapping(target = "totalAbonado", ignore = true)
    @Mapping(target = "saldoPendiente", ignore = true)
    @Mapping(target = "abonos", ignore = true)
    CompraDTO toDTO(Compra compra);
}
```

- [ ] **Step 3: Create AbonoCompraMapper**

```java
package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.AbonoCompra;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraDTO;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AbonoCompraMapper {

    @Mapping(source = "compra.id", target = "compraId")
    AbonoCompraDTO toDTO(AbonoCompra abono);
}
```

- [ ] **Step 4: Compile check**

```bash
cd Backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS. MapStruct generates implementations in `target/generated-sources`.

- [ ] **Step 5: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/mapper/ProveedorMapper.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/mapper/CompraMapper.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/mapper/AbonoCompraMapper.java
git commit -m "feat(tier3): mappers Proveedor, Compra, AbonoCompra"
```

---

## Task 7: ProveedorService

**Files:**
- Create: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/compraService/IProveedorService.java`
- Create: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/compraService/ProveedorServiceImpl.java`

- [ ] **Step 1: Create IProveedorService**

```java
package com.casaverde.backend.fachadaServices.services.compraService;

import java.util.List;

import com.casaverde.backend.fachadaServices.dto.ProveedorDTO;
import com.casaverde.backend.fachadaServices.dto.ProveedorRequest;

public interface IProveedorService {
    List<ProveedorDTO> listar(boolean soloActivos);
    ProveedorDTO crear(ProveedorRequest req);
    ProveedorDTO actualizar(Long id, ProveedorRequest req);
    ProveedorDTO toggle(Long id);
}
```

- [ ] **Step 2: Create ProveedorServiceImpl**

```java
package com.casaverde.backend.fachadaServices.services.compraService;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor;
import com.casaverde.backend.capaAccesoADatos.repositories.ProveedorRepository;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.ProveedorDTO;
import com.casaverde.backend.fachadaServices.dto.ProveedorRequest;
import com.casaverde.backend.fachadaServices.mapper.ProveedorMapper;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements IProveedorService {

    private final ProveedorRepository proveedorRepo;
    private final ProveedorMapper proveedorMapper;
    private final IAuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<ProveedorDTO> listar(boolean soloActivos) {
        List<Proveedor> lista = soloActivos
                ? proveedorRepo.findByActivoTrue()
                : proveedorRepo.findAllByOrderByNombreAsc();
        return lista.stream().map(proveedorMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public ProveedorDTO crear(ProveedorRequest req) {
        if (req.getNombre() == null || req.getNombre().isBlank())
            throw new ValidacionException("El nombre del proveedor es obligatorio");
        if (req.getTipo() == null)
            throw new ValidacionException("El tipo de proveedor es obligatorio");

        Proveedor p = Proveedor.builder()
                .nombre(req.getNombre().trim())
                .tipo(req.getTipo())
                .telefono(req.getTelefono() != null ? req.getTelefono().trim() : null)
                .build();
        Proveedor guardado = proveedorRepo.save(p);
        auditoriaService.registrar("Proveedor", guardado.getId(), "CREAR", guardado.getNombre());
        return proveedorMapper.toDTO(guardado);
    }

    @Transactional
    public ProveedorDTO actualizar(Long id, ProveedorRequest req) {
        Proveedor p = proveedorRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + id));
        if (req.getNombre() == null || req.getNombre().isBlank())
            throw new ValidacionException("El nombre del proveedor es obligatorio");
        if (req.getTipo() == null)
            throw new ValidacionException("El tipo de proveedor es obligatorio");

        p.setNombre(req.getNombre().trim());
        p.setTipo(req.getTipo());
        p.setTelefono(req.getTelefono() != null ? req.getTelefono().trim() : null);
        auditoriaService.registrar("Proveedor", p.getId(), "ACTUALIZAR", p.getNombre());
        return proveedorMapper.toDTO(p);
    }

    @Transactional
    public ProveedorDTO toggle(Long id) {
        Proveedor p = proveedorRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + id));
        p.setActivo(!p.getActivo());
        String accion = p.getActivo() ? "ACTIVAR" : "DESACTIVAR";
        auditoriaService.registrar("Proveedor", p.getId(), accion, p.getNombre());
        return proveedorMapper.toDTO(p);
    }
}
```

- [ ] **Step 3: Compile check**

```bash
cd Backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/services/compraService/IProveedorService.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/services/compraService/ProveedorServiceImpl.java
git commit -m "feat(tier3): IProveedorService + ProveedorServiceImpl"
```

---

## Task 8: ProveedorController

**File:**
- Create: `Backend/src/main/java/com/casaverde/backend/capaControladores/ProveedorController.java`

- [ ] **Step 1: Create ProveedorController**

```java
package com.casaverde.backend.capaControladores;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.ProveedorDTO;
import com.casaverde.backend.fachadaServices.dto.ProveedorRequest;
import com.casaverde.backend.fachadaServices.services.compraService.IProveedorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final IProveedorService proveedorService;

    @GetMapping
    public List<ProveedorDTO> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return proveedorService.listar(soloActivos);
    }

    @PostMapping
    public ResponseEntity<ProveedorDTO> crear(@RequestBody ProveedorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.crear(req));
    }

    @PutMapping("/{id}")
    public ProveedorDTO actualizar(@PathVariable Long id, @RequestBody ProveedorRequest req) {
        return proveedorService.actualizar(id, req);
    }

    @PutMapping("/{id}/toggle")
    public ProveedorDTO toggle(@PathVariable Long id) {
        return proveedorService.toggle(id);
    }
}
```

- [ ] **Step 2: Compile check**

```bash
cd Backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/capaControladores/ProveedorController.java
git commit -m "feat(tier3): ProveedorController GET/POST/PUT/toggle"
```

---

## Task 9: CompraService

**Files:**
- Create: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/compraService/ICompraService.java`
- Create: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/compraService/CompraServiceImpl.java`

- [ ] **Step 1: Create ICompraService**

```java
package com.casaverde.backend.fachadaServices.services.compraService;

import java.time.LocalDate;
import java.util.List;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraDTO;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraRequest;
import com.casaverde.backend.fachadaServices.dto.CompraDTO;
import com.casaverde.backend.fachadaServices.dto.CompraRequest;

public interface ICompraService {
    List<CompraDTO> listar(LocalDate fecha, EstadoCompra estado);
    CompraDTO obtener(Long id);
    CompraDTO registrar(CompraRequest req);
    void anular(Long id);
    AbonoCompraDTO registrarAbono(Long compraId, AbonoCompraRequest req);
    List<AbonoCompraDTO> listarAbonos(Long compraId);
}
```

- [ ] **Step 2: Create CompraServiceImpl**

```java
package com.casaverde.backend.fachadaServices.services.compraService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.AbonoCompra;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Compra;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Inventario;
import com.casaverde.backend.capaAccesoADatos.models.entitys.MovimientoInventario;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;
import com.casaverde.backend.capaAccesoADatos.repositories.AbonoCompraRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.CompraRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.InventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.MovimientoInventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ProveedorRepository;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraDTO;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraRequest;
import com.casaverde.backend.fachadaServices.dto.CompraDTO;
import com.casaverde.backend.fachadaServices.dto.CompraRequest;
import com.casaverde.backend.fachadaServices.mapper.AbonoCompraMapper;
import com.casaverde.backend.fachadaServices.mapper.CompraMapper;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;
import com.casaverde.backend.fachadaServices.services.pagoService.ICajaMovimientoService;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements ICompraService {

    private final CompraRepository compraRepo;
    private final ProveedorRepository proveedorRepo;
    private final AbonoCompraRepository abonoRepo;
    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoRepo;
    private final ICajaMovimientoService cajaMovimientoService;
    private final ITasaCambioService tasaCambioService;
    private final IAuditoriaService auditoriaService;
    private final CompraMapper compraMapper;
    private final AbonoCompraMapper abonoMapper;

    @Transactional(readOnly = true)
    public List<CompraDTO> listar(LocalDate fecha, EstadoCompra estado) {
        List<Compra> compras;
        if (fecha != null && estado != null) {
            compras = compraRepo.findByFechaAndEstadoOrderByCreadoEnDesc(fecha, estado);
        } else if (fecha != null) {
            compras = compraRepo.findByFechaOrderByCreadoEnDesc(fecha);
        } else if (estado != null) {
            compras = compraRepo.findByEstadoOrderByCreadoEnDesc(estado);
        } else {
            compras = compraRepo.findAllByOrderByCreadoEnDesc();
        }
        return compras.stream().map(this::enrichCompraDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompraDTO obtener(Long id) {
        Compra compra = compraRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + id));
        return enrichCompraDTO(compra);
    }

    @Transactional
    public CompraDTO registrar(CompraRequest req) {
        var proveedor = proveedorRepo.findById(req.getProveedorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + req.getProveedorId()));
        if (!proveedor.getActivo())
            throw new ValidacionException("El proveedor está inactivo");

        String insumoNorm = req.getInsumo().trim().toLowerCase();
        if (!List.of("gallina", "cuy").contains(insumoNorm))
            throw new ValidacionException("El insumo debe ser 'gallina' o 'cuy'");

        BigDecimal totalCop = req.getCantidad().multiply(req.getPrecioUnitario())
                .setScale(0, RoundingMode.HALF_UP);

        Compra compra = compraRepo.save(Compra.builder()
                .proveedor(proveedor)
                .proveedorNombre(proveedor.getNombre())
                .insumo(insumoNorm)
                .cantidad(req.getCantidad())
                .precioUnitario(req.getPrecioUnitario().setScale(0, RoundingMode.HALF_UP))
                .totalCop(totalCop)
                .estado(EstadoCompra.PENDIENTE)
                .observacion(req.getObservacion())
                .fecha(req.getFecha())
                .build());

        sumarInventario(insumoNorm, req.getCantidad(), compra.getId());
        auditoriaService.registrar("Compra", compra.getId(),
                "REGISTRAR", "Compra de " + req.getCantidad() + " " + insumoNorm
                        + " a " + proveedor.getNombre() + " por $" + totalCop);

        return enrichCompraDTO(compra);
    }

    @Transactional
    public void anular(Long id) {
        Compra compra = compraRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + id));
        if (compra.getEstado() != EstadoCompra.PENDIENTE)
            throw new ValidacionException("Solo se pueden anular compras sin abonos (estado PENDIENTE)");
        if (!abonoRepo.findByCompraIdOrderByCreadoEnDesc(id).isEmpty())
            throw new ValidacionException("No se puede anular una compra que tiene abonos registrados");

        revertirInventario(compra.getInsumo(), compra.getCantidad(), compra.getId());
        auditoriaService.registrar("Compra", compra.getId(), "ANULAR",
                "Compra anulada: " + compra.getCantidad() + " " + compra.getInsumo());
        compraRepo.delete(compra);
    }

    @Transactional
    public AbonoCompraDTO registrarAbono(Long compraId, AbonoCompraRequest req) {
        Compra compra = compraRepo.findById(compraId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + compraId));
        if (compra.getEstado() == EstadoCompra.PAGADO)
            throw new ValidacionException("La compra ya está pagada completamente");

        String moneda = req.getMoneda().trim().toUpperCase();

        BigDecimal tasaUsd = tasaCambioService.getValorVigente();
        BigDecimal montoCop = "USD".equals(moneda)
                ? req.getMonto().multiply(tasaUsd).setScale(0, RoundingMode.HALF_UP)
                : req.getMonto().setScale(0, RoundingMode.HALF_UP);
        BigDecimal tasaUsada = "USD".equals(moneda) ? tasaUsd : null;

        BigDecimal totalAbonado = abonoRepo.sumMontoCopByCompraId(compraId);
        BigDecimal saldoPendiente = compra.getTotalCop().subtract(totalAbonado);
        if (montoCop.compareTo(saldoPendiente) > 0)
            throw new ValidacionException(
                    "El abono ($" + montoCop + ") supera el saldo pendiente ($" + saldoPendiente + ")");

        cajaMovimientoService.registrar(
                CajaMovimientoTipo.COMPRA_PAGO,
                moneda,
                req.getMonto().negate(),
                montoCop.negate(),
                "compra",
                compraId,
                "Pago compra #" + compraId + " (" + compra.getInsumo() + ")",
                null);

        AbonoCompra abono = abonoRepo.save(AbonoCompra.builder()
                .compra(compra)
                .monto(req.getMonto())
                .moneda(moneda)
                .montoCop(montoCop)
                .tasaUsada(tasaUsada)
                .fondoDebitado(moneda)
                .fecha(req.getFecha())
                .build());

        BigDecimal nuevoTotal = totalAbonado.add(montoCop);
        EstadoCompra nuevoEstado = nuevoTotal.compareTo(compra.getTotalCop()) >= 0
                ? EstadoCompra.PAGADO
                : EstadoCompra.ABONADO_PARCIAL;
        compra.setEstado(nuevoEstado);
        compraRepo.save(compra);

        auditoriaService.registrar("AbonoCompra", abono.getId(), "REGISTRAR",
                "Abono $" + req.getMonto() + " " + moneda + " a compra #" + compraId);
        return abonoMapper.toDTO(abono);
    }

    @Transactional(readOnly = true)
    public List<AbonoCompraDTO> listarAbonos(Long compraId) {
        if (!compraRepo.existsById(compraId))
            throw new RecursoNoEncontradoException("Compra no encontrada: " + compraId);
        return abonoRepo.findByCompraIdOrderByCreadoEnDesc(compraId).stream()
                .map(abonoMapper::toDTO)
                .collect(Collectors.toList());
    }

    // --- helpers ---

    private CompraDTO enrichCompraDTO(Compra compra) {
        CompraDTO dto = compraMapper.toDTO(compra);
        BigDecimal totalAbonado = abonoRepo.sumMontoCopByCompraId(compra.getId());
        dto.setTotalAbonado(totalAbonado);
        dto.setSaldoPendiente(compra.getTotalCop().subtract(totalAbonado));
        List<AbonoCompraDTO> abonos = abonoRepo.findByCompraIdOrderByCreadoEnDesc(compra.getId())
                .stream().map(abonoMapper::toDTO).collect(Collectors.toList());
        dto.setAbonos(abonos);
        return dto;
    }

    private void sumarInventario(String insumo, BigDecimal cantidad, Long compraId) {
        Inventario inv = inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new ValidacionException("Insumo no encontrado en inventario: " + insumo));
        BigDecimal nuevo = inv.getStockActual().add(cantidad);
        inv.setStockActual(nuevo);
        inventarioRepo.save(inv);
        movimientoRepo.save(MovimientoInventario.builder()
                .insumo(insumo)
                .tipo("entrada")
                .cantidad(cantidad)
                .stockResultante(nuevo)
                .referenciaTipo("compra")
                .referenciaId(compraId)
                .observacion("Compra #" + compraId)
                .build());
    }

    private void revertirInventario(String insumo, BigDecimal cantidad, Long compraId) {
        Inventario inv = inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new ValidacionException("Insumo no encontrado en inventario: " + insumo));
        BigDecimal nuevo = inv.getStockActual().subtract(cantidad);
        if (nuevo.compareTo(BigDecimal.ZERO) < 0)
            nuevo = BigDecimal.ZERO;
        inv.setStockActual(nuevo);
        inventarioRepo.save(inv);
        movimientoRepo.save(MovimientoInventario.builder()
                .insumo(insumo)
                .tipo("ajuste")
                .cantidad(nuevo)
                .stockResultante(nuevo)
                .referenciaTipo("compra_anulada")
                .referenciaId(compraId)
                .observacion("Anulación compra #" + compraId)
                .build());
    }
}
```

- [ ] **Step 3: Compile check**

```bash
cd Backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/services/compraService/ICompraService.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/services/compraService/CompraServiceImpl.java
git commit -m "feat(tier3): ICompraService + CompraServiceImpl con registrar, anular y registrarAbono"
```

---

## Task 10: CompraController

**File:**
- Create: `Backend/src/main/java/com/casaverde/backend/capaControladores/CompraController.java`

- [ ] **Step 1: Create CompraController**

```java
package com.casaverde.backend.capaControladores;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraDTO;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraRequest;
import com.casaverde.backend.fachadaServices.dto.CompraDTO;
import com.casaverde.backend.fachadaServices.dto.CompraRequest;
import com.casaverde.backend.fachadaServices.services.compraService.ICompraService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
public class CompraController {

    private final ICompraService compraService;

    @GetMapping
    public List<CompraDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) EstadoCompra estado) {
        return compraService.listar(fecha, estado);
    }

    @GetMapping("/{id}")
    public CompraDTO obtener(@PathVariable Long id) {
        return compraService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<CompraDTO> registrar(@RequestBody CompraRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.registrar(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        compraService.anular(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/abonos")
    public List<AbonoCompraDTO> listarAbonos(@PathVariable Long id) {
        return compraService.listarAbonos(id);
    }

    @PostMapping("/{id}/abonos")
    public ResponseEntity<AbonoCompraDTO> registrarAbono(
            @PathVariable Long id,
            @RequestBody AbonoCompraRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.registrarAbono(id, req));
    }
}
```

- [ ] **Step 2: Compile check**

```bash
cd Backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Run existing tests**

```bash
cd Backend && ./mvnw test -q
```

Expected: BUILD SUCCESS. All previously passing tests still pass. No new tests fail.

- [ ] **Step 4: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/capaControladores/CompraController.java
git commit -m "feat(tier3): CompraController — GET/POST/DELETE compras y GET/POST abonos"
```

---

## Task 11: Frontend Services

**Files:**
- Create: `Frontend/src/services/proveedorService.js`
- Create: `Frontend/src/services/compraService.js`

- [ ] **Step 1: Create proveedorService.js**

```js
import api from "./api";

export const proveedorService = {
  listar: (soloActivos = false) =>
    api.get("/proveedores", { params: { soloActivos } }).then((r) => r.data),
  crear: (data) => api.post("/proveedores", data).then((r) => r.data),
  actualizar: (id, data) => api.put(`/proveedores/${id}`, data).then((r) => r.data),
  toggle: (id) => api.put(`/proveedores/${id}/toggle`).then((r) => r.data),
};
```

- [ ] **Step 2: Create compraService.js**

```js
import api from "./api";

export const compraService = {
  listar: (params = {}) => api.get("/compras", { params }).then((r) => r.data),
  obtener: (id) => api.get(`/compras/${id}`).then((r) => r.data),
  registrar: (data) => api.post("/compras", data).then((r) => r.data),
  anular: (id) => api.delete(`/compras/${id}`).then((r) => r.data),
  listarAbonos: (id) => api.get(`/compras/${id}/abonos`).then((r) => r.data),
  registrarAbono: (id, data) =>
    api.post(`/compras/${id}/abonos`, data).then((r) => r.data),
};
```

- [ ] **Step 3: Commit**

```bash
git add Frontend/src/services/proveedorService.js Frontend/src/services/compraService.js
git commit -m "feat(tier3): services proveedorService.js y compraService.js"
```

---

## Task 12: ModalProveedor + ProveedoresTab

**Files:**
- Create: `Frontend/src/components/configuracion/ModalProveedor.jsx`
- Create: `Frontend/src/pages/configuracion/tabs/ProveedoresTab.jsx`

- [ ] **Step 1: Create ModalProveedor.jsx**

```jsx
import { useState } from "react";
import { X } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

const TIPOS = ["CUY", "GALLINA", "AMBOS", "OTROS"];

export default function ModalProveedor({ proveedor, onClose, onSave }) {
  const [form, setForm] = useState({
    nombre: proveedor?.nombre ?? "",
    tipo: proveedor?.tipo ?? "GALLINA",
    telefono: proveedor?.telefono ?? "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.nombre.trim()) { setError("El nombre es obligatorio"); return; }
    setSaving(true);
    try {
      await onSave({
        nombre: form.nombre.trim(),
        tipo: form.tipo,
        telefono: form.telefono.trim() || null,
      });
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
            {proveedor ? "Editar proveedor" : "Nuevo proveedor"}
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
              />
            </div>
            <div className="form-group">
              <label className="form-label">Tipo *</label>
              <select
                className="form-control"
                value={form.tipo}
                onChange={(e) => setForm((f) => ({ ...f, tipo: e.target.value }))}
              >
                {TIPOS.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Teléfono</label>
              <input
                className="form-control"
                value={form.telefono}
                onChange={(e) => setForm((f) => ({ ...f, telefono: e.target.value }))}
                placeholder="Opcional"
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancelar
            </button>
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

- [ ] **Step 2: Create ProveedoresTab.jsx**

```jsx
import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Power } from "lucide-react";
import { proveedorService } from "../../../services/proveedorService";
import ModalProveedor from "../../../components/configuracion/ModalProveedor";

const TIPO_BADGE = {
  CUY:     "badge-success",
  GALLINA: "badge-warning",
  AMBOS:   "badge-info",
  OTROS:   "badge-gray",
};

export default function ProveedoresTab() {
  const qc = useQueryClient();
  const [editando, setEditando] = useState(null);
  const [creando, setCreando] = useState(false);

  const { data: proveedores = [], isLoading } = useQuery({
    queryKey: ["proveedores"],
    queryFn: () => proveedorService.listar(),
  });

  const mutCreate = useMutation({
    mutationFn: proveedorService.crear,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["proveedores"] }),
  });
  const mutUpdate = useMutation({
    mutationFn: ({ id, data }) => proveedorService.actualizar(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["proveedores"] }),
  });
  const mutToggle = useMutation({
    mutationFn: proveedorService.toggle,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["proveedores"] }),
  });

  const handleSave = async (data) => {
    if (editando) await mutUpdate.mutateAsync({ id: editando.id, data });
    else await mutCreate.mutateAsync(data);
  };

  return (
    <div>
      <div className="toolbar">
        <div className="flex-1" />
        <button className="btn btn-primary" onClick={() => setCreando(true)}>
          <Plus size={15} /> Nuevo proveedor
        </button>
      </div>

      <div className="card">
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : proveedores.length === 0 ? (
            <div className="empty-state"><p>Sin proveedores registrados</p></div>
          ) : (
            <table>
              <thead>
                <tr><th>Nombre</th><th>Tipo</th><th>Teléfono</th><th>Estado</th><th>Acciones</th></tr>
              </thead>
              <tbody>
                {proveedores.map((p) => (
                  <tr key={p.id}>
                    <td className="font-medium">{p.nombre}</td>
                    <td><span className={`badge ${TIPO_BADGE[p.tipo] ?? "badge-gray"}`}>{p.tipo}</span></td>
                    <td>{p.telefono ?? "—"}</td>
                    <td>
                      <span className={`badge ${p.activo ? "badge-success" : "badge-danger"}`}>
                        {p.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-1">
                        <button className="btn-icon" title="Editar" onClick={() => setEditando(p)}>
                          <Pencil size={15} />
                        </button>
                        <button
                          className="btn-icon"
                          title={p.activo ? "Desactivar" : "Activar"}
                          onClick={() => mutToggle.mutate(p.id)}
                        >
                          <Power size={15} className={p.activo ? "text-green-600" : "text-red-600"} />
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
        <ModalProveedor
          proveedor={editando}
          onClose={() => { setCreando(false); setEditando(null); }}
          onSave={handleSave}
        />
      )}
    </div>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add Frontend/src/components/configuracion/ModalProveedor.jsx \
        Frontend/src/pages/configuracion/tabs/ProveedoresTab.jsx
git commit -m "feat(tier3): ModalProveedor y ProveedoresTab"
```

---

## Task 13: ModalRegistrarCompra

**File:**
- Create: `Frontend/src/components/configuracion/ModalRegistrarCompra.jsx`

- [ ] **Step 1: Create ModalRegistrarCompra.jsx**

```jsx
import { useState, useMemo } from "react";
import { X } from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { proveedorService } from "../../services/proveedorService";
import { compraService } from "../../services/compraService";
import { formatCOP } from "../../utils/format";

const hoy = () => new Date().toLocaleDateString("en-CA");

export default function ModalRegistrarCompra({ onClose }) {
  const qc = useQueryClient();
  const [form, setForm] = useState({
    proveedorId: "",
    insumo: "gallina",
    cantidad: "",
    precioUnitario: "",
    fecha: hoy(),
    observacion: "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const { data: proveedores = [] } = useQuery({
    queryKey: ["proveedores", true],
    queryFn: () => proveedorService.listar(true),
  });

  const total = useMemo(() => {
    const c = parseFloat(form.cantidad);
    const p = parseFloat(form.precioUnitario);
    return !isNaN(c) && !isNaN(p) ? c * p : 0;
  }, [form.cantidad, form.precioUnitario]);

  const mutRegistrar = useMutation({
    mutationFn: compraService.registrar,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["compras"] });
      onClose();
    },
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.proveedorId) { setError("Selecciona un proveedor"); return; }
    if (!form.cantidad || parseFloat(form.cantidad) <= 0) { setError("La cantidad debe ser mayor a 0"); return; }
    if (!form.precioUnitario || parseFloat(form.precioUnitario) <= 0) { setError("El precio debe ser mayor a 0"); return; }
    setSaving(true);
    try {
      await mutRegistrar.mutateAsync({
        proveedorId: Number(form.proveedorId),
        insumo: form.insumo,
        cantidad: parseFloat(form.cantidad),
        precioUnitario: parseFloat(form.precioUnitario),
        fecha: form.fecha,
        observacion: form.observacion.trim() || null,
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <span className="modal-title">Registrar compra</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group">
              <label className="form-label">Proveedor *</label>
              <select
                className="form-control"
                value={form.proveedorId}
                onChange={(e) => setForm((f) => ({ ...f, proveedorId: e.target.value }))}
              >
                <option value="">Seleccionar…</option>
                {proveedores.map((p) => (
                  <option key={p.id} value={p.id}>{p.nombre}</option>
                ))}
              </select>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Insumo *</label>
                <select
                  className="form-control"
                  value={form.insumo}
                  onChange={(e) => setForm((f) => ({ ...f, insumo: e.target.value }))}
                >
                  <option value="gallina">Gallina</option>
                  <option value="cuy">Cuy</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Fecha *</label>
                <input
                  type="date"
                  className="form-control"
                  value={form.fecha}
                  onChange={(e) => setForm((f) => ({ ...f, fecha: e.target.value }))}
                />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Cantidad *</label>
                <input
                  type="number"
                  className="form-control"
                  min="0.25"
                  step="0.25"
                  value={form.cantidad}
                  onChange={(e) => setForm((f) => ({ ...f, cantidad: e.target.value }))}
                  placeholder="0"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Precio unitario COP *</label>
                <input
                  type="number"
                  className="form-control"
                  min="1"
                  step="1"
                  value={form.precioUnitario}
                  onChange={(e) => setForm((f) => ({ ...f, precioUnitario: e.target.value }))}
                  placeholder="0"
                />
              </div>
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
            <div className="flex justify-between items-center pt-2 border-t border-gray-100">
              <span className="text-sm text-gray-500">Total estimado</span>
              <span className="font-bold text-lg">{formatCOP(total)}</span>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Registrando…" : "Registrar compra"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add Frontend/src/components/configuracion/ModalRegistrarCompra.jsx
git commit -m "feat(tier3): ModalRegistrarCompra con total en tiempo real"
```

---

## Task 14: ModalDetalleCompra

**File:**
- Create: `Frontend/src/components/configuracion/ModalDetalleCompra.jsx`

- [ ] **Step 1: Create ModalDetalleCompra.jsx**

```jsx
import { useState } from "react";
import { X } from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { compraService } from "../../services/compraService";
import { fondoService } from "../../services/fondoService";
import { formatCOP, formatCreadoEn } from "../../utils/format";

const hoy = () => new Date().toLocaleDateString("en-CA");

const ESTADO_BADGE = {
  PENDIENTE:      "badge-gray",
  ABONADO_PARCIAL: "badge-warning",
  PAGADO:         "badge-success",
};

export default function ModalDetalleCompra({ compraId, onClose }) {
  const qc = useQueryClient();
  const [abonoForm, setAbonoForm] = useState({ monto: "", moneda: "COP", fecha: hoy() });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const { data: compra, isLoading } = useQuery({
    queryKey: ["compra", compraId],
    queryFn: () => compraService.obtener(compraId),
  });

  const { data: fondos = [] } = useQuery({
    queryKey: ["fondos"],
    queryFn: fondoService.listar,
  });

  const mutAbono = useMutation({
    mutationFn: (data) => compraService.registrarAbono(compraId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["compra", compraId] });
      qc.invalidateQueries({ queryKey: ["compras"] });
      setAbonoForm({ monto: "", moneda: "COP", fecha: hoy() });
      setError("");
    },
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const handleAbonar = async (e) => {
    e.preventDefault();
    if (!abonoForm.monto || parseFloat(abonoForm.monto) <= 0) {
      setError("El monto debe ser mayor a 0");
      return;
    }
    setSaving(true);
    try {
      await mutAbono.mutateAsync({
        monto: parseFloat(abonoForm.monto),
        moneda: abonoForm.moneda,
        fecha: abonoForm.fecha,
      });
    } finally {
      setSaving(false);
    }
  };

  const totalAbonado = compra?.abonos?.reduce((s, a) => s + Number(a.montoCop), 0) ?? 0;

  return (
    <div className="modal-overlay">
      <div className="modal modal-lg">
        <div className="modal-header">
          <span className="modal-title">Detalle de compra</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="modal-body">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : compra ? (
            <>
              {/* Datos de la compra */}
              <div className="grid grid-cols-2 gap-3 mb-4 text-sm">
                <div><span className="text-gray-500">Proveedor:</span> <strong>{compra.proveedorNombre}</strong></div>
                <div><span className="text-gray-500">Insumo:</span> <strong className="capitalize">{compra.insumo}</strong></div>
                <div><span className="text-gray-500">Cantidad:</span> <strong>{compra.cantidad}</strong></div>
                <div><span className="text-gray-500">Precio unitario:</span> <strong>{formatCOP(compra.precioUnitario)}</strong></div>
                <div><span className="text-gray-500">Total:</span> <strong>{formatCOP(compra.totalCop)}</strong></div>
                <div>
                  <span className="text-gray-500">Estado:</span>{" "}
                  <span className={`badge ${ESTADO_BADGE[compra.estado] ?? "badge-gray"}`}>{compra.estado}</span>
                </div>
                {compra.observacion && (
                  <div className="col-span-2"><span className="text-gray-500">Obs:</span> {compra.observacion}</div>
                )}
              </div>

              {/* Resumen financiero */}
              <div className="flex gap-4 mb-4 text-sm bg-gray-50 rounded p-3">
                <div><span className="text-gray-500">Total abonado:</span> <strong>{formatCOP(compra.totalAbonado)}</strong></div>
                <div><span className="text-gray-500">Saldo pendiente:</span> <strong className="text-amber-700">{formatCOP(compra.saldoPendiente)}</strong></div>
              </div>

              {/* Tabla abonos */}
              {compra.abonos?.length > 0 && (
                <div className="table-wrap mb-4">
                  <table>
                    <thead>
                      <tr><th>Fecha</th><th>Monto</th><th>Moneda</th><th>Total COP</th><th>Fondo</th></tr>
                    </thead>
                    <tbody>
                      {compra.abonos.map((a) => (
                        <tr key={a.id}>
                          <td>{a.fecha}</td>
                          <td>{Number(a.monto).toLocaleString("es-CO")}</td>
                          <td>{a.moneda}</td>
                          <td>{formatCOP(a.montoCop)}</td>
                          <td>{a.fondoDebitado}</td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot>
                      <tr>
                        <td colSpan={3} className="text-right font-medium">Total</td>
                        <td className="font-medium">{formatCOP(totalAbonado)}</td>
                        <td />
                      </tr>
                    </tfoot>
                  </table>
                </div>
              )}

              {/* Formulario abono (si no está PAGADO) */}
              {compra.estado !== "PAGADO" && (
                <form onSubmit={handleAbonar} className="border-t pt-4">
                  <p className="text-sm font-medium mb-3">
                    Registrar abono — saldo pendiente: <strong>{formatCOP(compra.saldoPendiente)}</strong>
                  </p>
                  {error && <div className="alert alert-error mb-3">{error}</div>}
                  <div className="form-row">
                    <div className="form-group">
                      <label className="form-label">Monto *</label>
                      <input
                        type="number"
                        className="form-control"
                        min="1"
                        step="1"
                        value={abonoForm.monto}
                        onChange={(e) => setAbonoForm((f) => ({ ...f, monto: e.target.value }))}
                        placeholder="0"
                      />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Moneda *</label>
                      <select
                        className="form-control"
                        value={abonoForm.moneda}
                        onChange={(e) => setAbonoForm((f) => ({ ...f, moneda: e.target.value }))}
                      >
                        {fondos.map((f) => (
                          <option key={f.moneda} value={f.moneda}>{f.alias ?? f.moneda}</option>
                        ))}
                      </select>
                    </div>
                    <div className="form-group">
                      <label className="form-label">Fecha *</label>
                      <input
                        type="date"
                        className="form-control"
                        value={abonoForm.fecha}
                        onChange={(e) => setAbonoForm((f) => ({ ...f, fecha: e.target.value }))}
                      />
                    </div>
                  </div>
                  <div className="flex justify-end mt-2">
                    <button type="submit" className="btn btn-primary" disabled={saving}>
                      {saving ? "Registrando…" : "Registrar abono"}
                    </button>
                  </div>
                </form>
              )}
            </>
          ) : null}
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>Cerrar</button>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add Frontend/src/components/configuracion/ModalDetalleCompra.jsx
git commit -m "feat(tier3): ModalDetalleCompra con historial de abonos y formulario de pago"
```

---

## Task 15: ComprasTab

**File:**
- Create: `Frontend/src/pages/configuracion/tabs/ComprasTab.jsx`

- [ ] **Step 1: Create ComprasTab.jsx**

```jsx
import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Eye, Trash2 } from "lucide-react";
import { compraService } from "../../../services/compraService";
import ModalRegistrarCompra from "../../../components/configuracion/ModalRegistrarCompra";
import ModalDetalleCompra from "../../../components/configuracion/ModalDetalleCompra";
import ConfirmDialog from "../../../components/shared/ConfirmDialog";
import { formatCOP } from "../../../utils/format";

const hoy = () => new Date().toLocaleDateString("en-CA");

const ESTADO_BADGE = {
  PENDIENTE:       "badge-gray",
  ABONADO_PARCIAL: "badge-warning",
  PAGADO:          "badge-success",
};

export default function ComprasTab() {
  const qc = useQueryClient();
  const [fecha, setFecha] = useState(hoy());
  const [estado, setEstado] = useState("");
  const [registrando, setRegistrando] = useState(false);
  const [detalleId, setDetalleId] = useState(null);
  const [anulando, setAnulando] = useState(null);

  const params = {};
  if (fecha) params.fecha = fecha;
  if (estado) params.estado = estado;

  const { data: compras = [], isLoading } = useQuery({
    queryKey: ["compras", params],
    queryFn: () => compraService.listar(params),
  });

  const mutAnular = useMutation({
    mutationFn: compraService.anular,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["compras"] });
      setAnulando(null);
    },
  });

  return (
    <div>
      <div className="toolbar">
        <div className="search-wrap flex gap-2 items-center">
          <input
            type="date"
            className="form-control"
            style={{ width: 160 }}
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
          />
          <select
            className="form-control"
            style={{ width: 160 }}
            value={estado}
            onChange={(e) => setEstado(e.target.value)}
          >
            <option value="">Todos los estados</option>
            <option value="PENDIENTE">Pendiente</option>
            <option value="ABONADO_PARCIAL">Parcial</option>
            <option value="PAGADO">Pagado</option>
          </select>
        </div>
        <button className="btn btn-primary" onClick={() => setRegistrando(true)}>
          <Plus size={15} /> Registrar compra
        </button>
      </div>

      <div className="card">
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : compras.length === 0 ? (
            <div className="empty-state"><p>Sin compras para los filtros seleccionados</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Proveedor</th>
                  <th>Insumo</th>
                  <th>Cantidad</th>
                  <th>Total COP</th>
                  <th>Abonado</th>
                  <th>Saldo</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {compras.map((c) => (
                  <tr key={c.id}>
                    <td>{c.fecha}</td>
                    <td>{c.proveedorNombre}</td>
                    <td className="capitalize">{c.insumo}</td>
                    <td>{c.cantidad}</td>
                    <td>{formatCOP(c.totalCop)}</td>
                    <td>{formatCOP(c.totalAbonado)}</td>
                    <td className={c.saldoPendiente > 0 ? "text-amber-700 font-medium" : ""}>
                      {formatCOP(c.saldoPendiente)}
                    </td>
                    <td>
                      <span className={`badge ${ESTADO_BADGE[c.estado] ?? "badge-gray"}`}>
                        {c.estado}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-1">
                        <button
                          className="btn-icon"
                          title="Ver detalle"
                          onClick={() => setDetalleId(c.id)}
                        >
                          <Eye size={15} />
                        </button>
                        {c.estado === "PENDIENTE" && (
                          <button
                            className="btn-icon"
                            title="Anular"
                            onClick={() => setAnulando(c)}
                          >
                            <Trash2 size={15} className="text-red-500" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {registrando && <ModalRegistrarCompra onClose={() => setRegistrando(false)} />}

      {detalleId && (
        <ModalDetalleCompra compraId={detalleId} onClose={() => setDetalleId(null)} />
      )}

      {anulando && (
        <ConfirmDialog
          emoji="🗑️"
          titulo="Anular compra"
          descripcion={`¿Anular la compra de ${anulando.cantidad} ${anulando.insumo} a ${anulando.proveedorNombre}? El stock se revertirá.`}
          labelOk="Anular"
          peligro
          onOk={() => mutAnular.mutate(anulando.id)}
          onCancelar={() => setAnulando(null)}
        />
      )}
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add Frontend/src/pages/configuracion/tabs/ComprasTab.jsx
git commit -m "feat(tier3): ComprasTab con filtros fecha/estado, detalle y anulación"
```

---

## Task 16: ConfiguracionPage — Add Tabs

**File:**
- Modify: `Frontend/src/pages/configuracion/ConfiguracionPage.jsx`

- [ ] **Step 1: Add imports and tabs**

Open `Frontend/src/pages/configuracion/ConfiguracionPage.jsx`.

Add two imports at the bottom of the existing import block:
```js
import ProveedoresTab from "./tabs/ProveedoresTab";
import ComprasTab     from "./tabs/ComprasTab";
```

Add two entries to the `TABS` array after `{ id: "reportes", label: "Reportes" }`:
```js
{ id: "proveedores", label: "Proveedores" },
{ id: "compras",     label: "Compras"     },
```

Add two conditional renders after `{tab === "reportes" && <ReportesTab />}`:
```jsx
{tab === "proveedores" && <ProveedoresTab />}
{tab === "compras"     && <ComprasTab     />}
```

The final file should look like:

```jsx
import { useState } from "react";
import ProductosTab  from "./tabs/ProductosTab";
import SalonesTab    from "./tabs/SalonesTab";
import TasaUsdTab    from "./tabs/TasaUsdTab";
import FondosTab     from "./tabs/FondosTab";
import UsuariosTab   from "./tabs/UsuariosTab";
import InventarioTab from "./tabs/InventarioTab";
import CategoriasTab from "./tabs/CategoriasTab";
import AuditoriaTab  from "./tabs/AuditoriaTab";
import BackupsTab    from "./tabs/BackupsTab";
import ReportesTab   from "./tabs/ReportesTab";
import ProveedoresTab from "./tabs/ProveedoresTab";
import ComprasTab     from "./tabs/ComprasTab";

const TABS = [
  { id: "productos",   label: "Productos"   },
  { id: "salones",     label: "Salones"     },
  { id: "tasa",        label: "Tasa USD"    },
  { id: "fondos",      label: "Fondos"      },
  { id: "usuarios",    label: "Usuarios"    },
  { id: "inventario",  label: "Inventario"  },
  { id: "categorias",  label: "Categorías"  },
  { id: "auditoria",   label: "Auditoría"   },
  { id: "backups",     label: "Backups"     },
  { id: "reportes",    label: "Reportes"    },
  { id: "proveedores", label: "Proveedores" },
  { id: "compras",     label: "Compras"     },
];

export default function ConfiguracionPage() {
  const [tab, setTab] = useState("productos");

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Configuración</h1>
      </div>

      <div className="tabs">
        {TABS.map((t) => (
          <button
            key={t.id}
            className={`tab${tab === t.id ? " activo" : ""}`}
            onClick={() => setTab(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === "productos"   && <ProductosTab />}
      {tab === "salones"     && <SalonesTab />}
      {tab === "tasa"        && <TasaUsdTab />}
      {tab === "fondos"      && <FondosTab />}
      {tab === "usuarios"    && <UsuariosTab />}
      {tab === "inventario"  && <InventarioTab />}
      {tab === "categorias"  && <CategoriasTab />}
      {tab === "auditoria"   && <AuditoriaTab  />}
      {tab === "backups"     && <BackupsTab    />}
      {tab === "reportes"    && <ReportesTab   />}
      {tab === "proveedores" && <ProveedoresTab />}
      {tab === "compras"     && <ComprasTab    />}
    </div>
  );
}
```

- [ ] **Step 2: Verify frontend builds without errors**

```bash
cd Frontend && npm run build 2>&1 | tail -20
```

Expected: no TypeScript/import errors. Output ends with `✓ built in` or similar.

- [ ] **Step 3: Commit**

```bash
git add Frontend/src/pages/configuracion/ConfiguracionPage.jsx
git commit -m "feat(tier3): ConfiguracionPage con tabs Proveedores y Compras (12 tabs total)"
```

---

## Task 17: Final Backend Test + Git Merge

- [ ] **Step 1: Run all backend tests**

```bash
cd Backend && ./mvnw test -q
```

Expected: BUILD SUCCESS. All previously passing tests still green.

- [ ] **Step 2: Merge to develop**

```bash
git checkout develop
git merge --no-ff feature/tier3-proveedores-compras -m "feat: Tier 3 — módulo Proveedores y Compras"
git branch -d feature/tier3-proveedores-compras
```

Expected: merge commit created, branch deleted.

---

## Self-Review Checklist

- **Spec § Proveedor CRUD** → Tasks 7 + 8 (IProveedorService, ProveedorServiceImpl, ProveedorController)
- **Spec § Compra registrar** → Task 9 `registrar()` — inventory summed, estado=PENDIENTE
- **Spec § Compra anular** → Task 9 `anular()` — validates PENDIENTE + no abonos, reverts inventory
- **Spec § AbonoCompra registrar** → Task 9 `registrarAbono()` — debits fund via ICajaMovimientoService, updates estado
- **Spec § CompraController** → Task 10 — all 6 endpoints
- **Spec § Frontend services** → Task 11
- **Spec § ProveedoresTab + ModalProveedor** → Task 12
- **Spec § ModalRegistrarCompra** → Task 13
- **Spec § ModalDetalleCompra** → Task 14
- **Spec § ComprasTab** → Task 15
- **Spec § ConfiguracionPage +2 tabs** → Task 16
- **Spec § TipoCajaMovimiento.COMPRA_PAGO** → Task 2 Step 3
- **Spec § totalAbonado/saldoPendiente in service** → Task 9 `enrichCompraDTO()`
- **Spec § mappers don't access repos** → Task 6 — mappers have `ignore = true` for calculated fields
