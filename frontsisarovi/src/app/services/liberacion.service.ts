import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { LiberacionLote, LiberacionRequest } from '../models/liberacion.model';

@Injectable({
  providedIn: 'root'
})
export class LiberacionService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/liberaciones`;

  listarLotesAdquiridos(projectId: number): Observable<LiberacionLote[]> {
    const params = new HttpParams().set('projectId', String(projectId));
    return this.http.get<LiberacionLote[]>(`${this.apiUrl}/lotes`, { params });
  }

  liberarLote(loteId: number, payload: LiberacionRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/lotes/${loteId}`, payload);
  }
}
