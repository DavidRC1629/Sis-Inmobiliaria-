export interface Cliente {
  id: number;
  nombres: string;
  apellidos: string;
  dni: string;
  email?: string | null;
  telefono: string;
  direccion: string;
  tipoRelacion: 'ADQUISICION' | 'SEPARACION';
  clienteDesde: string;
  projectId: number;
  projectNombre: string;
  etapaNumero: number;
  parcelaNombre: string;
  manzana: string;
  loteId: number;
  loteNumero: number;
}

export interface ClienteRequest {
  nombres: string;
  apellidos: string;
  dni: string;
  email?: string | null;
  telefono: string;
  direccion: string;
  tipoRelacion: 'ADQUISICION' | 'SEPARACION';
  clienteDesde: string;
  loteId: number;
}

export interface ClienteProjectSummary {
  projectId: number;
  projectNombre: string;
  cantidadClientes: number;
}

export interface ClienteLoteOption {
  loteId: number;
  loteNumero: number;
  manzana: string;
  parcelaNombre: string;
  etapaNumero: number;
  projectId: number;
  projectNombre: string;
}

export interface PropietarioRequest {
  nombres: string;
  apellidos: string;
  dni: string;
  email?: string | null;
  telefono: string;
  direccion: string;
}

export interface ClienteAdquisicionRequest {
  loteId: number;
  tipoOperacion: 'CONTADO' | 'CREDITO' | 'SEPARACION';
  fechaOperacion: string;
  asesor?: string;
  medios?: string;
  precioVenta: number;
  montoOperacion: number;
  montoSeparacionObjetivo?: number;
  plazoMeses?: number;
  interesPorcentaje?: number;
  propietarios: PropietarioRequest[];
}
