import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TerrenoPropio } from '../models/terreno-propio.model';

@Injectable({ providedIn: 'root' })
export class TerrenoPropioService {
  private apiUrl = 'http://localhost:8080/api/terrenos-propios';

  constructor(private http: HttpClient) {}

  getAll(): Observable<TerrenoPropio[]> {
    return this.http.get<TerrenoPropio[]>(this.apiUrl);
  }

  getById(id: number): Observable<TerrenoPropio> {
    return this.http.get<TerrenoPropio>(`${this.apiUrl}/${id}`);
  }

  create(data: any): Observable<TerrenoPropio> {
    return this.http.post<TerrenoPropio>(this.apiUrl, data);
  }

  existsByNumeroPartida(numeroPartida: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/exists/partida/${numeroPartida}`);
  }

  uploadImagen(id: number, file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.apiUrl}/${id}/imagen`, formData, { responseType: 'text' });
  }

  deleteImagen(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/imagen`);
  }

  adquirirTerreno(id: number, data: any): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/adquirir`, data);
  }
}
