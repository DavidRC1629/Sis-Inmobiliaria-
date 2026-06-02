export interface DevolucionPago {
  id: number;
  monto: number;
  fechaPago: string;
  descripcion: string;
  medioPago: string;
  fechaRegistro: string;
}

export interface Devolucion {
  id: number;
  loteId: number;
  loteNumero: number;
  manzana: string;
  parcelaNombre: string;
  etapaNumero: number;
  proyectoNombre: string;
  montoTotal: number;
  montoPagado: number;
  montoPendiente: number;
  dias: number;
  fechaInicio: string;
  fechaFinEstimada: string;
  descripcion: string;
  estado: 'EN_CURSO' | 'COMPLETADA' | string;
  progreso: number;
  fechaCreacion: string;
  fechaActualizacion: string;
  pagos: DevolucionPago[];
}

export interface DevolucionCreateRequest {
  loteId: number;
  loteNumero: number;
  manzana: string;
  parcelaNombre: string;
  etapaNumero: number;
  proyectoNombre: string;
  montoTotal: number;
  fechaInicio: string;
  fechaFinEstimada: string;
  dias: number;
  descripcion: string;
}

export interface DevolucionPagoCreateRequest {
  monto: number;
  fechaPago: string;
  descripcion: string;
  medioPago: string;
}
