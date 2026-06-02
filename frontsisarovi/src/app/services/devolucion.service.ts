import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AppRefreshService } from './app-refresh.service';
import { Devolucion, DevolucionCreateRequest, DevolucionPagoCreateRequest } from '../models/devolucion.model';

@Injectable({
  providedIn: 'root'
})
export class DevolucionService {
  private readonly http = inject(HttpClient);
  private readonly refreshService = inject(AppRefreshService);
  private readonly apiUrl = `${environment.apiUrl}/devoluciones`;

  listar(estado?: string): Observable<Devolucion[]> {
    let params = new HttpParams();
    if (estado && estado !== 'TODAS') {
      params = params.set('estado', estado);
    }
    return this.http.get<Devolucion[]>(this.apiUrl, { params });
  }

  obtener(id: number): Observable<Devolucion> {
    return this.http.get<Devolucion>(`${this.apiUrl}/${id}`);
  }

  crear(payload: DevolucionCreateRequest): Observable<Devolucion> {
    return this.http.post<Devolucion>(this.apiUrl, payload).pipe(
      tap(() => this.refreshService.notifyChange())
    );
  }

  registrarPago(id: number, payload: DevolucionPagoCreateRequest): Observable<Devolucion> {
    return this.http.post<Devolucion>(`${this.apiUrl}/${id}/pagos`, payload).pipe(
      tap(() => this.refreshService.notifyChange())
    );
  }
}
