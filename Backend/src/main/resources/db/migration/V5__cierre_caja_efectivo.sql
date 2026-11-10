-- V5: el arqueo de billetes del cierre de caja pasa de validar "COP" a
-- validar "EFECTIVO" (caja física). COP deja de contarse a mano en el
-- cierre — ahora crece solo por consignaciones desde Efectivo y se confía
-- en el saldo que lleva el sistema. USD y NEQUI no cambian.
--
-- Renombra las 5 columnas de cierre_caja ligadas al arqueo físico de
-- billetes; billetes_100k..2k quedan igual (son cantidades de billete,
-- no atadas a una moneda por nombre).

ALTER TABLE cierre_caja RENAME COLUMN esperado_cop   TO esperado_efectivo;
ALTER TABLE cierre_caja RENAME COLUMN contado_cop    TO contado_efectivo;
ALTER TABLE cierre_caja RENAME COLUMN diferencia_cop TO diferencia_efectivo;
ALTER TABLE cierre_caja RENAME COLUMN base_cop       TO base_efectivo;
ALTER TABLE cierre_caja RENAME COLUMN monedas_cop    TO monedas_efectivo;
