# Guía de ejecución para el evaluador

Pasos para clonar, ejecutar y probar **Casa Verde** sin ayuda del equipo. Hay dos formas de correr la aplicación (elige **una**) y una sección de pruebas automáticas.

| Qué quiere hacer | Sección | Tiempo aprox. |
|---|---|---|
| Ver la aplicación funcionando (recomendado) | [A. Ejecución local](#a-ejecución-local-recomendada) | 5–10 min la primera vez |
| Verla con Docker (bonificable) | [B. Ejecución con Docker](#b-ejecución-con-docker-opcional) | 5–10 min la primera vez |
| Verificar las pruebas automáticas | [C. Pruebas](#c-pruebas-automáticas) | 3–5 min |
| Recorrer el sistema como usuario | [D. Recorrido guiado](#d-recorrido-guiado-del-sistema) | 10 min |

---

## 0. Requisitos

| Herramienta | Versión | Comprobar con |
|---|---|---|
| Git | cualquiera | `git --version` |
| Java JDK | **17** (exactamente; ver nota) | `java -version` |
| Node.js | **20 o superior** | `node -v` |
| Docker Desktop | solo para la sección B | `docker --version` |

- **No hace falta instalar Maven** (el proyecto trae `mvnw`) **ni una base de datos** (SQLite se crea sola).
- Se necesita **internet la primera vez** (descarga de dependencias de Maven y npm).
- **Java:** el proyecto está configurado y probado con **Java 17**. Con otra versión de Java puede fallar; si `java -version` no muestra 17, instalar un JDK 17 (por ejemplo Temurin o Microsoft OpenJDK 17) y apuntar `JAVA_HOME` a él.
- **Puertos libres:** `5173` (frontend), `5020` (backend); `80` solo si se usa Docker.

## 1. Obtener el código

```bash
git clone https://github.com/jhonrivera0111-creator/proyecto-restaurante.git
cd proyecto-restaurante
```

La rama por defecto (`main`) ya contiene todo lo entregado.

---

## A. Ejecución local (recomendada)

Se usan **dos terminales separadas**, **en este orden: primero el frontend y después el backend**.

### Terminal 1 — Frontend

```bash
cd Frontend
npm install
npm run dev
```

Debe mostrar `Local: http://localhost:5173/`. Dejar la terminal abierta. (Todavía no se puede iniciar sesión: falta el backend.)

### Terminal 2 — Backend (otra terminal aparte)

**Windows (PowerShell o cmd):**
```bash
cd Backend
.\mvnw.cmd spring-boot:run
```

**Linux / macOS / Git Bash:**
```bash
cd Backend
./mvnw spring-boot:run
```

La primera vez descarga Maven y las dependencias (varios minutos). Está listo cuando la consola muestra:

```
Started CasaVerdeApplication in X seconds
```

### Comprobar que está arriba

| Qué | Dirección | Resultado esperado |
|---|---|---|
| Backend | <http://localhost:5020/actuator/health> | `{"status":"UP"}` |
| Aplicación | <http://localhost:5173> | Pantalla de inicio de sesión de Casa Verde |
| Documentación de la API | <http://localhost:5020/swagger-ui.html> | Swagger UI |

### Iniciar sesión

| Usuario | Contraseña |
|---|---|
| Administrador (ADMIN) | `admin123` |

Los usuarios con rol OPERADOR se crean en *Configuración → Usuarios* y no requieren contraseña.

### Detener y reiniciar los datos

- Detener: `Ctrl + C` en cada terminal.
- Volver a datos limpios: detener el backend y borrar la carpeta `Backend/DataBase/`; al arrancar de nuevo se recrea con los datos iniciales (en ese caso hay que repetir el paso de precios del recorrido guiado).

---

## B. Ejecución con Docker (opcional)

Requiere Docker Desktop abierto (ícono de la ballena en *Engine running*). **No usar al mismo tiempo que la ejecución local**: comparten el puerto 5020.

Desde la raíz del repositorio:

```bash
cp .env.example .env          # Windows cmd/PowerShell: copy .env.example .env
docker compose up --build
```

La primera vez tarda varios minutos (descarga imágenes y compila). Cuando ambos contenedores estén *healthy*:

| Qué | Dirección |
|---|---|
| Aplicación (nginx + frontend) | <http://localhost> |
| API / Swagger | <http://localhost:5020/swagger-ui.html> |

Usuario: **Administrador**, contraseña la definida en `APP_ADMIN_PASSWORD` del archivo `.env` (en `.env.example` es `admin123`).

Para detener: `Ctrl + C` y luego `docker compose down`. Para **borrar también los datos**: `docker compose down -v`.

---

## C. Pruebas automáticas

Las pruebas no necesitan que la aplicación esté corriendo y usan una base de datos temporal (no tocan los datos reales).

### Backend (JUnit 5 + Spring Boot Test + ArchUnit)

```bash
cd Backend
.\mvnw.cmd test        # Windows
./mvnw test            # Linux / macOS / Git Bash
```

**Resultado esperado:**

```
Tests run: 118, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Grupo | Pruebas | Qué valida |
|---|---|---|
| Reglas de negocio y servicios | 99 | Pedidos, pagos, caja, inventario, compras, empleados, cierre |
| `ArquitecturaCapasTest` | 8 | **Contrato de arquitectura (ArchUnit):** controllers sin repositories, servicios por interfaz, capas sin dependencias hacia arriba |
| `SeguridadEndpointsTest` | 11 | Con la seguridad activada: 401 sin sesión o con token falso/alterado/vencido, 403 de OPERADOR en rutas de ADMIN |

Para correr solo una clase: `.\mvnw.cmd test -Dtest=ArquitecturaCapasTest`. Los reportes quedan en `Backend/target/surefire-reports/`.

### Frontend (Vitest + Testing Library)

```bash
cd Frontend
npm install        # si no se hizo antes
npm run test:run
```

**Resultado esperado:** `Test Files 7 passed`, `Tests 118 passed`.

### Demostrar que la prueba de arquitectura detecta violaciones

1. Abrir `Backend/src/main/java/com/casaverde/backend/capaControladores/PedidoController.java`.
2. Debajo de `private final IPedidoService service;` agregar:
   ```java
   private final com.casaverde.backend.capaAccesoADatos.repositories.PedidoRepository repo;
   ```
3. Ejecutar `.\mvnw.cmd test -Dtest=ArquitecturaCapasTest` → **falla** (`Tests run: 8, Failures: 1`, *"los controllers solo delegan en la capa de servicios"*).
4. **Quitar la línea agregada** y volver a ejecutar → pasa (8 de 8).

---

## D. Recorrido guiado del sistema

Con la aplicación corriendo (sección A o B) e iniciada la sesión como Administrador. Ejercita el flujo principal: pedido → cobro → caja → inventario → cierre.

0. **Poner precios (obligatorio la primera vez).** El catálogo inicial se crea con precio **$0** a propósito, y un pedido de $0 no se puede cobrar ni finalizar. En *Configuración → Productos* hacer clic en el precio de **"1/2 Gallina"** y de **"Gaseosa"** y asignar, por ejemplo, 28.000 y 4.000.
1. **Cargar inventario.** *Inventario → Proveedores → Nuevo proveedor* (datos ficticios). Luego *Inventario → Compras → Registrar compra*: 5 gallinas y 5 cuyes. El stock sube en la pestaña *Stock*.
2. **Crear un pedido.** *Pedidos → Nuevo Pedido*: agregar "1/2 Gallina" y "Gaseosa". El total (32.000) lo calcula el backend.
3. **Cobrar.** Abrir el pedido → *Cobrar*: una parte en COP (p. ej. 20.000) y el resto en NEQUI. El estado pasa de `PENDIENTE` → `ABONADO_PARCIAL` → `PAGADO`. **Comprobar en *Fondos* que todavía no cambió nada**: el dinero no entra a caja al pagar.
4. **Finalizar pedido.** Ahora sí:
   - *Fondos:* suben Efectivo (+20.000) y NEQUI (+12.000).
   - *Inventario:* la gallina baja 0,5.
5. **Registrar una salida.** *Fondos → Nueva salida*: un gasto de servicios. El fondo baja y queda en el libro de caja.
6. **Cerrar caja.** *Resumen → Cierre de caja*: ingresar lo contado y cerrar. Se bloquea si quedan pedidos abiertos y, al cerrar, genera un backup.
7. **Revisar trazabilidad.** *Configuración → Auditoría* muestra cada operación; *Resumen* permite exportar CSV.

Regla clave a observar: **registrar un pago no mueve la caja; solo al finalizar**, y finalizar descuenta el inventario y acredita los fondos en una sola transacción (si falta stock, no se finaliza).

---

## E. Dónde está la documentación

| Documento | Contenido |
|---|---|
| [README.md](../README.md) | Qué hace el sistema, estructura y cómo ejecutarlo |
| [MANUAL_BUENAS_PRACTICAS.md](MANUAL_BUENAS_PRACTICAS.md) | Entregable 2: reglas, convenciones, cómo agregar un módulo, definición de terminado |
| [INFORME_PRUEBAS.md](INFORME_PRUEBAS.md) | Entregable 3: informe de pruebas para público no técnico |
| [REGISTRO_DE_CAMBIOS.md](REGISTRO_DE_CAMBIOS.md) | Cambios de esta etapa con motivo y archivos |
| [arquitectura.md](arquitectura.md) | Arquitectura, capas, modelo de datos y reglas de negocio |
| [EXPLICACION_DEL_CODIGO.md](EXPLICACION_DEL_CODIGO.md) | Mapa del código: qué hace cada carpeta y archivo |
| [APORTES_DEL_EQUIPO.md](APORTES_DEL_EQUIPO.md) | Integrantes y aportes |

---

## F. Problemas frecuentes

| Síntoma | Causa / solución |
|---|---|
| `invalid target release: 17` o error de compilación raro | La versión de Java no es 17. Instalar JDK 17 y ajustar `JAVA_HOME`. |
| `Port 5020 was already in use` / `5173` / `80` | Otro programa o una ejecución anterior sigue usando el puerto. Cerrarla (o `docker compose down`). |
| La app abre pero no lista usuarios / "Error de conexión" | El backend no está corriendo o aún no terminó de arrancar. Esperar `Started CasaVerdeApplication`. |
| `npm error enoent Could not read package.json` | `npm` se ejecutó fuera de `Frontend/`. Hacer `cd Frontend`. |
| El pedido queda en `PENDIENTE` aunque se pague / no deja finalizar | Los productos tienen precio $0: asignar precios (paso 0 del recorrido). |
| `./mvnw: Permission denied` (Linux/macOS) | `chmod +x Backend/mvnw` |
| `"C:\Users\Nombre" no se reconoce...` en Windows | Ya está corregido en `Backend/mvnw.cmd`; actualizar el repo (`git pull`) o usar `./mvnw` desde Git Bash. |
| Una prueba de "fecha de reserva pasada" falla solo por la noche | Las pruebas asumen que el computador está en la zona horaria de Colombia (UTC-5); si la zona del equipo ya está en el día siguiente, esa única prueba puede fallar. No indica un error de la aplicación: ejecutar en horario diurno o con la zona horaria de Colombia. |
| En Docker no se puede iniciar sesión | Falta el archivo `.env` (copiar `.env.example` a `.env`). |
