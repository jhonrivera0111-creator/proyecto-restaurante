# Manual de buenas prácticas del equipo — Casa Verde

Reglas que el equipo sigue para que el sistema siga siendo ordenado, confiable (maneja dinero e inventario) y fácil de mantener. Complementa [arquitectura.md](arquitectura.md) y [CLAUDE.md](../CLAUDE.md).

## 1. Principios

1. **El backend manda.** Todo cálculo de dinero, estados e inventario se hace en el backend; el frontend solo muestra y captura.
2. **Dinero siempre con `BigDecimal`.** Nunca `double`/`float`.
3. **Toda operación crítica es transaccional (`@Transactional`) y queda auditada** (`auditoriaService.registrar(...)`).
4. **Nada de caja o inventario se borra.** Las correcciones se hacen con movimientos de reverso.
5. **Los fondos COP, USD y NEQUI son independientes.**

## 2. Arquitectura en capas (la regla que no se rompe)

```
Controller → IXxxService → XxxServiceImpl → Repository → Entity
```

| Regla | Verificada automáticamente |
|---|---|
| Un controller nunca usa un repository, una entidad ni una `...Impl` | Sí — `ArquitecturaCapasTest` |
| Un servicio nunca depende de un controller | Sí |
| La capa de datos no conoce servicios ni controllers | Sí |
| Repositories son interfaces, sin `@Repository` | Sí |
| Sin `@Autowired` en campos (inyección por constructor con `@RequiredArgsConstructor`) | Sí |
| La API devuelve DTOs, nunca entidades JPA | Sí (controllers no importan entidades) |

## 3. Convenciones de código

**Backend**
- Paquete raíz `com.casaverde.backend`: `capaControladores`, `fachadaServices` (`services`, `dto`, `mapper`), `capaAccesoADatos` (`models`, `repositories`), `config`.
- Servicios: interfaz `IXxxService` + `XxxServiceImpl`. **La interfaz se crea primero.**
- Mappers: interfaces MapStruct con `@Mapper(componentModel = "spring")`.
- Entidades con concurrencia: `@Version` (bloqueo optimista). Fechas en `America/Bogota`.
- Errores de negocio: `ValidacionException` / `RecursoNoEncontradoException`, manejados por `GlobalExceptionHandler`.
- Cambios de esquema: **solo** con una migración Flyway nueva (`V9__...sql`). Nunca editar una migración ya aplicada.

**Frontend**
- Datos del servidor con TanStack Query (no `useEffect` + `useState`). Tras cada mutación, `invalidateQueries`.
- Formularios con react-hook-form + zod (solo validación de formato).
- Dinero formateado con `formatCOP` de `utils/format.js`.
- Estilos con Tailwind / clases de `index.css`; no se crean archivos CSS nuevos.

**Nombres:** clases y archivos en español, como el dominio (`Pedido`, `Fondo`, `CierreCaja`). Mensajes de commit en español, estilo `tipo: descripción` (`feat:`, `fix:`, `docs:`, `test:`).

**Comentarios:** cada clase de servicio lleva un bloque `/** ... */` que explica *qué regla de negocio implementa y por qué*; los métodos públicos, una línea con su efecto (en especial si toca caja o inventario). Se comenta el **porqué**, no lo obvio.

### 3.1 Ejemplo: capa correcta vs. incorrecta

```java
// ✅ Correcto: el controller solo delega en la interfaz del servicio
@RestController
@RequiredArgsConstructor
public class PedidoController {
    private final IPedidoService service;
    @PutMapping("/{id}/finalizar")
    public PedidoDTO finalizar(@PathVariable Long id) { return service.finalizarPedido(id); }
}

// ❌ Incorrecto: el controller habla con la base de datos y devuelve la entidad
public class PedidoController {
    private final PedidoRepository repo;                 // salta la capa de servicios
    public Pedido obtener(Long id) { return repo.findById(id).get(); }  // expone la entidad JPA
}
```

El segundo ejemplo **no compila como "terminado"**: `ArquitecturaCapasTest` lo detecta y falla (se demostró inyectando un repository en `PedidoController`: 1 fallo, *"los controllers solo delegan en la capa de servicios"*).

### 3.2 Reglas para operaciones que tocan dinero o inventario

1. El método del servicio lleva `@Transactional`: si algo falla, no queda nada a medias.
2. El saldo de un fondo **solo** cambia a través de `ICajaMovimientoService.registrar(...)`, que deja un `CajaMovimiento` con saldo resultante y origen (`referenciaTipo` + `referenciaId`). Nunca `fondo.setSaldo(...)` desde otro servicio.
3. El stock **solo** cambia a través de `IInventarioService`, que deja un `MovimientoInventario`.
4. Se valida antes de escribir: saldo suficiente, stock suficiente, estado permitido. Si no se cumple, se lanza `ValidacionException` con un mensaje que entienda el operador.
5. Se registra auditoría: `auditoriaService.registrar("pedido", id, "FINALIZAR", "...")`.
6. Los montos se normalizan con `IMonetaryService` (COP/NEQUI sin decimales, USD con 2) y las conversiones USD→COP guardan la tasa usada.

### 3.3 Prácticas de pruebas

- **Pirámide:** muchas unitarias (servicios con Mockito, rápidas) → algunas de integración (Spring Boot + SQLite temporal, `CasaVerdeBusinessRulesTests`) → reglas de arquitectura (ArchUnit) → frontend (Vitest).
- **Nombre que se lea como regla de negocio:** `finalizarPedido_sinStock_lanzaValidacion`, no `test1`.
- **Cada error corregido deja una prueba** que lo reproduce, para que no vuelva.
- Las pruebas usan el perfil `test` y una base temporal en `Backend/target/test-data/`: **nunca** tocan la base de datos real.
- **Datos ficticios** siempre ("Cliente Prueba", "3000000000").
- Los resultados que se reportan se copian de la salida real del comando (`Tests run: ..., Failures: ...`), con fecha.

### 3.4 Seguridad

- Contraseñas solo con BCrypt (`PasswordEncoder`); nunca se registran en logs ni se devuelven en un DTO.
- Toda ruta nueva de `/api/v1/**` queda protegida por defecto; si debe ser pública o solo-ADMIN se agrega explícitamente en `SecurityConfig` **y** una prueba en `SeguridadEndpointsTest`.
- Secretos (token, contraseña inicial) por variable de entorno (`.env`, ignorado por git). `.env.example` solo lleva valores de ejemplo.

## 4. Flujo de trabajo en Git (GitFlow)

- `main`: estable, solo recibe `release/*` y `hotfix/*`. `develop`: integración.
- Todo trabajo nuevo en `feature/nombre-corto` que sale de `develop`, y vuelve con merge `--no-ff`.
- Nunca commits directos a `main`. Commits pequeños y con mensaje claro.
- Todo ajuste a la arquitectura o a las pruebas se registra en el commit y en [docs/REGISTRO_DE_CAMBIOS.md](REGISTRO_DE_CAMBIOS.md).

## 5. Cómo agregar un módulo nuevo sin romper la arquitectura

Ejemplo: agregar "Reservas".

1. **Migración:** `V9__reservas.sql` con la tabla.
2. **Entidad** en `capaAccesoADatos/models/entitys/Reserva.java` y **repository** `ReservaRepository extends JpaRepository`.
3. **DTOs** (`ReservaDTO`, `ReservaRequest` con validaciones `@Valid`) y **mapper** MapStruct.
4. **Interfaz** `IReservaService` y luego `ReservaServiceImpl` con `@Transactional` + auditoría.
5. **Controller** `ReservaController` que inyecta **solo** `IReservaService`.
6. **Seguridad:** decidir quién puede usarlo (autenticado u solo ADMIN) y, si no es lo por defecto, ajustar `SecurityConfig` y su prueba.
7. **Pruebas:** una unitaria del servicio (Mockito) mínimo; si toca caja o inventario, también un caso en `CasaVerdeBusinessRulesTests`.
8. Correr `./mvnw test`: si `ArquitecturaCapasTest` falla, algo quedó en la capa equivocada.
9. **Frontend:** `services/reservaService.js`, página en `pages/`, modales en `components/`, ruta en `App.jsx`, enlace en `Sidebar.jsx`.
10. Documentar el endpoint en `CLAUDE.md` y el cambio en `docs/REGISTRO_DE_CAMBIOS.md`.

**Errores típicos que rompen la arquitectura (y cómo evitarlos)**

| Tentación | Qué pasa | Qué hacer |
|---|---|---|
| "Es más rápido usar el repository desde el controller" | Falla `ArquitecturaCapasTest` | Crear el método en el servicio |
| Devolver la entidad JPA en la API | Falla la regla de entidades; se filtran campos internos | Crear un DTO y su mapper |
| Inyectar `XxxServiceImpl` | Falla la regla de interfaces; acoplamiento | Inyectar `IXxxService` |
| Calcular el total en React | Totales distintos entre pantallas | El total viene en el DTO desde el backend |
| Editar una migración ya aplicada | Flyway rechaza el arranque (checksum) | Crear `V(n+1)__...sql` |
| `@Autowired` en un campo | Falla la regla de inyección | `private final` + `@RequiredArgsConstructor` |

**Agregar un patrón nuevo:** se discute primero, se anota la decisión (qué y por qué) en `docs/arquitectura.md` → "Decisiones de diseño", y si es una regla verificable se agrega a `ArquitecturaCapasTest`.

## 6. Definición de terminado

Una tarea está lista solo si:

- [ ] Compila sin errores.
- [ ] `./mvnw test` y `npm run test:run` pasan (incluida la prueba de arquitectura).
- [ ] Las reglas de negocio de `docs/arquitectura.md` §5 siguen cumpliéndose.
- [ ] Tiene validaciones en backend y, si toca dinero/inventario, es transaccional y auditada.
- [ ] Tiene al menos una prueba que cubre el cambio.
- [ ] No hay datos reales de personas: solo datos ficticios.
- [ ] README/CLAUDE.md/docs actualizados si cambia un endpoint, pantalla o regla.
- [ ] Revisado por otro integrante, y cada integrante puede explicar el cambio.
- [ ] Registrado: commit con mensaje claro en una rama `feature/*` y fila en `REGISTRO_DE_CAMBIOS.md` si afecta arquitectura o pruebas.

**Revisión entre integrantes (preguntas que hace quien revisa)**
1. ¿Está en la capa correcta? ¿Pasa la prueba de arquitectura?
2. Si toca dinero o inventario: ¿es transaccional, deja movimiento trazable y auditoría?
3. ¿Qué pasa si la operación se ejecuta dos veces o por dos usuarios a la vez? (`@Version`)
4. ¿Hay una prueba que falle si alguien rompe esta regla mañana?
5. ¿El mensaje de error lo entiende el operador del asadero?

## 7. Uso de IA

Se permite usar IA. Regla del equipo: **nadie entrega código que no pueda explicar**, y los resultados de pruebas siempre se obtienen ejecutándolas, nunca se escriben a mano.
