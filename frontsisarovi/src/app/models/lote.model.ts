export interface Lote {
  id: number;
  numero: number;
  calle?: string;
  perimetro?: number;
  areaM2?: number;
  medidaFrente?: number;
  medidaIzquierda?: number;
  medidaDerecha?: number;
  medidaFondo?: number;
  numeroPartida: string;
  precioLote?: number;
  manzanaId?: number;
  manzana: string;
  parcelaId: number;
  parcelaNombre?: string;
  etapaNumero?: number;
  projectId?: number;
  projectNombre?: string;
  adquirido?: boolean;
}

export interface LoteRequest {
  numero: number;
  calle?: string;
  perimetro?: number;
  areaM2?: number;
  medidaFrente?: number;
  medidaIzquierda?: number;
  medidaDerecha?: number;
  medidaFondo?: number;
  numeroPartida: string;
  precioLote: number;
  manzanaId?: number | null;
  manzana: string;
}
