import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { ProformaItem, ProformaRequest } from '../models/proforma.model';
import { AppRefreshService } from './app-refresh.service';

@Injectable({ providedIn: 'root' })
export class ProformaService {
  private readonly historialCacheKey = 'proformas_historial_cache';
  private readonly refreshService = inject(AppRefreshService);

    getManzanas(parcelaId: number) {
      return this.http.get<any[]>(`${environment.apiUrl}/parcelas/${parcelaId}/manzanas`);
    }
    getLotesByManzana(manzanaId: number) {
      return this.http.get<any[]>(`${environment.apiUrl}/manzanas/${manzanaId}/lotes`);
    }
  constructor(private http: HttpClient) {}

  getProyectos() {
    return this.http.get<any[]>(`${environment.apiUrl}/projects`);
  }

  getLogoArovi() {
    return this.http.get<{ logoAroviUrl: string }>(`${environment.apiUrl}/settings/logo-arovi`);
  }

  updateLogoArovi(logoAroviUrl: string) {
    return this.http.put<{ logoAroviUrl: string }>(`${environment.apiUrl}/settings/logo-arovi`, { logoAroviUrl });
  }

  updateProjectLogo(projectId: number, logoUrl: string) {
    return this.http.put<any>(`${environment.apiUrl}/projects/${projectId}/logo`, { logoUrl });
  }
  getEtapas(projectId: number) {
    return this.http.get<any[]>(`${environment.apiUrl}/projects/${projectId}/etapas`);
  }
  getParcelas(etapaId: number) {
    return this.http.get<any[]>(`${environment.apiUrl}/etapas/${etapaId}/parcelas`);
  }
  getLotes(parcelaId: number) {
    return this.http.get<any[]>(`${environment.apiUrl}/parcelas/${parcelaId}/lotes`);
  }

  createProforma(payload: ProformaRequest): Observable<ProformaItem> {
    return this.http.post<ProformaItem>(`${environment.apiUrl}/proformas`, payload);
  }

  createProformaWithPdf(payload: ProformaRequest, pdfBlob: Blob, fileName: string): Observable<ProformaItem> {
    const formData = new FormData();
    formData.append('payload', JSON.stringify(payload));
    formData.append('pdfFile', pdfBlob, fileName);

    return this.http.post<ProformaItem>(`${environment.apiUrl}/proformas/pdf`, formData).pipe(
      tap((saved) => {
        if (!saved) {
          return;
        }
        const current = this.getHistorialCache();
        const withoutSame = current.filter((item) => item.id !== saved.id);
        this.saveHistorialCache([saved, ...withoutSame]);
        this.refreshService.notifyChange();
      })
    );
  }

  getHistorialProformas(): Observable<ProformaItem[]> {
    return this.http.get<ProformaItem[]>(`${environment.apiUrl}/proformas/historial`, {
      params: { _t: Date.now().toString() },
      headers: {
        'Cache-Control': 'no-cache',
        Pragma: 'no-cache'
      }
    }).pipe(
      tap((items) => {
        this.saveHistorialCache(items || []);
        this.refreshService.notifyChange();
      })
    );
  }

  getHistorialCache(): ProformaItem[] {
    try {
      const raw = localStorage.getItem(this.historialCacheKey);
      if (!raw) {
        return [];
      }
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed as ProformaItem[] : [];
    } catch {
      return [];
    }
  }

  private saveHistorialCache(items: ProformaItem[]): void {
    try {
      localStorage.setItem(this.historialCacheKey, JSON.stringify(items || []));
    } catch {
    }
  }

  buscarProformas(tipo: 'codigo' | 'cliente', q: string): Observable<ProformaItem[]> {
    return this.http.get<ProformaItem[]>(`${environment.apiUrl}/proformas/buscar`, {
      params: { tipo, q, _t: Date.now().toString() },
      headers: {
        'Cache-Control': 'no-cache',
        Pragma: 'no-cache'
      }
    });
  }

  verPdfProforma(id: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/proformas/${id}/pdf`, { responseType: 'blob' });
  }
}
