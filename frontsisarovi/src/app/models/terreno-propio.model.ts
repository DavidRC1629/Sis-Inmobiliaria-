export interface TerrenoPropio {
  id?: number;
  numeroLote: number;
  calle?: string;
  areaM2?: number;
  perimetro?: number;
  medidaFrente?: number;
  medidaFondo?: number;
  medidaIzquierda?: number;
  medidaDerecha?: number;
  numeroPartida: string;
  precio: number;
  propietario: any; // Cliente
  imagenUrl?: string;
  estado: string;
}
