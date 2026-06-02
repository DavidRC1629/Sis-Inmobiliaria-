export type CronogramaTipoOperacion = 'CONTADO' | 'CREDITO' | 'SEPARACION';
export type CronogramaEstado = 'AL_DIA' | 'DEUDOR' | 'SEPARACION_EN_CURSO';

export interface CronogramaCuota {
  id: number;
  numeroCuota: number;
  fechaVencimiento: string;
  montoCuota: number;
  montoPagado: number;
  saldoPendiente: number;
  estadoPago: 'PENDIENTE' | 'PARCIAL' | 'PAGADA';
  diasRetraso: number;
  fechaPago?: string | null;
  pagos?: CronogramaPagoCuota[];
}

export interface CronogramaPagoCuota {
  id: number;
  fechaPago: string;
  monto: number;
  tipoPago?: string;
  estadoPago: 'PENDIENTE' | 'PARCIAL' | 'PAGADA' | string;
  notas?: string;
}

export interface CronogramaContrato {
  id: number;
  clienteId: number;
  clienteNombre: string;
  clienteDni: string;
  asesor?: string | null;
  projectId: number;
  projectNombre: string;
  etapaNumero: number;
  parcelaNombre: string;
  manzana: string;
  loteId: number;
  loteNumero: number;
  tipoOperacion: CronogramaTipoOperacion;
  estado: CronogramaEstado;
  fechaOperacion: string;
  fechaInicioCronograma?: string | null;
  precioVenta: number;
  montoPagadoTotal: number;
  montoSeparacionObjetivo: number;
  montoSeparacionAcumulado: number;
  saldoFinanciarInicial: number;
  saldoPendienteActual: number;
  plazoMeses: number;
  interesPorcentaje: number;
  montoCuotaReferencial: number;
  pagosSeparacion?: CronogramaPagoCuota[];
  cuotas: CronogramaCuota[];
}

export interface CronogramaFiltro {
  projectId?: number;
  etapaNumero?: number;
  parcelaNombre?: string;
  manzana?: string;
  loteId?: number;
  dni?: string;
  nombres?: string;
  estado?: CronogramaEstado;
}

export interface RegistrarPagoPayload {
  monto: number;
  fechaPago?: string;
  observacion?: string;
  metadata?: {
    tipo?: string;
    numeroCuota?: number;
    clienteNombre?: string;
    clienteDni?: string;
    medios?: Array<{ medio: string; monto: number; efectivoEntregado: number }>;
    [key: string]: any;
  };
}
