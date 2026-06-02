
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Lote, LoteRequest } from '../models/lote.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class LoteService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  getLotesByParcela(parcelaId: number): Observable<Lote[]> {
    return this.http.get<Lote[]>(`${this.apiUrl}/parcelas/${parcelaId}/lotes`);
  }

  getLotesByManzana(parcelaId: number, manzana: string): Observable<Lote[]> {
    return this.http.get<Lote[]>(`${this.apiUrl}/parcelas/${parcelaId}/lotes/manzana/${manzana}`);
  }

  getLoteById(parcelaId: number, loteId: number): Observable<Lote> {
    return this.http.get<Lote>(`${this.apiUrl}/parcelas/${parcelaId}/lotes/${loteId}`);
  }

  existsNumeroPartida(parcelaId: number, numeroPartida: string, excludeLoteId?: number): Observable<boolean> {
    const params = new URLSearchParams();
    params.set('numeroPartida', numeroPartida);
    if (excludeLoteId != null) {
      params.set('excludeLoteId', String(excludeLoteId));
    }
    return this.http.get<boolean>(`${this.apiUrl}/parcelas/${parcelaId}/lotes/numero-partida/existe?${params.toString()}`);
  }

  createLote(parcelaId: number, request: LoteRequest): Observable<Lote> {
    return this.http.post<Lote>(`${this.apiUrl}/parcelas/${parcelaId}/lotes`, request);
  }

  updateLote(parcelaId: number, loteId: number, request: LoteRequest): Observable<Lote> {
    return this.http.put<Lote>(`${this.apiUrl}/parcelas/${parcelaId}/lotes/${loteId}`, request);
  }

  deleteLote(parcelaId: number, loteId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/parcelas/${parcelaId}/lotes/${loteId}`);
  }
}
