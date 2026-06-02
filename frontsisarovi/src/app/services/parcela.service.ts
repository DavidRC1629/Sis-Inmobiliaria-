
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ManzanaOption, Parcela, ParcelaRequest } from '../models/parcela.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ParcelaService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  getParcelasByEtapa(etapaId: number): Observable<Parcela[]> {
    return this.http.get<Parcela[]>(`${this.apiUrl}/etapas/${etapaId}/parcelas`);
  }

  getParcelaById(parcelaId: number): Observable<Parcela> {
    return this.http.get<Parcela>(`${this.apiUrl}/parcelas/${parcelaId}`);
  }

  getManzanasByParcela(parcelaId: number): Observable<ManzanaOption[]> {
    return this.http.get<ManzanaOption[]>(`${this.apiUrl}/parcelas/${parcelaId}/manzanas`);
  }

  getParcelaByIdInEtapa(etapaId: number, parcelaId: number): Observable<Parcela> {
    return this.http.get<Parcela>(`${this.apiUrl}/etapas/${etapaId}/parcelas/${parcelaId}`);
  }

  searchByPropietario(etapaId: number, propietario: string): Observable<Parcela[]> {
    return this.http.get<Parcela[]>(`${this.apiUrl}/etapas/${etapaId}/parcelas/search?propietario=${propietario}`);
  }

  createParcela(etapaId: number, request: ParcelaRequest): Observable<Parcela> {
    return this.http.post<Parcela>(`${this.apiUrl}/etapas/${etapaId}/parcelas`, request);
  }

  updateParcela(etapaId: number, parcelaId: number, request: ParcelaRequest): Observable<Parcela> {
    return this.http.put<Parcela>(`${this.apiUrl}/etapas/${etapaId}/parcelas/${parcelaId}`, request);
  }

  deleteParcela(etapaId: number, parcelaId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/etapas/${etapaId}/parcelas/${parcelaId}`);
  }
}
