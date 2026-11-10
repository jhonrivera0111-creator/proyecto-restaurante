# Casa Verde — Sistema de operación para asadero

Sistema de escritorio para la operación diaria de un asadero/restaurante: toma de pedidos, cobro en varias monedas, control de caja, inventario de insumos, compras a proveedores, empleados y cierre de caja diario. Funciona 100 % en local (base de datos SQLite, sin servicios en la nube).

| Módulo | Qué permite |
|---|---|
| **Pedidos** | Crear pedidos por mesa/salón o para llevar, agregar ítems, cobrar en COP, USD o NEQUI, finalizar o anular. |
| **Fondos (caja)** | Saldo separado por moneda (COP, USD, NEQUI), libro de caja, entradas, transferencias y salidas (gastos). |
| **Inventario** | Stock de insumos (gallina, cuy), compras a proveedores con abonos y adelantos. Al finalizar un pedido se descuenta el stock automáticamente. |
| **Resumen y cierre** | Resumen del día, exportación CSV y cierre de caja (esperado vs. contado) con backup automático. |
| **Empleados** | Empleados quincenales o por día, adelantos, liquidaciones y vacaciones. |
| **Cuentas por cobrar** | Deudas de clientes ("fiados") y sus abonos. |
| **Configuración** | Productos, categorías, salones, tasa USD, usuarios, auditoría y backups. |

---

## Tabla de contenido

1. [Tecnologías](#tecnologías)
2. [Estructura del repositorio](#estructura-del-repositorio)
3. [Requisitos previos](#requisitos-previos)
4. [Ejecución local (recomendada)](#ejecución-local-recomendada)
5. [Ejecución con Docker (alternativa)](#ejecución-con-docker-alternativa)
6. [Cómo probar el sistema (recorrido guiado)](#cómo-probar-el-sistema-recorrido-guiado)
7. [Pruebas automatizadas](#pruebas-automatizadas)
8. [Documentación](#documentación)
9. [Problemas frecuentes](#problemas-frecuentes)

---

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 17, Spring Boot 3.4, Spring Data JPA / Hibernate, Flyway, MapStruct, Lombok, Spring Security (token propio + BCrypt) |
| Base de datos | SQLite (archivo local, se crea solo al arrancar) |
| Frontend | React 19, Vite 7, Tailwind CSS 4, TanStack Query, Zustand, React Router 7 |
| Pruebas | JUnit 5 + Spring Boot Test (backend), Vitest + Testing Library (frontend) |
| Despliegue opcional | Docker Compose (backend + nginx), Electron (instalador de escritorio) |

---

## Estructura del repositorio

```
Casa-Verde/
├── README.md                 ← este archivo
├── docs/                     ← documentación del proyecto (ver sección Documentación)
├── docker-compose.yml        ← despliegue con Docker (backend + frontend nginx)
├── .env.example              ← variables de entorno para Docker
│
├── Backend/                  ← API REST Spring Boot (puerto 5020)
│   ├── mvnw / mvnw.cmd       ← Maven Wrapper: no hace falta instalar Maven
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/casaverde/backend/
│       │   ├── capaControladores/   ← @RestController (endpoints /api/v1/...)
│       │   ├── fachadaServices/     ← interfaces de servicio, implementaciones, DTOs, mappers
│       │   ├── capaAccesoADatos/    ← entidades JPA, enums y repositorios
│       │   └── config/              ← seguridad, CORS, datos iniciales, manejo de errores
│       ├── main/resources/
│       │   ├── application.properties
│       │   └── db/migration/        ← migraciones Flyway (V1..V8)
│       └── test/                    ← pruebas JUnit
│
└── Frontend/                 ← SPA React (puerto 5173 en desarrollo)
    ├── package.json
    ├── vite.config.js        ← proxy /api → http://localhost:5020
    ├── src/
    │   ├── pages/            ← una carpeta por pantalla (pedidos, fondos, inventario…)
    │   ├── components/       ← modales y componentes reutilizables
    │   ├── services/         ← clientes HTTP (Axios) por recurso
    │   ├── hooks/ store/ utils/
    │   └── **/__tests__/     ← pruebas Vitest
    └── electron/             ← empaquetado de escritorio (opcional)
```

Arquitectura en capas del backend: `Controller → Interfaz de servicio (IXxxService) → Implementación → Repository → Entidad`. Más detalle en [docs/arquitectura.md](docs/arquitectura.md).

---

## Requisitos previos

| Herramienta | Versión | Verificar con |
|---|---|---|
| Java JDK | **17** | `java -version` |
| Node.js | **20 o superior** (probado con 22) | `node -v` |
| npm | viene con Node | `npm -v` |
| Git | cualquiera | `git --version` |

**No hace falta instalar Maven** (el proyecto trae `mvnw`) **ni una base de datos** (SQLite se crea sola).

---

## Ejecución local (recomendada)

Se necesitan **dos terminales**: una para el backend y otra para el frontend.

### 1. Clonar el repositorio

```bash
git clone https://github.com/Harold-V/Casa-Verde.git
cd Casa-Verde
```

### 2. Terminal 1 — Backend

```bash
cd Backend

# Windows (cmd / PowerShell)
mvnw.cmd spring-boot:run

# Linux / macOS / Git Bash
./mvnw spring-boot:run
```

La primera vez descarga Maven y las dependencias (varios minutos). Está listo cuando la consola muestra:

```
Started CasaVerdeApplication in X seconds
```

Al arrancar por primera vez el backend:
- crea la base de datos en `Backend/DataBase/casaverde.db` y aplica las migraciones Flyway;
- siembra datos iniciales: categorías, 17 productos, fondos COP/USD/NEQUI, tasa USD = 4200 COP, inventario en 0 y el usuario **ADMIN**.

Verificación rápida: abrir <http://localhost:5020/actuator/health> → debe responder `{"status":"UP"}`.

### 3. Terminal 2 — Frontend

```bash
cd Frontend
npm install        # solo la primera vez
npm run dev
```

Abrir **<http://localhost:5173>** en el navegador.

> Vite redirige las llamadas `/api/...` al backend en `localhost:5020`, así que el backend debe estar corriendo antes de usar la app.

### 4. Iniciar sesión

| Usuario | Contraseña |
|---|---|
| ADMIN | `admin123` |

Los usuarios con rol OPERADOR se crean desde **Configuración → Usuarios** y no requieren contraseña.

### Reiniciar los datos

Para volver a un estado limpio: detener el backend y borrar la carpeta `Backend/DataBase/`. Al volver a arrancar se recrea con los datos iniciales.

---

## Ejecución con Docker (alternativa)

Requiere Docker Desktop. Desde la raíz del repositorio:

```bash
cp .env.example .env      # en Windows: copy .env.example .env
docker compose up --build
```

- App: <http://localhost> (nginx sirve el frontend y redirige `/api` al backend)
- API: <http://localhost:5020>
- Usuario: ADMIN con la contraseña definida en `APP_ADMIN_PASSWORD` del archivo `.env`.

Los datos se guardan en volúmenes de Docker (`casaverde_db`, `casaverde_backups`). Para borrarlos: `docker compose down -v`.

---

## Cómo probar el sistema (recorrido guiado)

Recorrido de ~10 minutos que ejercita el flujo principal (pedido → cobro → caja → inventario → cierre):

1. **Cargar inventario.** *Inventario → Proveedores → Nuevo proveedor*. Luego *Inventario → Compras → Registrar compra*: comprar, por ejemplo, 5 gallinas y 5 cuyes. El stock sube en la pestaña *Stock*.
2. **Crear un pedido.** *Pedidos → Nuevo Pedido*: agregar "1/2 Gallina" y "Gaseosa". El total lo calcula el backend.
3. **Cobrar.** Abrir el pedido → *Cobrar*: registrar un pago parcial en COP y el resto en NEQUI. El estado pasa de `PENDIENTE` → `ABONADO_PARCIAL` → `PAGADO`.
4. **Finalizar.** *Finalizar pedido*. En ese momento:
   - *Fondos*: los fondos COP y NEQUI aumentan (el dinero solo entra a caja al finalizar, no al registrar el pago);
   - *Inventario*: la gallina baja 0,5.
5. **Registrar una salida.** *Fondos → Nueva salida*: un gasto de servicios. El fondo baja y queda en el libro de caja.
6. **Cerrar caja.** *Resumen → Cierre de caja*: ingresar lo contado y *Cerrar caja*. El cierre se bloquea si quedan pedidos abiertos y, al cerrar, genera un backup.
7. **Revisar trazabilidad.** *Configuración → Auditoría* muestra cada operación registrada.

También se puede probar la API directamente desde **Swagger UI**: <http://localhost:5020/swagger-ui.html>. Para los endpoints protegidos, primero llamar a `POST /api/v1/sesion/login` y usar el token devuelto en el botón *Authorize*.

---

## Pruebas automatizadas

### Backend (JUnit 5 + Spring Boot Test)

```bash
cd Backend
mvnw.cmd test        # Windows
./mvnw test          # Linux / macOS / Git Bash
```

Resultado esperado: `Tests run: 99, Failures: 0, Errors: 0`. Las pruebas usan el perfil `test` con una base de datos SQLite temporal en `Backend/target/test-data/`; no tocan los datos de desarrollo. Cubren:

| Clase de prueba | Qué valida |
|---|---|
| `PedidoServiceTest` | Ciclo de vida del pedido, pagos, estados, finalización y anulación |
| `CasaVerdeBusinessRulesTests` | Reglas de negocio de punta a punta (caja, inventario, cierre) |
| `FondoServiceTest` | Ajustes, entradas y transferencias entre fondos |
| `InventarioServiceTest` | Movimientos de stock y validaciones |
| `CompraServiceTest` | Compras a proveedor, abonos y adelantos |
| `EmpleadoServiceTest` | Adelantos, días trabajados y liquidaciones |
| `CrearPedidoRequestValidationTest` | Validación de datos de entrada |
| `CasaVerdeApplicationTests` | Que el contexto de Spring arranca |

Reporte detallado: `Backend/target/surefire-reports/`.

### Frontend (Vitest + Testing Library)

```bash
cd Frontend
npm run test:run        # ejecuta una vez
npm test                # modo observación (re-ejecuta al guardar)
npm run test:coverage   # con reporte de cobertura → Frontend/coverage/index.html
```

Resultado esperado: `Test Files 7 passed`, `Tests 118 passed`. Cubren utilidades de formato de dinero y fechas, reglas de fracciones de producto, pantalla de login y modales principales.

---

## Documentación

| Documento | Contenido |
|---|---|
| [docs/arquitectura.md](docs/arquitectura.md) | Arquitectura, capas, modelo de datos y reglas de negocio |
| [CLAUDE.md](CLAUDE.md) | Referencia técnica completa: contrato de todos los endpoints REST, entidades, convenciones de código y flujo Git |
| Swagger UI (con el backend corriendo) | <http://localhost:5020/swagger-ui.html> — documentación interactiva de la API |
| OpenAPI JSON | <http://localhost:5020/v3/api-docs> |

---

## Problemas frecuentes

| Síntoma | Causa / solución |
|---|---|
| `npm error enoent Could not read package.json` | `npm` se ejecutó fuera de `Frontend/`. Hacer `cd Frontend` primero. El backend **no** usa npm. |
| `'mvn' no se reconoce como comando` | Usar el wrapper incluido: `mvnw.cmd` (Windows) o `./mvnw`. |
| `Port 5020 was already in use` / `Port 5173 is already in use` | Otra instancia sigue corriendo (o el contenedor Docker). Cerrarla o `docker compose down`. |
| La app abre pero muestra "Error de conexión" o no lista usuarios | El backend no está corriendo o aún no termina de arrancar. |
| `invalid target release: 17` | La variable `JAVA_HOME` apunta a una versión de Java distinta de la 17. |
| En Docker no se puede iniciar sesión como ADMIN | Falta el archivo `.env` con `APP_ADMIN_PASSWORD` (ver `.env.example`). |
