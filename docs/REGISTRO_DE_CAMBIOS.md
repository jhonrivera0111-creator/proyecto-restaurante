# Registro de cambios de la etapa de validación final

| Fecha | Cambio | Motivo | Archivos |
|---|---|---|---|
| 2026-11-12 | Se agrega prueba de contrato de arquitectura (ArchUnit, 8 reglas) | Demostrar con evidencia automática que las capas se respetan | `Backend/pom.xml`, `Backend/src/test/java/.../arquitectura/ArquitecturaCapasTest.java` |
| 2026-11-12 | Se agrega Manual de buenas prácticas | Entregable 2 | `docs/MANUAL_BUENAS_PRACTICAS.md` |
| 2026-11-12 | Se agrega Informe de pruebas | Entregable 3 | `docs/INFORME_PRUEBAS.md` |
| 2026-11-12 | README enlaza la nueva documentación | Entregable 1 (replicabilidad) | `README.md` |
| 2026-11-12 | Se comenta el código principal en español (solo comentarios, sin cambios de lógica) | Que cualquier integrante pueda explicar cada parte | `PedidoServiceImpl`, `PedidoFinalizacionServiceImpl`, `Pedido`, `EstadoPedido`, `PedidoTotales`, `PedidoController`, `CajaMovimientoServiceImpl`, `InventarioServiceImpl`, `CierreCajaServiceImpl`, `SecurityConfig`, `AuthTokenService`, `GlobalExceptionHandler`, `DataInitializer`, `Frontend/src/{App.jsx,services/api.js,services/pedidoService.js,store/useSessionStore.js,utils/format.js}` |
| 2026-11-12 | Se agregan pruebas de seguridad con la autenticación activada (11 casos: 401 sin token / token falso, alterado o vencido; 403 OPERADOR en rutas ADMIN; login y rutas públicas). Control: con la seguridad apagada fallan 5 | Cerrar el hallazgo #2 del informe de pruebas (seguridad sin cobertura) | `Backend/src/test/java/.../seguridad/SeguridadEndpointsTest.java`, `docs/INFORME_PRUEBAS.md` |
| 2026-11-12 | README: orden de arranque (frontend en una terminal, luego backend en otra), solución al fallo de `mvnw.cmd` con espacios en la ruta, conteo de pruebas actualizado y enlaces a toda la documentación | Entregable 1 (replicabilidad sin ayuda del equipo) | `README.md` |
| 2026-11-12 | Manual de buenas prácticas ampliado: ejemplo de capa correcta/incorrecta, reglas para dinero e inventario, prácticas de pruebas y de seguridad, errores típicos y lista de revisión entre integrantes | Entregable 2 (3–5 páginas con reglas aplicables) | `docs/MANUAL_BUENAS_PRACTICAS.md` |
| 2026-11-12 | `CLAUDE.md` y `AGENTS.md` corregidos: los endpoints sí están protegidos (token firmado + roles) | La documentación decía lo contrario que el código | `CLAUDE.md`, `AGENTS.md` |
| 2026-11-12 | Se agregan guías de sustentación, explicación del código y presentación | Preparar la defensa en vivo | `docs/SUSTENTACION_PRINCIPAL.md`, `docs/EXPLICACION_DEL_CODIGO.md`, `docs/GUIA_PRESENTACION.md` |
