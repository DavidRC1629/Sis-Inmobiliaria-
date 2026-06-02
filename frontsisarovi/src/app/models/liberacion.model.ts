export interface LiberacionLote {
  loteId: number;
  loteNumero: number;
  manzana: string;
  parcelaNombre: string;
  parcelaId?: number | null;
  etapaNumero: number;
  etapaId?: number | null;
  projectId: number;
  projectNombre: string;

  titulares: string;
  titularesDni: string;
  cantidadTitulares: number;

  contratoId?: number | null;
  tipoOperacion?: string;
  estadoCronograma?: string;
  estadoVisual: string;
  moroso: boolean;
  montoPagadoTotal: number;
  requierePasswordAdmin: boolean;
}

export interface LiberacionRequest {
  descripcion: string;
  adminPassword?: string;
}
