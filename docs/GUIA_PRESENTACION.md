# Guía para la presentación — Validación y defensa final

## 0. Qué te piden (y qué NO)

El enunciado dice: **no se trata de agregar funcionalidades**, sino de *demostrar con evidencias que la arquitectura definida funciona como se diseñó*, someterla a pruebas, analizar su comportamiento y **defender las decisiones y acciones futuras**. La sustentación es en vivo, justificando cambios y cualquier duda.

Por eso la presentación **no es "mire lo bonito que quedó el sistema"**: es *"así diseñamos, así lo comprobamos, esto encontramos y esto haremos"*. La app se muestra solo como **objeto de prueba**.

| Entregable del PDF | Dónde está | Qué decir en una frase |
|---|---|---|
| 1. Repositorio organizado (replicable) | `README.md` + estructura de carpetas | "Cualquiera lo clona, lo ejecuta y corre las pruebas sin ayuda nuestra." |
| 2. Manual de buenas prácticas (3–5 págs.) | `docs/MANUAL_BUENAS_PRACTICAS.md` | "Reglas, convenciones, cómo agregar un módulo sin romper la arquitectura y cuándo algo está *terminado*." |
| 3. Informe de pruebas (corazón del corte) | `docs/INFORME_PRUEBAS.md` | "Resultados reales y repetibles, explicados para alguien no técnico." |
| Cambios registrados | `docs/REGISTRO_DE_CAMBIOS.md` + commits | "Todo ajuste tiene fecha, motivo y archivos." |
| Despliegue (bonificable, no calificable) | `docker-compose.yml` | "Opcional: se levanta con `docker compose up`." |

**Reglas finales (críticas):** cada integrante debe poder explicar *cualquier* parte; los resultados deben ser **reales y repetibles** (si algo es inventado se anula ese criterio); los datos personales deben ser **ficticios**.

---

## 1. Estructura sugerida (≈ 12–15 min + preguntas)

Ajusta los tiempos a lo que te hayan dado. Reparte secciones entre los integrantes: **todos hablan**, porque a cualquiera le pueden preguntar.

| # | Min | Diapositiva / momento | Contenido | Evidencia a mostrar |
|---|---|---|---|---|
| 1 | 1 | **Qué es Casa Verde** | Sistema local para un asadero: pedidos, caja multimoneda, inventario, compras, empleados, cierre. Una frase del problema: "dinero e inventario se descuadran". | Captura de la app |
| 2 | 2 | **La arquitectura que definimos** | Diagrama: `Controller → IService → ServiceImpl → Repository → Entity → SQLite` + React delgado. Patrones: capas, fachada de servicios, DTO/Mapper, bloqueo optimista, snapshot. | Diagrama de `docs/arquitectura.md` |
| 3 | 1 | **Qué hicimos en esta etapa** | "No agregamos funciones; validamos." Lista de ajustes: prueba de arquitectura, manual, informe, documentación y comentarios en el código. | `REGISTRO_DE_CAMBIOS.md`, historial de git |
| 4 | 3 | **Estrategia de pruebas** | Pirámide: unitarias (Mockito) → integración (Spring Boot + SQLite temporal) → **contrato de arquitectura (ArchUnit)** → frontend (Vitest). Por qué cada una. | Tabla del informe |
| 5 | 3 | **DEMO EN VIVO de las pruebas** (ver §2) | Correr `mvn test`, mostrar 118 verdes (incluye 11 de seguridad); romper una regla de arquitectura y ver que **falla**. | Terminal en vivo |
| 6 | 2 | **Comportamiento del sistema** | Mostrar que la arquitectura se cumple en ejecución: un pedido → cobrar → finalizar → caja e inventario cambian. | App (3 min máx.) |
| 7 | 2 | **Hallazgos honestos** | Qué se cerró (seguridad automatizada: 11 pruebas nuevas) y qué NO está probado: rendimiento/carga y E2E. Impacto y plan. | Sección 5 del informe |
| 8 | 1 | **Acciones futuras** | JMeter/k6 sobre `POST /pedidos`; pruebas con `app.security.enabled=true`; Playwright; PostgreSQL. | Lista priorizada |
| 9 | 1 | **Cierre** | "La arquitectura se cumple, está protegida por una prueba automática y sabemos dónde están los riesgos." | — |

> Consejo: el **mensaje central** a repetir es: *"Lo que diseñamos, lo podemos demostrar y lo protegemos con pruebas."*

---

## 2. Guion de la demostración en vivo (la parte que da puntos)

Preparar **antes** (ver checklist §5). Tener 2 terminales abiertas y VS Code.

**Paso A — Las pruebas son reales (2 min)**
```bash
cd Backend
mvnw.cmd test            # Windows (si falla por espacios en la ruta, usar el mvn.cmd de .m2)
```
Mostrar al final: `Tests run: 118, Failures: 0, Errors: 0 · BUILD SUCCESS`. Decir: "Son repetibles; el reporte queda en `Backend/target/surefire-reports/`."

**Paso B — La arquitectura se protege sola (2 min) ← el golpe de efecto**
1. Abrir `PedidoController.java`.
2. Inyectar temporalmente un repository (por ejemplo agregar `private final PedidoRepository repo;`).
3. Correr `mvnw.cmd test -Dtest=ArquitecturaCapasTest` → **falla** y señala la violación.
4. Revertir: borrar la línea agregada (o `Ctrl+Z` en el editor) y volver a correr → verde. Solo usa `git checkout -- <archivo>` si **ya hiciste commit** de tus cambios; si no, borraría tus comentarios y ediciones sin guardar.

> Mensaje: "Si alguien rompe la arquitectura, el sistema lo detecta antes de que llegue a producción."

**Paso C — Comportamiento (3 min, mínimo)**
Inventario → registrar compra de gallinas/cuyes → Pedido "1/2 Gallina" → cobrar parte COP + parte NEQUI → **finalizar** → mostrar Fondos (suben) e Inventario (baja 0,5) → Configuración → Auditoría.
Frase clave: *"Registrar el pago no mueve la caja; solo al finalizar, y todo en una transacción."*

Si algo falla en vivo: **no improvises**; muestra el reporte ya generado y di con calma qué pasó. Es mejor que ocultarlo.

---

## 3. Qué responder a cada tema (mapa de defensa)

| Si preguntan... | Responde |
|---|---|
| ¿Qué cambió en esta etapa? | Prueba de arquitectura (pom + test), manual, informe, registro de cambios, documentación y comentarios en el código. Todo en `REGISTRO_DE_CAMBIOS.md` y en git. |
| ¿Cómo demuestran que la arquitectura se respeta? | `ArquitecturaCapasTest` (ArchUnit, 8 reglas): controllers no tocan repositories, servicios no dependen de controllers, sin inyección por campo, la API no expone entidades. Y la demo de sabotaje. |
| ¿Por qué esas decisiones? | Capas = separar responsabilidades; interfaces = bajo acoplamiento y mocks; DTO/MapStruct = contrato de API estable; `BigDecimal` = dinero exacto; `@Version` = concurrencia; snapshot = histórico inmutable; Flyway = esquema versionado. |
| ¿Los resultados son reales? | Sí: comandos del informe §3, reportes en `target/surefire-reports/`, repetibles. |
| ¿Qué no probaron? | Rendimiento/carga y E2E. Están como hallazgos con plan (informe §5). La seguridad sí: `SeguridadEndpointsTest`, 11 pruebas con la seguridad encendida. |
| ¿Cómo saben que la prueba de seguridad sirve? | La corrimos con la seguridad apagada a propósito y fallaron 5 (las que exigen bloquear); encendida, pasan las 11. |
| ¿Y la concurrencia? | `@Version` + transacciones: el segundo operador recibe 409 y reintenta. |
| ¿Cómo agrego un módulo sin romper nada? | Manual §5: migración Flyway → entidad/repo → DTO/mapper → interfaz → impl → controller → pruebas → correr ArchUnit. |
| ¿Cuándo una tarea está terminada? | Definición de terminado del manual: compila, pruebas verdes (incluida la de arquitectura), reglas de negocio intactas, validación en backend, trazabilidad (caja/inventario/auditoría). |
| ¿Escala? | Hoy local monousuario; JPA + capas permiten PostgreSQL y cliente-servidor. SQLite es el límite de concurrencia. |
| ¿Usaron IA? | Sí, está permitido; cada uno puede explicar el código (guías: `EXPLICACION_DEL_CODIGO.md`). |
| ¿Datos personales? | Todos ficticios (verifica nombres y celulares en la BD de demo antes de presentar). |

Documentos de apoyo para estudiar: [SUSTENTACION_PRINCIPAL.md](SUSTENTACION_PRINCIPAL.md), [EXPLICACION_DEL_CODIGO.md](EXPLICACION_DEL_CODIGO.md), [GUIA_SUSTENTACION.md](GUIA_SUSTENTACION.md).

---

## 4. Cómo repartir al equipo

Cada quien debe poder contestar **todo**, pero conviene que cada uno lidere una parte:

| Persona | Lidera |
|---|---|
| Jhon Rivera | Contexto + arquitectura (diapositivas 1–3) |
| Lenny Chilito | Estrategia de pruebas + demo de ArchUnit y seguridad (4–5) |
| Carlos Caicedo | Comportamiento del sistema: recorrido de un pedido, caja e inventario (6) |
| Yonatan Buitron | Hallazgos, acciones futuras, frontend y cierre (7–9) |

(Coincide con [APORTES_DEL_EQUIPO.md](APORTES_DEL_EQUIPO.md).)

---

## 5. Checklist antes de presentar

**La víspera**
- [ ] `git status` limpio: **haz commit** de todo (el enunciado exige cambios *registrados*; el repo tiene pocos commits). Usa una rama (`feature/validacion-final`) y haz push.
- [ ] `cd Backend && mvnw.cmd test` → anota el resultado real (118/0 fallos).
- [ ] `cd Frontend && npm run test:run` → 118 pasan.
- [ ] Repetir el sabotaje de arquitectura una vez para saber cómo se ve el fallo.
- [ ] Revisar que la fecha y los números del informe coincidan con lo que corriste.
- [ ] Probar clonar el repo en una carpeta limpia y seguir el README (replicabilidad).
- [ ] Verificar que los datos de la BD de demo sean **ficticios**.
- [x] `CLAUDE.md`/`AGENTS.md` corregidos (los endpoints sí están protegidos).
- [x] URL del `git clone` del README coincide con el repo real.

**Antes de entrar (15 min)**
- [ ] App corriendo en **dos terminales, en este orden**: 1) Frontend (`cd Frontend && npm run dev`), 2) Backend en otra terminal (`cd Backend` y `mvn spring-boot:run`). Esperar `Started CasaVerdeApplication` y abrir http://localhost:5173 (Administrador / `admin123`).
- [ ] Terminal en `Backend/` lista y VS Code con `PedidoController.java` abierto.
- [ ] Capturas/reportes de respaldo por si falla la conexión en vivo.
- [ ] Cierra notificaciones; comparte pantalla completa o ventana específica.

**Si usas Windows con espacios en el usuario**
`mvnw.cmd` falla ("`C:\Users\Lenny` no se reconoce"). Usa el Maven descargado:
```
& 'C:\Users\Lenny Chilito Lozada\.m2\wrapper\dists\apache-maven-3.9.9\977a63e90f436cd6ade95b4c0e10c20c\bin\mvn.cmd' test
```

---

## 6. Qué evita puntos (errores comunes)

1. **Presentar solo la app** y no las pruebas: el enunciado dice que el informe es el corazón.
2. **Inventar o redondear resultados**: anula el criterio. Muestra salida real.
3. **Esconder lo no probado**: nómbralo tú antes de que pregunten.
4. **No saber explicar una parte**: repasen cada archivo; si no lo saben, digan "lo verifico" y expliquen cómo lo buscarían, no adivinen.
5. **Cambios sin registro**: cada ajuste debe estar en `REGISTRO_DE_CAMBIOS.md` y en un commit.
6. **Informe demasiado técnico**: debe leerlo un no técnico (lenguaje simple, tablas cortas).

---

## 7. Frase de apertura y de cierre (para memorizar)

**Apertura:** "Casa Verde es un sistema local para un asadero. En esta etapa no agregamos funciones: comprobamos con evidencias que la arquitectura que diseñamos se cumple y se mantiene."

**Cierre:** "La arquitectura en capas se respeta, está protegida por una prueba de contrato automática, las reglas de dinero, inventario y acceso están cubiertas por 236 pruebas repetibles (118 de backend y 118 de frontend), y tenemos claros los riesgos pendientes: rendimiento/carga y pruebas de extremo a extremo."
