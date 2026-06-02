
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { CronogramaContrato, CronogramaFiltro, RegistrarPagoPayload } from '../models/cronograma.model';

@Injectable({
  providedIn: 'root'
})
export class CronogramaService {
  private readonly listCacheKey = 'sisarovi.cronogramas.list-cache';
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/cronogramas`;
  private listCache = this.readCache();

  // Limpia el cache local de contratos (para forzar refresco tras operaciones críticas)
  clearCache(): void {
    this.listCache = {};
    try {
      sessionStorage.removeItem(this.listCacheKey);
    } catch (e) {}
  }

  getCachedListSnapshot(filtro?: CronogramaFiltro): CronogramaContrato[] {
    const exact = this.listCache[this.serializeFilter(filtro)] || [];
    if (exact.length > 0) {
      return [...exact];
    }

    const fullList = this.listCache['{}'] || [];
    if (!filtro || fullList.length === 0) {
      return [...fullList];
    }

    return this.filterLocally(fullList, filtro);
  }

  listar(filtro?: CronogramaFiltro): Observable<CronogramaContrato[]> {
    let params = new HttpParams();

    if (filtro) {
      Object.entries(filtro).forEach(([key, value]) => {
        if (value !== undefined && value !== null && `${value}`.trim() !== '') {
          params = params.set(key, String(value));
        }
      });
    }

    return this.http.get<CronogramaContrato[]>(this.apiUrl, { params }).pipe(
      tap((contratos) => {
        const normalized = Array.isArray(contratos) ? [...contratos] : [];
        this.listCache[this.serializeFilter(filtro)] = normalized;
        this.writeCache();
      })
    );
  }

  obtenerPorId(id: number): Observable<CronogramaContrato> {
    return this.http.get<CronogramaContrato>(`${this.apiUrl}/${id}`);
  }

  registrarPagoSeparacion(contratoId: number, payload: RegistrarPagoPayload): Observable<CronogramaContrato> {
    return this.http.post<CronogramaContrato>(`${this.apiUrl}/${contratoId}/pagos/separacion`, payload).pipe(
      tap((updated) => this.upsertInCachedLists(updated))
    );
  }

  registrarPagoCuota(cuotaId: number, payload: RegistrarPagoPayload): Observable<CronogramaContrato> {
    return this.http.post<CronogramaContrato>(`${this.apiUrl}/cuotas/${cuotaId}/pagos`, payload).pipe(
      tap((updated) => this.upsertInCachedLists(updated))
    );
  }

  updateAsesor(id: number, asesor: string): Observable<CronogramaContrato> {
    return this.http.patch<CronogramaContrato>(`${this.apiUrl}/${id}/asesor`, { asesor }).pipe(
      tap((updated) => this.upsertInCachedLists(updated))
    );
  }

  private serializeFilter(filtro?: CronogramaFiltro): string {
    if (!filtro) {
      return '{}';
    }

    const normalized: Record<string, string> = {};
    Object.entries(filtro).forEach(([key, value]) => {
      if (value !== undefined && value !== null && `${value}`.trim() !== '') {
        normalized[key] = String(value).trim();
      }
    });

    return JSON.stringify(normalized);
  }

  private upsertInCachedLists(updated: CronogramaContrato): void {
    if (!updated?.id) {
      return;
    }

    Object.keys(this.listCache).forEach((key) => {
      const current = this.listCache[key] || [];
      const next = current.map((contrato) => contrato.id === updated.id ? updated : contrato);
      this.listCache[key] = next;
    });

    this.writeCache();
  }

  private filterLocally(contratos: CronogramaContrato[], filtro: CronogramaFiltro): CronogramaContrato[] {
    const normalizedDni = String(filtro.dni || '').trim().toLowerCase();
    const normalizedNombres = String(filtro.nombres || '').trim().toLowerCase();
    const normalizedEstado = String(filtro.estado || '').trim().toUpperCase();
    const normalizedProjectId = filtro.projectId != null ? Number(filtro.projectId) : null;
    const normalizedEtapaNumero = filtro.etapaNumero != null ? Number(filtro.etapaNumero) : null;
    const normalizedParcelaNombre = String(filtro.parcelaNombre || '').trim().toLowerCase();
    const normalizedManzana = String(filtro.manzana || '').trim().toLowerCase();
    const normalizedLoteId = filtro.loteId != null ? Number(filtro.loteId) : null;

    return contratos.filter((contrato) => {
      if (normalizedDni && !String(contrato.clienteDni || '').toLowerCase().includes(normalizedDni)) {
        return false;
      }

      if (normalizedNombres && !String(contrato.clienteNombre || '').toLowerCase().includes(normalizedNombres)) {
        return false;
      }

      if (normalizedEstado && String(contrato.estado || '').toUpperCase() !== normalizedEstado) {
        return false;
      }

      if (normalizedProjectId != null && Number(contrato.projectId || 0) !== normalizedProjectId) {
        return false;
      }

      if (normalizedEtapaNumero != null && Number(contrato.etapaNumero || 0) !== normalizedEtapaNumero) {
        return false;
      }

      if (normalizedParcelaNombre && !String(contrato.parcelaNombre || '').toLowerCase().includes(normalizedParcelaNombre)) {
        return false;
      }

      if (normalizedManzana && !String(contrato.manzana || '').toLowerCase().includes(normalizedManzana)) {
        return false;
      }

      if (normalizedLoteId != null && Number(contrato.loteId || 0) !== normalizedLoteId) {
        return false;
      }

      return true;
    });
  }

  private readCache(): Record<string, CronogramaContrato[]> {
    try {
      const raw = sessionStorage.getItem(this.listCacheKey);
      if (!raw) {
        return {};
      }

      const parsed = JSON.parse(raw);
      return parsed && typeof parsed === 'object' ? parsed as Record<string, CronogramaContrato[]> : {};
    } catch {
      return {};
    }
  }

  private writeCache(): void {
    try {
      sessionStorage.setItem(this.listCacheKey, JSON.stringify(this.listCache));
    } catch {
      // Ignore storage failures.
    }
  }

  aplicarDescuento(payload: { clienteId: number; montoDescuento: number; observacion?: string }): Observable<CronogramaContrato> {
    return this.http.post<CronogramaContrato>(`${this.apiUrl}/aplicar-descuento`, payload);
  }
}
