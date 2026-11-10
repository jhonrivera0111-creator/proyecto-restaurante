-- V2: Etapa 1 (alta prioridad) — integridad referencial de moneda/fondo y CHECK de enums.
--
-- SQLite no soporta ALTER TABLE ADD CONSTRAINT: cada tabla se reconstruye
-- (create nueva -> copiar datos -> drop vieja -> rename) preservando columnas,
-- tipos, defaults, FKs e indices existentes.
--
-- FK moneda/fondo_debitado/fondo_acreditado -> fondo(moneda): agregadas solo en
-- columnas donde el valor SIEMPRE es una moneda real (validado contra datos
-- existentes y contra el codigo de cada service). Se EXCLUYEN a proposito:
--   - adelanto.fondo_debitado          (puede ser el literal 'ESPECIE', no una moneda)
--   - abono_compra.fondo_debitado      (puede ser el literal 'ADELANTO_PROVEEDOR', no una moneda)
-- Ambas columnas conservan su valor libre sin FK.
--
-- CHECK agregados solo en columnas respaldadas por un enum Java real, usando
-- exactamente los valores de ese enum (CajaMovimientoTipo, TipoMovimientoInventario,
-- RolUsuario) o por una lista cerrada validada en servicio (Gasto.CATEGORIAS).
-- No se agrega CHECK a movimiento_inventario.referencia_tipo ni
-- caja_movimiento.referencia_tipo: son etiquetas de referencia libres, sin enum
-- Java que las respalde.

-- ── pago ─────────────────────────────────────────────────────────────
CREATE TABLE pago_new (
    monto numeric(19,2) not null,
    monto_cop numeric(19,0) not null,
    tasa_usada numeric(19,2),
    id integer,
    pedido_id integer not null,
    registrado_en timestamp not null,
    version bigint,
    moneda varchar(30) not null,
    primary key (id),
    foreign key (pedido_id) references pedido(id) on delete cascade,
    foreign key (moneda) references fondo(moneda)
);
INSERT INTO pago_new (monto, monto_cop, tasa_usada, id, pedido_id, registrado_en, version, moneda)
SELECT monto, monto_cop, tasa_usada, id, pedido_id, registrado_en, version, moneda FROM pago;
DROP TABLE pago;
ALTER TABLE pago_new RENAME TO pago;
CREATE INDEX idx_pago_pedido_id on pago (pedido_id);
CREATE INDEX idx_pago_registrado_en on pago (registrado_en);

-- ── gasto ────────────────────────────────────────────────────────────
CREATE TABLE gasto_new (
    cantidad_inventario numeric(19,4),
    monto numeric(19,2) not null,
    monto_cop numeric(19,0) not null,
    tasa_usada numeric(19,2),
    id integer,
    registrado_en timestamp not null,
    version bigint,
    categoria varchar(30) not null check (categoria in ('insumos','servicios','equipos','otros')),
    moneda varchar(30) not null,
    insumo_inventario varchar(100),
    concepto varchar(300) not null,
    observacion TEXT,
    primary key (id),
    foreign key (moneda) references fondo(moneda)
);
INSERT INTO gasto_new (cantidad_inventario, monto, monto_cop, tasa_usada, id, registrado_en, version, categoria, moneda, insumo_inventario, concepto, observacion)
SELECT cantidad_inventario, monto, monto_cop, tasa_usada, id, registrado_en, version, categoria, moneda, insumo_inventario, concepto, observacion FROM gasto;
DROP TABLE gasto;
ALTER TABLE gasto_new RENAME TO gasto;
CREATE INDEX idx_gasto_registrado on gasto (registrado_en);

-- ── adelanto (empleado) ──────────────────────────────────────────────
CREATE TABLE adelanto_new (
    fecha date not null,
    monto numeric(19,2) not null,
    monto_cop numeric(19,0) not null,
    pagado boolean not null,
    tasa_usada numeric(19,2),
    creado_en timestamp not null,
    empleado_id integer not null,
    id integer,
    version bigint,
    tipo VARCHAR(20) DEFAULT 'EFECTIVO' not null check (tipo in ('EFECTIVO','ESPECIE')),
    fondo_debitado varchar(30) not null,
    moneda varchar(30) not null,
    observacion TEXT,
    primary key (id),
    foreign key (empleado_id) references empleado(id),
    foreign key (moneda) references fondo(moneda)
);
INSERT INTO adelanto_new (fecha, monto, monto_cop, pagado, tasa_usada, creado_en, empleado_id, id, version, tipo, fondo_debitado, moneda, observacion)
SELECT fecha, monto, monto_cop, pagado, tasa_usada, creado_en, empleado_id, id, version, tipo, fondo_debitado, moneda, observacion FROM adelanto;
DROP TABLE adelanto;
ALTER TABLE adelanto_new RENAME TO adelanto;
CREATE INDEX idx_adelanto_empleado_pagado on adelanto (empleado_id, pagado);

-- ── abono_cuenta_cobrar ──────────────────────────────────────────────
CREATE TABLE abono_cuenta_cobrar_new (
    fecha date not null,
    monto numeric(19,2) not null,
    monto_cop numeric(19,0) not null,
    tasa_usada numeric(19,2),
    creado_en timestamp not null,
    cuenta_cobrar_id integer not null,
    id integer,
    version bigint,
    fondo_acreditado varchar(30) not null,
    moneda varchar(30) not null,
    observacion TEXT,
    primary key (id),
    foreign key (cuenta_cobrar_id) references cuenta_cobrar(id),
    foreign key (moneda) references fondo(moneda),
    foreign key (fondo_acreditado) references fondo(moneda)
);
INSERT INTO abono_cuenta_cobrar_new (fecha, monto, monto_cop, tasa_usada, creado_en, cuenta_cobrar_id, id, version, fondo_acreditado, moneda, observacion)
SELECT fecha, monto, monto_cop, tasa_usada, creado_en, cuenta_cobrar_id, id, version, fondo_acreditado, moneda, observacion FROM abono_cuenta_cobrar;
DROP TABLE abono_cuenta_cobrar;
ALTER TABLE abono_cuenta_cobrar_new RENAME TO abono_cuenta_cobrar;
CREATE INDEX idx_abono_cc_cuenta_creado on abono_cuenta_cobrar (cuenta_cobrar_id, creado_en);

-- ── pago_empleado ────────────────────────────────────────────────────
CREATE TABLE pago_empleado_new (
    dias_trabajados integer,
    monto_cop numeric(19,0) not null,
    monto_pagado numeric(19,2) not null,
    periodo_fin date not null,
    periodo_inicio date not null,
    salario_bruto numeric(19,0) not null,
    tasa_usada numeric(19,2),
    total_adelantos numeric(19,0) not null,
    total_neto numeric(19,0) not null,
    creado_en timestamp not null,
    empleado_id integer not null,
    id integer,
    tipo varchar(20) not null check (tipo in ('QUINCENA','COBRO_DIAS')),
    fondo_debitado varchar(30) not null,
    moneda varchar(30) not null,
    observacion TEXT,
    primary key (id),
    foreign key (empleado_id) references empleado(id),
    foreign key (moneda) references fondo(moneda),
    foreign key (fondo_debitado) references fondo(moneda)
);
INSERT INTO pago_empleado_new (dias_trabajados, monto_cop, monto_pagado, periodo_fin, periodo_inicio, salario_bruto, tasa_usada, total_adelantos, total_neto, creado_en, empleado_id, id, tipo, fondo_debitado, moneda, observacion)
SELECT dias_trabajados, monto_cop, monto_pagado, periodo_fin, periodo_inicio, salario_bruto, tasa_usada, total_adelantos, total_neto, creado_en, empleado_id, id, tipo, fondo_debitado, moneda, observacion FROM pago_empleado;
DROP TABLE pago_empleado;
ALTER TABLE pago_empleado_new RENAME TO pago_empleado;
CREATE INDEX idx_pago_empleado_empleado on pago_empleado (empleado_id);

-- ── caja_movimiento ──────────────────────────────────────────────────
CREATE TABLE caja_movimiento_new (
    monto numeric(19,2) not null,
    monto_cop numeric(19,0) not null,
    saldo_resultante numeric(19,2) not null,
    id integer,
    referencia_id bigint,
    registrado_en timestamp not null,
    moneda varchar(30) not null,
    referencia_tipo varchar(50),
    tipo varchar(50) not null check (tipo in ('COBRO_PEDIDO','REVERSO_PAGO','GASTO','REVERSO_GASTO','AJUSTE_FONDO','ENTRADA_MANUAL','TRANSFERENCIA_SALIDA','TRANSFERENCIA_ENTRADA','COMPRA_PAGO','ADELANTO_EMPLEADO','PAGO_EMPLEADO','ADELANTO_PROVEEDOR','APLICACION_ADELANTO_PROVEEDOR','COBRO_CUENTA_COBRAR')),
    concepto varchar(300) not null,
    observacion TEXT,
    primary key (id),
    foreign key (moneda) references fondo(moneda)
);
INSERT INTO caja_movimiento_new (monto, monto_cop, saldo_resultante, id, referencia_id, registrado_en, moneda, referencia_tipo, tipo, concepto, observacion)
SELECT monto, monto_cop, saldo_resultante, id, referencia_id, registrado_en, moneda, referencia_tipo, tipo, concepto, observacion FROM caja_movimiento;
DROP TABLE caja_movimiento;
ALTER TABLE caja_movimiento_new RENAME TO caja_movimiento;
CREATE INDEX idx_cm_registrado on caja_movimiento (registrado_en);
CREATE INDEX idx_cm_moneda_registrado on caja_movimiento (moneda, registrado_en);
CREATE INDEX idx_cm_referencia on caja_movimiento (referencia_tipo, referencia_id);

-- ── movimiento_inventario ────────────────────────────────────────────
CREATE TABLE movimiento_inventario_new (
    cantidad numeric(19,4) not null,
    stock_resultante numeric(19,4) not null,
    id integer,
    referencia_id bigint,
    registrado_en timestamp not null,
    tipo varchar(20) not null check (tipo in ('entrada','salida','ajuste')),
    referencia_tipo varchar(30),
    insumo varchar(100) not null,
    observacion TEXT,
    primary key (id)
);
INSERT INTO movimiento_inventario_new (cantidad, stock_resultante, id, referencia_id, registrado_en, tipo, referencia_tipo, insumo, observacion)
SELECT cantidad, stock_resultante, id, referencia_id, registrado_en, tipo, referencia_tipo, insumo, observacion FROM movimiento_inventario;
DROP TABLE movimiento_inventario;
ALTER TABLE movimiento_inventario_new RENAME TO movimiento_inventario;
CREATE INDEX idx_mi_insumo_registrado on movimiento_inventario (insumo, registrado_en);
CREATE INDEX idx_mi_tipo_ref_insumo on movimiento_inventario (tipo, referencia_tipo, insumo);
CREATE INDEX idx_mi_registrado on movimiento_inventario (registrado_en);

-- ── usuario ──────────────────────────────────────────────────────────
CREATE TABLE usuario_new (
    activo boolean not null,
    requiere_password boolean not null,
    creado_en timestamp not null,
    id integer,
    nombre varchar(255) not null unique,
    password_hash varchar(255),
    rol varchar(255) not null check (rol in ('ADMIN','OPERADOR')),
    primary key (id)
);
INSERT INTO usuario_new (activo, requiere_password, creado_en, id, nombre, password_hash, rol)
SELECT activo, requiere_password, creado_en, id, nombre, password_hash, rol FROM usuario;
DROP TABLE usuario;
ALTER TABLE usuario_new RENAME TO usuario;

-- ── abono_compra ─────────────────────────────────────────────────────
-- fondo_debitado NO recibe FK: puede ser 'ADELANTO_PROVEEDOR' (abono aplicado
-- contra un adelanto prepagado, sin tocar el fondo — ver CompraServiceImpl).
CREATE TABLE abono_compra_new (
    fecha date not null,
    monto numeric(19,2) not null,
    monto_cop numeric(19,0) not null,
    tasa_usada numeric(19,2),
    adelanto_proveedor_id integer,
    compra_id integer not null,
    creado_en timestamp not null,
    id integer,
    version bigint,
    fondo_debitado varchar(30) not null,
    moneda varchar(30) not null,
    primary key (id),
    foreign key (compra_id) references compra(id),
    foreign key (adelanto_proveedor_id) references adelanto_proveedor(id),
    foreign key (moneda) references fondo(moneda)
);
INSERT INTO abono_compra_new (fecha, monto, monto_cop, tasa_usada, adelanto_proveedor_id, compra_id, creado_en, id, version, fondo_debitado, moneda)
SELECT fecha, monto, monto_cop, tasa_usada, adelanto_proveedor_id, compra_id, creado_en, id, version, fondo_debitado, moneda FROM abono_compra;
DROP TABLE abono_compra;
ALTER TABLE abono_compra_new RENAME TO abono_compra;
CREATE INDEX idx_abono_compra_compra_creado on abono_compra (compra_id, creado_en);

-- ── adelanto_proveedor ───────────────────────────────────────────────
CREATE TABLE adelanto_proveedor_new (
    activo boolean not null,
    fecha date not null,
    monto numeric(19,0) not null,
    monto_cop_inicial numeric(19,0) not null,
    precio_unitario_acordado numeric(19,0) not null,
    saldo_restante numeric(19,0) not null,
    creado_en timestamp not null,
    id integer,
    proveedor_id integer not null,
    version bigint,
    fondo_debitado varchar(30) not null,
    moneda varchar(30) not null,
    insumo varchar(100) not null,
    proveedor_nombre varchar(150) not null,
    observacion TEXT,
    primary key (id),
    foreign key (proveedor_id) references proveedor(id),
    foreign key (moneda) references fondo(moneda),
    foreign key (fondo_debitado) references fondo(moneda)
);
INSERT INTO adelanto_proveedor_new (activo, fecha, monto, monto_cop_inicial, precio_unitario_acordado, saldo_restante, creado_en, id, proveedor_id, version, fondo_debitado, moneda, insumo, proveedor_nombre, observacion)
SELECT activo, fecha, monto, monto_cop_inicial, precio_unitario_acordado, saldo_restante, creado_en, id, proveedor_id, version, fondo_debitado, moneda, insumo, proveedor_nombre, observacion FROM adelanto_proveedor;
DROP TABLE adelanto_proveedor;
ALTER TABLE adelanto_proveedor_new RENAME TO adelanto_proveedor;
CREATE INDEX idx_adelanto_prov_proveedor_activo on adelanto_proveedor (proveedor_id, activo);
