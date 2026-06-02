export interface Etapa {
  id: number;
  numeroEtapa: number;
  cantidadParcelas: number;
  projectId: number;
}

export interface EtapaRequest {
  numeroEtapa: number;
}
