
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Etapa, EtapaRequest } from '../models/etapa.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EtapaService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  getEtapasByProject(projectId: number): Observable<Etapa[]> {
    return this.http.get<Etapa[]>(`${this.apiUrl}/projects/${projectId}/etapas`);
  }

  getEtapaById(projectId: number, etapaId: number): Observable<Etapa> {
    return this.http.get<Etapa>(`${this.apiUrl}/projects/${projectId}/etapas/${etapaId}`);
  }

  createEtapa(projectId: number, request: EtapaRequest): Observable<Etapa> {
    return this.http.post<Etapa>(`${this.apiUrl}/projects/${projectId}/etapas`, request);
  }

  updateEtapa(projectId: number, etapaId: number, request: EtapaRequest): Observable<Etapa> {
    return this.http.put<Etapa>(`${this.apiUrl}/projects/${projectId}/etapas/${etapaId}`, request);
  }

  deleteEtapa(projectId: number, etapaId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/projects/${projectId}/etapas/${etapaId}`);
  }
}
