# Informe de pruebas — Casa Verde

**Para:** dueño del negocio / stakeholders no técnicos · **Fecha:** 2026-11-12
Todos los resultados salen de ejecutar las pruebas; se pueden repetir con los comandos indicados. No se usan datos personales reales.

## 1. En una frase

El sistema calcula bien el dinero, descuenta bien el inventario, no deja entrar a quien no tiene permiso y mantiene ordenada su estructura interna; las pruebas lo comprueban cada vez que se ejecutan.

## 2. Resumen

| Qué se probó | Para qué sirve (en simple) | Cantidad | Resultado |
|---|---|---|---|
| Pantallas y utilidades (frontend) | Que los montos se muestren bien, el login y los formularios funcionen | 118 pruebas | ✅ 118 pasan, 0 fallan (ejecutado el 2026-11-12) |
| Reglas del negocio (backend) | Que cobrar, finalizar pedidos, caja, inventario, compras y nómina den los números correctos | 99 pruebas | ✅ 99 pasan, 0 fallan (2026-11-12) |
| Estructura del software (arquitectura) | Que nadie "salte" capas y el sistema siga fácil de mantener | 8 reglas | ✅ 8 pasan; se comprobó que una regla saboteada a propósito sí falla |
| Seguridad (acceso y permisos) | Que nadie entre sin iniciar sesión, que no se pueda falsificar el permiso y que un operador no entre a lo que es solo del administrador | 11 pruebas | ✅ 11 pasan (2026-11-12); con la seguridad apagada a propósito, 5 fallan, como debe ser |

## 3. Cómo repetir las pruebas

```bash
# Frontend (Node 20+)
cd Frontend && npm install && npm run test:run

# Backend (Java 17) — incluye pruebas de reglas y de arquitectura
cd Backend && ./mvnw test      # Windows: mvnw.cmd test
```

Reportes del backend: `Backend/target/surefire-reports/`.

**Resultado backend (salida real de `mvnw test`, JDK 17, 2026-11-12):**

```
Tests run: 118, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

(99 de reglas de negocio y servicios + 8 de arquitectura + 11 de seguridad.)

**Controles para demostrar que las pruebas sí detectan errores** (se hicieron y luego se revirtieron):
- *Arquitectura:* se inyectó a propósito un repository en `PedidoController` → `ArquitecturaCapasTest` falló (`Tests run: 8, Failures: 1`, *"los controllers solo delegan en la capa de servicios"*). Se quitó la línea y volvió a 8/8.
- *Seguridad:* se ejecutó `SeguridadEndpointsTest` con la seguridad desactivada → `Tests run: 11, Failures: 5` (las cinco que exigen bloquear accesos). Con la seguridad activada: 11/11.

## 4. Qué cubre cada prueba

| Prueba | Qué garantiza para el negocio |
|---|---|
| `PedidoServiceTest`, `CasaVerdeBusinessRulesTests` | El total del pedido y los pagos en COP/USD/NEQUI cuadran; el dinero entra a caja solo al finalizar; no se finaliza sin stock |
| `FondoServiceTest` | Entradas, ajustes y transferencias entre fondos no pierden dinero |
| `InventarioServiceTest` | El stock no queda negativo y cada movimiento queda registrado |
| `CompraServiceTest` | Compras, abonos y adelantos a proveedores suman bien |
| `EmpleadoServiceTest` | Adelantos, días trabajados y liquidaciones dan el neto correcto |
| `CrearPedidoRequestValidationTest` | Se rechazan pedidos con datos inválidos |
| `SeguridadEndpointsTest` | Con la seguridad encendida (como en producción): sin sesión o con un pase falso, vencido o alterado no se entra (401); el operador no entra a configuración, usuarios ni backups (403); el administrador sí; una clave equivocada no da acceso; la respuesta de login no muestra la clave cifrada |
| `ArquitecturaCapasTest` | **Prueba de contrato:** controllers no tocan la base de datos directamente, servicios no dependen de controllers, sin inyección por campo, la API no expone entidades |
| Frontend (7 archivos) | Formato de moneda/fechas, fracciones de producto, login, confirmaciones y modal de compras |

## 5. Hallazgos y riesgos (honestos)

| # | Hallazgo | Impacto | Acción |
|---|---|---|---|
| 1 | No hay pruebas de rendimiento ni de carga | No se sabe cuántos pedidos simultáneos soporta | Siguiente etapa: prueba de carga (JMeter/k6) sobre `POST /pedidos` |
| 2 | ~~No había pruebas automáticas de seguridad~~ **Resuelto en esta etapa** con `SeguridadEndpointsTest` (11 pruebas) | — | Mantener: toda ruta nueva pública o solo-ADMIN debe sumar un caso aquí |
| 3 | Las pruebas de reglas de negocio corren con la seguridad apagada (para aislar la lógica) | Se compensa con `SeguridadEndpointsTest`, que sí la enciende | Ninguna adicional |
| 3b | La lista de usuarios (`GET /api/v1/usuarios`) es pública porque la pantalla de inicio la necesita | Cualquiera en la red local ve los nombres de usuario (no las claves) | Evaluar mostrar solo nombres sin rol, o un endpoint reducido |
| 4 | Pruebas de interfaz de extremo a extremo (Playwright/Cypress) no existen | Flujos completos dependen de prueba manual (recorrido guiado del README) | Futuro |

## 6. Conclusión

Las reglas críticas de dinero, inventario y caja están cubiertas por pruebas automáticas repetibles, el control de acceso se verifica con la seguridad encendida, y la estructura en capas queda protegida por una prueba de arquitectura. Quedan como mejora futura: rendimiento/carga y pruebas de extremo a extremo.
