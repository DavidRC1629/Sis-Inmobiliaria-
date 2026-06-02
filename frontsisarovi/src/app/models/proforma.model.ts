export interface ProformaRequest {
  codigo?: string;
  proyecto?: string;
  clienteNombre?: string;
  clienteDni?: string;
  clienteCelular?: string;
  asesor?: string;
  fechaEmision?: string;
  fechaVencimiento?: string;
  precioContado?: number;
  detalle?: any;
}

export interface ProformaItem {
  id: number;
  codigo: string;
  proyecto: string;
  clienteNombre: string;
  clienteDni: string;
  asesor: string;
  fechaEmision: string;
  fechaVencimiento: string;
  precioContado: number;
  createdAt: string;
  hasPdf?: boolean;
}
