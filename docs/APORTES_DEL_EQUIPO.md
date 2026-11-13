# Aportes del equipo

Equipo: **Jhon Rivera · Carlos Caicedo · Yonatan Buitron · Lenny Chilito**.

## 1. Cómo nos organizamos

El proyecto es un sistema local para operar un asadero (pedidos, caja en tres monedas, inventario, compras, empleados y cierre diario). En esta etapa **no se agregaron funcionalidades**: el objetivo del curso era **demostrar con evidencias que la arquitectura en capas diseñada antes funciona como se definió**, someterla a pruebas, defender las decisiones y dejar claras las acciones futuras.

Para lograrlo, el trabajo se repartió por **áreas completas de la arquitectura** y no por tareas sueltas. Así cada integrante es responsable de una parte de principio a fin (desde la base de datos hasta la pantalla o la prueba) y puede explicarla sin depender de otro.

| Integrante | Área | Entregable principal |
|---|---|---|
| Jhon Rivera | Coordinación, pedidos, caja y catálogo | Ciclo de vida del pedido, libro de caja, migraciones y despliegue |
| Carlos Caicedo | Inventario, compras, empleados y cierre de caja | Stock, proveedores, nómina, cierre y pruebas de reglas de negocio |
| Yonatan Buitron | Frontend (React) | Pantallas, formularios, manejo de sesión y pruebas de interfaz |
| Lenny Chilito | Validación de la arquitectura | Pruebas de arquitectura y seguridad, manual, informe y replicabilidad |

**Reglas que seguimos todos** (detalladas en el [manual de buenas prácticas](MANUAL_BUENAS_PRACTICAS.md)):
- La lógica de negocio vive solo en el backend; el frontend únicamente muestra y captura datos.
- Todo cálculo de dinero o inventario usa `BigDecimal`, nunca `double`.
- Toda operación crítica es transaccional (`@Transactional`) y queda auditada.
- Las capas se respetan: `Controller → IXxxService → XxxServiceImpl → Repository → Entity`.
- Cada cambio se registra en un commit con mensaje claro y en el [registro de cambios](REGISTRO_DE_CAMBIOS.md).

---

## 2. Jhon Rivera — Coordinación, pedidos, caja y catálogo

### Qué hizo
- **Coordinó el repositorio:** estableció el flujo GitFlow (`main`, `develop` y ramas `feature/*`), creó el repositorio en GitHub y unió el trabajo del equipo mediante pull requests.
- Desarrolló el **ciclo de vida del pedido**: crear, editar ítems, registrar pagos, finalizar, fiar y anular.
- Desarrolló el **libro de caja y los fondos** (COP, USD, NEQUI y la caja física de efectivo), las transferencias entre fondos y los gastos o "salidas de caja".
- Armó el **catálogo**: productos con su receta de inventario, categorías, salones y el historial de cambios de precio.
- Definió el **esquema de la base de datos** con migraciones versionadas (Flyway, V1 a V8) y preparó el **despliegue con Docker Compose** (backend + nginx).

### Cómo lo hizo
- **Arquitectura en capas:** `PedidoController` solo recibe la petición, valida el formato del JSON y delega en la interfaz `IPedidoService`. Las reglas viven en `PedidoServiceImpl`, que a su vez delega los efectos pesados (inventario y caja) en `PedidoFinalizacionServiceImpl`.
- **Máquina de estados del pedido:** `PENDIENTE → ABONADO_PARCIAL → PAGADO → FINALIZADO`, más `FIADO` y `ANULADO`. Los tres primeros estados se calculan solos según lo pagado (`EstadoPedido.calcular`), de modo que nadie los edita a mano.
- **Decisión clave de negocio:** registrar un pago **no mueve la caja**. El dinero entra a los fondos solo al **finalizar**, en una única transacción junto con el descuento de inventario y la auditoría. Así, si un pedido se edita o se anula antes, no hay que reversar dinero y la caja no queda descuadrada.
- **Fiados:** al fiar, la comida ya se sirvió, por eso el inventario baja de inmediato, pero la caja no se toca; el saldo queda como deuda en Cuentas por Cobrar y el pedido se congela (solo admite abonos).
- **Totales calculados al vuelo** (`PedidoTotales`): subtotal, descuento, total, abonado, saldo pendiente y vueltas se calculan en el backend cada vez y no se guardan, para que nunca queden desincronizados.
- **Snapshots:** cada línea del pedido guarda una copia del nombre, el precio y la receta del producto. Si mañana cambia el precio de una gallina, los pedidos anteriores conservan su valor original.
- **Dinero exacto y concurrencia:** `BigDecimal` en todos los montos y `@Version` (bloqueo optimista) en pedidos y fondos, de modo que dos operadores cobrando a la vez no se pisan: el segundo recibe un error 409 y reintenta.
- **Libro de caja inmutable:** `CajaMovimientoServiceImpl` es el único punto por el que cambia un saldo. Cada movimiento guarda su saldo resultante y su origen (`referenciaTipo` + `referenciaId`), y un egreso nunca puede dejar un fondo en negativo. Las correcciones se hacen con movimientos de reverso, no borrando.
- **Monedas:** COP y NEQUI sin centavos, USD con dos decimales; el pago en dólares se convierte con la tasa vigente y esa tasa se guarda en el pago para que el histórico no cambie.

### Dificultades y cómo se resolvieron
- **Pagos en varias monedas:** al finalizar había que repartir el total del pedido entre los fondos sin acreditar las vueltas. Se resolvió agrupando los pagos por moneda y acreditando cada fondo hasta cubrir el total; el excedente nunca entra a caja.
- **Cambios de precio:** se resolvió con los snapshots y el historial de precios, que se crea automáticamente al actualizar un producto.

### Qué puede explicar
Por qué el dinero entra al finalizar y no al pagar, cómo se calculan los totales, cómo se reparten los pagos entre monedas, qué pasa al fiar o anular, cómo se evita el conflicto entre dos usuarios, cómo se versiona el esquema con Flyway y cómo se despliega con Docker.

---

## 3. Carlos Caicedo — Inventario, compras, empleados y cierre de caja

### Qué hizo
- Desarrolló el control de **inventario** de insumos (gallina y cuy) con su historial de movimientos.
- Desarrolló las **compras a proveedores** con abonos, **adelantos** prepagados y el seguimiento de lo que se debe.
- Desarrolló el módulo de **empleados**: nómina quincenal o por día, adelantos, liquidaciones y vacaciones.
- Desarrolló el **cierre de caja** diario, los **backups** automáticos y las exportaciones a CSV.
- Escribió las **pruebas de reglas de negocio** que comprueban que el dinero y el stock cuadran.

### Cómo lo hizo
- **Consumo de insumos:** `InventarioServiceImpl` lee la receta guardada en cada ítem del pedido (por ejemplo, 1/2 gallina = 0,5 gallina), la multiplica por la cantidad vendida y suma por insumo. Los productos sin receta (gaseosa, arroz) se ignoran. Se usa `BigDecimal` para que fracciones como 0,125 sean exactas.
- **Validar antes de escribir:** primero se comprueba que el stock alcance para *todos* los insumos; si falta alguno, se lanza un error y no se descuenta nada. Solo entonces se descuenta y se deja un movimiento de inventario como rastro.
- **Compras:** al registrar una compra se suma el stock; los **abonos** son los que debitan el fondo. Un **adelanto** a proveedor se consume contra la compra sin volver a tocar la caja, y anular una compra solo es posible si no tiene abonos (revierte el stock).
- **Empleados:** un empleado quincenal cobra un salario fijo y uno por día, la tarifa por los días acumulados. Al liquidar se descuentan los adelantos pendientes, se debita el fondo y se marcan adelantos y días como pagados, todo en una transacción.
- **Cierre de caja:** `CierreCajaServiceImpl` permite un solo cierre por fecha, se bloquea si quedan pedidos activos ese día, compara el saldo esperado del sistema con lo contado físicamente (también por denominaciones) y genera un backup de la base de datos.
- **Pruebas:** usan un perfil `test` con una base SQLite temporal, de modo que no tocan los datos reales. Escribió `CasaVerdeBusinessRulesTests` (68 casos de punta a punta) y las pruebas unitarias con Mockito de pedidos, fondos, inventario, compras y empleados.

### Dificultades y cómo se resolvieron
- **Descuadre al anular compras:** si el stock ya se había consumido, anular la compra dejaría el inventario negativo. Se resolvió rechazando la anulación con un mensaje claro, y una prueba verifica que el stock quede intacto.
- **Pruebas independientes:** como todas comparten la base temporal, se cuidó que cada prueba deje sus datos aislados para que el orden de ejecución no cambie el resultado.

### Qué puede explicar
Cómo se calcula el consumo de insumos, por qué no se puede finalizar un pedido sin stock, cómo funcionan abonos y adelantos de proveedores, cómo se liquida a un empleado, qué valida el cierre de caja y cómo están armadas las pruebas de integración.

---

## 4. Yonatan Buitron — Frontend (React)

### Qué hizo
- Construyó la **interfaz de usuario**: pantallas de Pedidos, Fondos, Inventario, Resumen, Empleados, Cuentas por cobrar y Configuración, con sus modales y formularios.
- Implementó la **pantalla de inicio de sesión** y el manejo de la sesión del usuario.
- Escribió las **pruebas del frontend** con Vitest y Testing Library.

### Cómo lo hizo
- **Frontend delgado:** la pantalla solo muestra y captura datos; todos los cálculos de dinero, estados e inventario llegan ya calculados en el DTO del backend. Por ejemplo, el total de un pedido nunca se calcula en React.
- **Lectura y actualización de datos con TanStack Query:** cada pantalla declara qué datos necesita (`useQuery`) y, tras una acción como cobrar o finalizar, se **invalidan** las consultas afectadas, de modo que Pedidos, Fondos, Inventario y Resumen se refrescan solos sin recargar la página.
- **Cliente HTTP único (`api.js`):** un cliente Axios con un interceptor que agrega el token de sesión (`Authorization: Bearer ...`) a cada petición y otro que, si el servidor responde 401, cierra la sesión y vuelve al login. Los mensajes de error del backend se muestran tal cual al operador.
- **Estado global mínimo con Zustand:** solo guarda el usuario que inició sesión y si el menú lateral está colapsado. Los datos del servidor viven en la caché de React Query, no en el estado global.
- **Formularios con react-hook-form + zod:** solo validan el formato y los campos requeridos; las reglas de negocio las valida el backend para no duplicarlas.
- **Diseño consistente:** un sistema de estilos propio sobre Tailwind (botones, tarjetas, modales, etiquetas de estado) y utilidades como `formatCOP` para que el dinero se vea igual en toda la aplicación. Se respetó el diseño de panel de administración: menú lateral, barra superior y contenido operativo.
- **Pruebas:** 118 pruebas en 7 archivos que cubren el formato de moneda y fechas, las reglas de fracciones de productos, la pantalla de login y los modales principales.

### Dificultades y cómo se resolvieron
- **Datos desactualizados entre pantallas:** al finalizar un pedido, Fondos e Inventario mostraban valores viejos. Se resolvió invalidando, desde un solo hook (`usePedidoMutations`), todas las consultas que cambian cuando se finaliza.
- **Rutas en la versión de escritorio:** al correr como archivo local las rutas normales no funcionan, así que se usa un enrutador con `#` cuando la aplicación se empaqueta con Electron.

### Qué puede explicar
Cómo viaja una acción desde el botón hasta el backend y de vuelta, por qué los datos del servidor no se guardan en el estado global, cómo se maneja y se cierra la sesión, cómo se validan los formularios y cómo se prueban los componentes.

---

## 5. Lenny Chilito — Validación de la arquitectura, seguridad y documentación

### Qué hizo
- Diseñó y ejecutó la **validación** de esta etapa: una prueba de contrato de arquitectura, pruebas de seguridad y los dos documentos centrales (manual de buenas prácticas e informe de pruebas).
- Revisó que el repositorio fuera **replicable** por una persona externa, desde un clon limpio.
- Documentó el proyecto: guía de ejecución para el evaluador, explicación del código, registro de cambios y comentarios en español en el código principal.

### Cómo lo hizo
- **Prueba de contrato de arquitectura (`ArquitecturaCapasTest`, ArchUnit, 8 reglas):** verifica que los controllers no usen repositories ni entidades, que los servicios se inyecten por su interfaz, que no haya dependencias hacia capas superiores, que los repositories sean interfaces y que no se use `@Autowired` en campos. Para demostrar que **detecta fallas de verdad**, inyectó a propósito un repository en `PedidoController`: la prueba falló señalando la violación y, al quitarlo, volvió a pasar.
- **Pruebas de seguridad (`SeguridadEndpointsTest`, 11 casos):** con la seguridad activada, como en producción, comprueban que sin sesión o con un token inventado, alterado o vencido la API responde 401; que un OPERADOR recibe 403 en configuración, usuarios y backups; que una clave incorrecta no entrega token; y que la respuesta del login no expone la clave cifrada. Como control, las ejecutó con la seguridad apagada y fallaron 5, como debía ser.
- **Informe de pruebas:** escrito para un lector no técnico, con tablas cortas, **resultados copiados de ejecuciones reales y repetibles** (118 pruebas de backend, 118 de frontend) y una sección de hallazgos honestos: rendimiento y pruebas de extremo a extremo no están cubiertos y quedan como acciones futuras.
- **Manual de buenas prácticas:** reglas del equipo, convenciones de código, ejemplo de capa correcta frente a incorrecta, cómo agregar un módulo sin romper la arquitectura, errores típicos y definición de terminado.
- **Replicabilidad:** clonó el repositorio en una carpeta limpia y siguió el README como lo haría el profesor. Encontró y corrigió tres fallos: `mvnw.cmd` no arrancaba si la carpeta de usuario de Windows tenía espacios, `mvnw` no tenía permiso de ejecución para Linux/macOS, y el recorrido guiado fallaba con una base nueva porque los productos inician con precio $0. También verificó que todo funcione con Docker.
- **Registro:** cada cambio quedó en un commit con mensaje claro, en ramas `feature/*` unidas a `develop` y `main`, y anotado en el registro de cambios.

### Dificultades y cómo se resolvieron
- **Pruebas con la seguridad apagada:** las pruebas de negocio corrían sin autenticación, así que no cubrían los permisos. Se agregó una clase aparte con su propia base temporal y la seguridad encendida.
- **Pruebas que dependen de la hora:** una prueba de fecha de reserva puede fallar si la zona horaria del computador está en el día siguiente a Colombia; se documentó como limitación conocida en la guía de ejecución.

### Qué puede explicar
Qué es una prueba de contrato de arquitectura y por qué se usa, cómo se prueba la seguridad y por qué se hizo aparte, qué cubre y qué no cubre el informe, cómo se reproducen los resultados y cómo se agrega un módulo nuevo sin romper la arquitectura.

---

## 6. Trabajo conjunto

- **Control de versiones:** GitFlow con `main` (estable), `develop` (integración) y ramas `feature/*`. Todo el trabajo de esta etapa pasó por una rama de característica antes de unirse.
- **Registro de cambios:** todo ajuste tiene fecha, motivo y archivos en [REGISTRO_DE_CAMBIOS.md](REGISTRO_DE_CAMBIOS.md).
- **Definición de terminado:** una tarea se considera lista solo si compila, las pruebas pasan (incluida la de arquitectura), respeta las reglas de negocio, valida en el backend, mantiene la trazabilidad de caja, inventario y auditoría, y tiene una prueba que la cubre.
- **Revisión entre integrantes:** como el enunciado exige que cada integrante pueda explicar cualquier parte, cada uno repasó las áreas de los demás con la [explicación del código](EXPLICACION_DEL_CODIGO.md).
- **Uso de IA:** se usaron herramientas de IA como apoyo. La regla del equipo fue que nadie entrega código que no pueda explicar y que los resultados de las pruebas siempre se obtienen ejecutándolas, nunca escribiéndolos a mano.
- **Datos:** todos los datos de prueba y de demostración son ficticios.
