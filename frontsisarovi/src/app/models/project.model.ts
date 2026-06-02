export interface Project {
  id: number;
  nombre: string;
  imagenUrl?: string;
  logoUrl?: string;
  createdByUsername: string;
  cantidadEtapas: number;
  cantidadParcelasTotal: number;
}

export interface ProjectRequest {
  nombre: string;
  imagenUrl?: string;
  logoUrl?: string;
  cantidadEtapas: number;
}
