// Estructura para respuesta de Reniec
export interface ReniecData {
  number: string;
  full_name: string;
  name: string;
  surname: string;
  date_of_birth: string;
  department: string;
  province: string;
  district: string;
  address: string;
  ubigeo: string;
}

export type UserOrReniec = User | ReniecData;
export interface User {
  id: number;
  dni: string;
  email?: string;
  nombres: string;
  primerApellido: string;
  segundoApellido: string;
  role: Role;
  estado: string;
  rejectionExpiresAt?: string | null;
}

export interface Role {
  id: number;
  name: string;
}

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface RegisterRequest {
  nombres: string;
  dni: string;
  email: string;
  password: string;
  primerApellido: string;
  segundoApellido: string;
}

export interface AuthResponse {
  token: string;
  dni: string;
  email?: string;
  nombres: string;
  primerApellido: string;
  segundoApellido: string;
  role: string;
  message?: string;
  requirePasswordChange?: boolean;
  user?: User;
}

export interface Project {
  id: number;
  nombre: string;
  imagenUrl?: string;
  createdByUsername: string;
  cantidadEtapas: number;
  cantidadParcelasTotal: number;
}

export interface ProjectRequest {
  nombre: string;
  imagenUrl?: string;
}
