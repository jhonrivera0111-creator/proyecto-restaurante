-- V3: Etapa 2 (prioridad media).
--
-- 1) Indice para el FK abono_compra.adelanto_proveedor_id (no tenia indice
--    dedicado, a diferencia de compra_id que si estaba cubierto).
--
-- 2) Renombra las 9 columnas camelCase de cierre_caja a snake_case, unico
--    lugar del schema que no seguia la convencion. Solo cambia el nombre
--    fisico de columna: el campo Java correspondiente (camelCase, correcto
--    en Java) y el contrato JSON/DTO/frontend no cambian — ver
--    CierreCaja.java donde se agrega el @Column(name=...) explicito.

CREATE INDEX idx_abono_compra_adelanto_prov on abono_compra (adelanto_proveedor_id);

ALTER TABLE cierre_caja RENAME COLUMN contadoCop TO contado_cop;
ALTER TABLE cierre_caja RENAME COLUMN contadoNequi TO contado_nequi;
ALTER TABLE cierre_caja RENAME COLUMN contadoUsd TO contado_usd;
ALTER TABLE cierre_caja RENAME COLUMN diferenciaCop TO diferencia_cop;
ALTER TABLE cierre_caja RENAME COLUMN diferenciaNequi TO diferencia_nequi;
ALTER TABLE cierre_caja RENAME COLUMN diferenciaUsd TO diferencia_usd;
ALTER TABLE cierre_caja RENAME COLUMN esperadoCop TO esperado_cop;
ALTER TABLE cierre_caja RENAME COLUMN esperadoNequi TO esperado_nequi;
ALTER TABLE cierre_caja RENAME COLUMN esperadoUsd TO esperado_usd;
