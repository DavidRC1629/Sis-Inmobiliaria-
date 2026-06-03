UPDATE registro_auditoria ra
JOIN clientes c ON c.dni = ra.cliente_dni
JOIN cronograma_contratos cc ON cc.cliente_id = c.id
JOIN cronograma_cuotas cq ON cq.contrato_id = cc.id
    AND cq.numero_cuota = CAST(REPLACE(REGEXP_SUBSTR(ra.descripcion, '#[0-9]+'), '#', '') AS UNSIGNED)
SET ra.descripcion = REPLACE(ra.descripcion, 'Amortización #', 'Pago de Cuota #')
WHERE ra.accion = 'INGRESO'
  AND ra.descripcion LIKE 'Amortización #%'
  AND ra.monto >= cq.monto_cuota;
