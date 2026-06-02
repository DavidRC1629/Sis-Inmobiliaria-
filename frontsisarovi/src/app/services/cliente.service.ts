import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Cliente, ClienteAdquisicionRequest, ClienteLoteOption, ClienteProjectSummary, ClienteRequest } from '../models/cliente.model';
import { AppRefreshService } from './app-refresh.service';

@Injectable({
  providedIn: 'root'
})
export class ClienteService {
  private readonly projectSummariesCacheKey = 'sisarovi.clientes.project-summaries';
  private readonly historialCacheKey = 'sisarovi.clientes.historial';
  private readonly projectClientsCacheKey = 'sisarovi.clientes.project-clients';
  private http = inject(HttpClient);
  private refreshService = inject(AppRefreshService);
  private apiUrl = `${environment.apiUrl}/clientes`;
  private projectSummariesCache: ClienteProjectSummary[] = this.readArrayCache<ClienteProjectSummary>(this.projectSummariesCacheKey);
  private historialCache = this.readRecordCache<Cliente>(this.historialCacheKey);
  private projectClientsCache = this.readRecordCache<Cliente>(this.projectClientsCacheKey);

  getProjectSummariesSnapshot(): ClienteProjectSummary[] {
    return [...this.projectSummariesCache];
  }

  getClientesByProjectSnapshot(projectId: number): Cliente[] {
    return [...(this.projectClientsCache[String(projectId)] || [])];
  }

  getHistorialSnapshot(query = ''): Cliente[] {
    const key = this.normalizeQuery(query);
    const exact = this.historialCache[key];
    if (Array.isArray(exact)) {
      return [...exact];
    }

    const full = this.historialCache[''] || [];
    if (!key || full.length === 0) {
      return [...full];
    }

    return this.filterClientesLocally(full, key);
  }

  invalidateCaches(): void {
    this.projectSummariesCache = [];
    this.historialCache = {};
    this.projectClientsCache = {};
    this.clearCache(this.projectSummariesCacheKey);
    this.clearCache(this.historialCacheKey);
    this.clearCache(this.projectClientsCacheKey);
  }

  getProjectSummaries(): Observable<ClienteProjectSummary[]> {
    return this.http.get<ClienteProjectSummary[]>(`${this.apiUrl}/proyectos`).pipe(
      tap((summaries) => {
        this.projectSummariesCache = Array.isArray(summaries) ? [...summaries] : [];
        this.writeCache(this.projectSummariesCacheKey, this.projectSummariesCache);
      })
    );
  }

  getClientesByProject(projectId: number): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(`${this.apiUrl}/proyectos/${projectId}`).pipe(
      tap((clientes) => {
        const normalized = Array.isArray(clientes) ? [...clientes] : [];
        this.projectClientsCache[String(projectId)] = normalized;
        this.writeCache(this.projectClientsCacheKey, this.projectClientsCache);
      })
    );
  }

  getHistorial(query?: string): Observable<Cliente[]> {
    let params = new HttpParams();
    const q = (query || '').trim();
    if (q) {
      params = params.set('q', q);
    }
    return this.http.get<Cliente[]>(this.apiUrl, { params }).pipe(
      tap((clientes) => {
        const normalized = Array.isArray(clientes) ? [...clientes] : [];
        this.historialCache[this.normalizeQuery(q)] = normalized;
        if (!q) {
          this.historialCache[''] = normalized;
        }
        this.writeCache(this.historialCacheKey, this.historialCache);
      })
    );
  }

  getLotesDisponibles(projectId: number, clienteId?: number): Observable<ClienteLoteOption[]> {
    let params = new HttpParams().set('projectId', String(projectId));
    if (clienteId) {
      params = params.set('clienteId', String(clienteId));
    }
    return this.http.get<ClienteLoteOption[]>(`${this.apiUrl}/lotes-disponibles`, { params });
  }

  updateCliente(clienteId: number, payload: ClienteRequest): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.apiUrl}/${clienteId}`, payload).pipe(
      tap(() => {
        this.invalidateCaches();
        this.refreshService.notifyChange();
      })
    );
  }

  deleteCliente(clienteId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${clienteId}`).pipe(
      tap(() => {
        this.invalidateCaches();
        this.refreshService.notifyChange();
      })
    );
  }

  registrarAdquisicion(payload: ClienteAdquisicionRequest): Observable<Cliente[]> {
    return this.http.post<Cliente[]>(`${this.apiUrl}/adquisiciones`, payload).pipe(
      tap(() => {
        this.invalidateCaches();
        this.refreshService.notifyChange();
      })
    );
  }

  private normalizeQuery(query: string): string {
    return (query || '').trim().toLowerCase();
  }

  private filterClientesLocally(clientes: Cliente[], normalizedQuery: string): Cliente[] {
    return clientes.filter((cliente) => {
      const fullName = `${cliente.nombres || ''} ${cliente.apellidos || ''}`.toLowerCase();
      const dni = String(cliente.dni || '').toLowerCase();
      return fullName.includes(normalizedQuery) || dni.includes(normalizedQuery);
    });
  }

  private readArrayCache<T>(key: string): T[] {
    try {
      const raw = sessionStorage.getItem(key);
      if (!raw) {
        return [];
      }

      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed as T[] : [];
    } catch {
      return [];
    }
  }

  private readRecordCache<T>(key: string): Record<string, T[]> {
    try {
      const raw = sessionStorage.getItem(key);
      if (!raw) {
        return {};
      }

      const parsed = JSON.parse(raw);
      return parsed && typeof parsed === 'object' ? parsed as Record<string, T[]> : {};
    } catch {
      return {};
    }
  }

  private writeCache(key: string, value: unknown): void {
    try {
      sessionStorage.setItem(key, JSON.stringify(value));
    } catch {
      // Ignore storage failures and keep running with memory state.
    }
  }

  private clearCache(key: string): void {
    try {
      sessionStorage.removeItem(key);
    } catch {
      // Ignore storage failures.
    }
  }
}