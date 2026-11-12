# Casa Verde — Documento para sustentar

Resumen de lo esencial para presentar y defender el proyecto. Para el detalle de cada archivo, ver [EXPLICACION_DEL_CODIGO.md](EXPLICACION_DEL_CODIGO.md).

---

## 1. ¿Qué es? (30 segundos)

**Casa Verde** es un sistema de escritorio para operar un asadero (cuyes y gallinas): toma de pedidos, cobro en tres monedas (COP, USD, NEQUI), caja, inventario de insumos, compras a proveedores, empleados, cuentas por cobrar y cierre de caja diario. Funciona 100 % en local con una base de datos SQLite.

**Problema que resuelve:** en un asadero el dinero y el inventario se descuadran fácilmente (se vende media gallina, se paga en dólares, se fía, se cobra por Nequi). El sistema deja **cada peso y cada insumo trazado**.

## 2. Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 17, Spring Boot 3.4, Spring Data JPA/Hibernate, Flyway, MapStruct, Lombok, Spring Security (token propio + BCrypt) |
| Base de datos | SQLite (archivo local) |
| Frontend | React 19, Vite, Tailwind CSS 4, TanStack Query, Zustand, React Router |
| Pruebas | JUnit 5 + Spring Boot Test + ArchUnit (118), Vitest + Testing Library (118) |
| Despliegue | Docker Compose (backend + nginx) o instalador Electron |

## 3. Arquitectura (la parte que más preguntan)

```
Navegador (React)  ──HTTP/JSON──▶  Controller ─▶ IXxxService ─▶ XxxServiceImpl ─▶ Repository ─▶ Entity ─▶ SQLite
   cliente delgado                 (presentación)   (contrato)    (reglas de negocio)  (Spring Data)   (JPA)
```

- **Arquitectura en capas** con dependencias en un solo sentido. Una **prueba de arquitectura (ArchUnit)** falla si alguien salta una capa (p. ej. un controller usando un repository).
- **Interfaz `IXxxService`** entre controller e implementación: bajo acoplamiento y se puede simular (Mockito) en pruebas.
- **DTO + MapStruct:** la API nunca expone las entidades JPA; el contrato de la API es estable.
- **Frontend delgado:** solo muestra datos. Todo cálculo de dinero, estados e inventario ocurre en el backend.

## 4. Decisiones de diseño y su porqué

| Decisión | Porqué (respuesta corta) |
|---|---|
| `BigDecimal` en dinero e inventario | `double` redondea mal; 1/8 de gallina = 0,125 debe ser exacto. |
| El dinero entra a caja **al finalizar**, no al pagar | El pedido se puede editar/anular antes; así no hay que reversar dinero. |
| **Snapshot** en cada ítem (nombre, precio, receta) | Si luego cambia el precio o la receta, el histórico no se altera. |
| `@Version` (bloqueo optimista) | Dos operadores cobrando a la vez no se pisan; el segundo recibe un error 409. |
| `@Transactional` en cada operación crítica | Finalizar un pedido toca pedido + inventario + caja + auditoría: o se guarda todo o nada. |
| Fondos separados por moneda | En la vida real son cajas distintas y se cuadran por separado. |
| Libro de caja inmutable (`CajaMovimiento`) | Cada cambio de saldo queda con su origen y saldo resultante; las correcciones son reversos, no borrados. |
| Flyway + `ddl-auto=validate` | El esquema evoluciona con migraciones versionadas y reproducibles. |
| Token firmado (HMAC-SHA256) de 12 h + BCrypt | Sesión sin estado en servidor; contraseñas nunca en texto plano. |
| SQLite + JPA | Ejecución local sin servidor; JPA permite migrar a PostgreSQL sin cambiar la lógica. |

## 5. Reglas de negocio clave

**Estados del pedido**

```
PENDIENTE → ABONADO_PARCIAL → PAGADO → FINALIZADO
                 └──── FIADO (se sirvió sin pagar todo) ──→ FINALIZADO al saldarse
 (cualquier estado no finalizado) → ANULADO
```

1. **Registrar un pago NO mueve la caja**: solo cambia el estado.
2. **Finalizar** (solo si está PAGADO), en una sola transacción: valida stock → descuenta inventario según la receta del ítem → acredita cada fondo según la moneda pagada → audita.
3. **Fiar**: la comida se sirve, así que el inventario baja ya; la caja no se toca; el saldo queda como deuda en *Cuentas por Cobrar*.
4. **Anular**: solo antes de finalizar. Como la caja no se acreditó, no hay dinero que devolver.
5. **Vueltas:** el cambio al cliente nunca entra a caja (solo se acredita el valor del pedido).
6. **Monedas:** COP y NEQUI sin centavos; USD con 2 decimales, convertido con la tasa vigente, que se guarda en cada pago.
7. **Gasto:** descuenta el fondo (valida saldo) y, si es un insumo, suma stock.
8. **Compra a proveedor:** suma stock al registrarse; los *abonos* son los que debitan el fondo (o consumen un *adelanto* prepagado).
9. **Cierre de caja:** uno por fecha, se bloquea con pedidos activos, compara esperado vs contado y crea backup.
10. **Backup automático** diario a las 00:00 y al arrancar si falta el del día.

## 6. Módulos

| Módulo | Qué hace |
|---|---|
| Pedidos (prioridad #1) | Crear/editar pedido, cobrar en varias monedas, descuento, reserva, fiar, finalizar, anular, comprobante |
| Fondos | Saldos por moneda, libro de caja, entradas, transferencias, salidas (gastos), cuentas NEQUI extra |
| Inventario | Stock de gallina y cuy, historial de movimientos, compras, proveedores, adelantos |
| Resumen y cierre | Resumen del día, exportación CSV, cierre de caja |
| Empleados | Quincenales o por día, adelantos, liquidación, vacaciones |
| Cuentas por cobrar | Fiados y abonos de clientes |
| Configuración | Productos (con receta e historial de precios), categorías, salones, tasa USD, usuarios, auditoría, backups |

## 6.1 Demo sugerida (5 min)

1. **Inventario → Proveedores / Compras:** crear proveedor, comprar 5 gallinas y 5 cuyes.
2. **Pedidos:** crear pedido con "1/2 Gallina" + una gaseosa.
3. **Cobrar:** parte en COP y parte en NEQUI → ver el estado cambiar.
4. **Finalizar** → mostrar que **Fondos** sube y **Inventario** baja 0,5 gallina.
5. **Resumen → Cierre de caja.**
6. **Configuración → Auditoría:** todo quedó registrado.
7. (Opcional) `mvnw test` y mostrar `ArquitecturaCapasTest`.

## 7. Pruebas

| Nivel | Herramienta | Resultado |
|---|---|---|
| Unitarias + integración backend (BD SQLite temporal) | JUnit 5, Spring Boot Test | 99 pruebas, 0 fallos |
| Seguridad (autenticación y roles, seguridad encendida) | Spring Boot Test + MockMvc | 11 pruebas, 0 fallos |
| Contrato de arquitectura | ArchUnit | 8 reglas de dependencia entre capas |
| Frontend | Vitest + Testing Library | 118 pruebas, 0 fallos (verificado) |

## 8. Preguntas probables

- **¿Por qué capas?** Separar responsabilidades: cambiar la BD o la pantalla no toca las reglas de negocio.
- **¿Qué pasa si dos personas cobran a la vez?** `@Version`: el segundo recibe 409 "modificado por otra operación, recarga".
- **¿Y si falla a mitad de finalizar?** La transacción se deshace entera; no queda inventario descontado sin caja acreditada.
- **¿Cómo se agrega un módulo nuevo?** Migración Flyway → entidad + repository → DTO + mapper → interfaz de servicio → implementación → controller → pruebas.
- **¿Escala?** Hoy es monousuario local; capas + JPA permiten pasar a PostgreSQL y cliente-servidor. SQLite es el límite de concurrencia.
- **¿Qué no probaron?** Rendimiento/carga y E2E. Decirlo con honestidad. (La seguridad sí se prueba: 11 pruebas.)
- **¿Usó IA?** Sí; cada integrante debe poder explicar cada parte (ver el otro documento).

## 9. Puntos débiles conocidos (prepara la respuesta)

1. `CLAUDE.md`/`AGENTS.md` decían que los endpoints "no están protegidos"; ya se corrigieron (está en el registro de cambios). Si preguntan: era la documentación la desactualizada, no el código, y ahora `SeguridadEndpointsTest` lo demuestra.
2. `GET /api/v1/usuarios` es público (la pantalla de login lo necesita para listar nombres).
3. El secreto del token por defecto es de desarrollo; en producción se define `APP_SECURITY_TOKEN_SECRET`.
4. El repo tiene pocos commits: tener lista la explicación del historial.
5. Algunas funciones nuevas (FIADO, descuento, caja EFECTIVO) están en el código pero no en el README/`arquitectura.md`.

## 10. Cómo correr en tu máquina (Windows con espacios en el usuario)

Se usan **dos terminales separadas**, en este orden: primero el frontend y después el backend.

**Terminal 1 — Frontend** (en `Frontend/`):
```
npm run dev
```

**Terminal 2 — Backend** (en `Backend/`; el `mvnw.cmd` del repo ya funciona aunque el usuario de Windows tenga espacios):
```
.\mvnw.cmd spring-boot:run
```

Esperar a que el backend muestre `Started CasaVerdeApplication` (o que http://localhost:5020/actuator/health responda `UP`) antes de iniciar sesión.

Abrir http://localhost:5173 — Administrador / `admin123`.
