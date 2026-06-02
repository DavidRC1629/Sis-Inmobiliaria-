import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReniecService {
  private apiUrl = environment.apiUrl + '/reniec';

  constructor(private http: HttpClient) {}

  buscarPorDni(dni: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/dni/${dni}`);
  }

  buscarPorNombre(nombre: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/nombre/${nombre}`);
  }

  isServiceUnavailable(error: any): boolean {
    return error?.status === 503 || error?.error?.error === 'RENIEC_UNAVAILABLE';
  }

  getFriendlyErrorMessage(error: any): string {
    const detail = String(error?.error?.detail || error?.error?.message || '').toLowerCase();

    if (detail.includes('suscrip') && detail.includes('caduc')) {
      return 'La suscripcion del servicio RENIEC ha caducado. Debes renovarla con el proveedor.';
    }

    if (this.isServiceUnavailable(error)) {
      return 'El servicio de RENIEC no está disponible';
    }

    if (error?.status === 404) {
      return 'No se encontró información en RENIEC para ese DNI';
    }

    return 'No se pudo consultar RENIEC en este momento';
  }

  getTechnicalErrorDetail(error: any): string {
    return String(error?.error?.detail || error?.error?.message || error?.message || error?.statusText || 'Sin detalle adicional');
  }
}
