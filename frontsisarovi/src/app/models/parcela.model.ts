export interface Parcela {
  id: number;
  nombre: string;
  numManzanas: number;
  propietario: string;
  cantidadLotes: number;
  lotesDisponibles: number;
  etapaId: number;
}

export interface ParcelaRequest {
  nombre: string;
  numManzanas: number;
  propietario: string;
}

export interface ManzanaOption {
  id: number;
  nombre: string;
}
