import { AfterViewInit, ApplicationRef, ChangeDetectorRef, Component, ElementRef, inject, OnInit, HostListener, QueryList, ViewChildren } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { catchError, finalize, forkJoin, of, timeout } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';
import jsPDF from 'jspdf';

interface AuditLogEntry {
  id: number;
  timestamp: Date;
  userDni: string;
  userName: string;
  entityType: string;
  entityLabel: string;
  entityId: number | null;
  operationType: 'INGRESO' | 'CREATE' | 'UPDATE' | 'DELETE' | 'PROFORMA' | 'CRONOGRAMA' | 'ADQUISICION_LOTE' | 'OTHER';
  ingresoMonto?: number;
  clienteNombre?: string;
  clienteDni?: string;
  medios?: string;
  item?: string;
  rawDescripcion?: string;
  loteNumero?: number;
  manzanaNombre?: string;
  parcelaNombre?: string;
  etapaNumero?: number;
  proyectoNombre?: string;
  oldValue?: any;
  newValue?: any;
  description: string;
}

interface RegistroAuditoriaApi {
  id: number;
  usuario: string;
  usuarioNombre?: string;
  usuarioDni?: string;
  accion: string;
  descripcion: string;
  fechaHora: string;
  clienteNombre?: string;
  clienteDni?: string;
  monto?: number;
  medios?: string;
  item?: string;
  loteNumero?: number;
  manzanaNombre?: string;
  parcelaNombre?: string;
  etapaNumero?: number;
  proyectoNombre?: string;
}

interface ProjectSummaryApi {
  id: number;
  nombre: string;
}

interface CronogramaLookupApi {
  clienteDni?: string;
  cuotas?: Array<{
    numeroCuota?: number;
    montoCuota?: number;
  }>;
}

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './registro.component.html',
  styleUrls: ['./registro.component.css']
})
export class RegistroComponent implements OnInit, AfterViewInit {
  private router = inject(Router);
  private authService = inject(AuthService);
  private http = inject(HttpClient);
  private cdRef = inject(ChangeDetectorRef);
  private appRef = inject(ApplicationRef);

  showProfileMenu = false;
  currentUser: any = null;
  auditLogs: AuditLogEntry[] = [];
  filteredLogs: AuditLogEntry[] = [];
  isLoading = false;
  entityTypeOptions: string[] = [];
  private projectNameById: Record<number, string> = {};
  private cuotaMontoByClienteYNumero: Record<string, number> = {};
  logsViewportMaxHeight: number | null = null;
  private loadGuardTimeoutId: ReturnType<typeof setTimeout> | null = null;
  @ViewChildren('logCardRef') logCards!: QueryList<ElementRef<HTMLElement>>;

  // Filtros
  selectedEntityType: string = 'all';
  selectedOperationType: string = 'all';
  selectedDateFrom: string = '';
  selectedDateTo: string = '';
  readonly pageSize = 40;
  currentPage = 1;
  editItemId: number | null = null;
  editItemDraft: string = '';
  editItemMessage: string = '';
  private editItemMessageTimeout: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });

    this.loadAuditLogs(true);
  }

  ngAfterViewInit(): void {
    this.logCards.changes.subscribe(() => {
      this.scheduleLogsViewportRecalc();
    });
    this.scheduleLogsViewportRecalc();
  }

  loadAuditLogs(showLoader: boolean = true): void {
    if (showLoader) {
      this.isLoading = true;
    }

    if (this.loadGuardTimeoutId) {
      clearTimeout(this.loadGuardTimeoutId);
      this.loadGuardTimeoutId = null;
    }

    this.loadGuardTimeoutId = setTimeout(() => {
      if (!this.isLoading) {
        return;
      }
      this.isLoading = false;
      this.requestViewUpdate();
    }, 4500);

    forkJoin({
      logs: this.http.get<RegistroAuditoriaApi[]>(`${environment.apiUrl}/registro-auditoria`, {
        params: { _t: Date.now().toString() },
        headers: {
          'Cache-Control': 'no-cache',
          Pragma: 'no-cache'
        }
      }).pipe(
        timeout(7000),
        catchError((err) => {
          console.error('Error al cargar registros:', err);
          return of([] as RegistroAuditoriaApi[]);
        })
      ),
      projects: this.http.get<ProjectSummaryApi[]>(`${environment.apiUrl}/projects`).pipe(
        timeout(7000),
        catchError((err) => {
          console.warn('No se pudieron cargar nombres de proyectos para auditoría:', err);
          return of([] as ProjectSummaryApi[]);
        })
      ),
      cronogramas: this.http.get<CronogramaLookupApi[]>(`${environment.apiUrl}/cronogramas`).pipe(
        timeout(7000),
        catchError((err) => {
          console.warn('No se pudieron cargar cronogramas para clasificar ingresos:', err);
          return of([] as CronogramaLookupApi[]);
        })
      )
    }).pipe(
      finalize(() => {
        if (this.loadGuardTimeoutId) {
          clearTimeout(this.loadGuardTimeoutId);
          this.loadGuardTimeoutId = null;
        }
        this.isLoading = false;
        this.requestViewUpdate();
      })
    ).subscribe({
      next: ({ logs, projects, cronogramas }) => {
        this.projectNameById = (projects || []).reduce((acc, project) => {
          if (project?.id != null && project?.nombre) {
            acc[project.id] = project.nombre;
          }
          return acc;
        }, {} as Record<number, string>);

        this.cuotaMontoByClienteYNumero = (cronogramas || []).reduce((acc, cronograma) => {
          const dni = (cronograma?.clienteDni || '').trim();
          if (!dni) {
            return acc;
          }

          for (const cuota of cronograma.cuotas || []) {
            const numero = Number(cuota?.numeroCuota || 0);
            if (!numero) {
              continue;
            }
            acc[this.buildCuotaLookupKey(dni, numero)] = Number(cuota?.montoCuota || 0);
          }
          return acc;
        }, {} as Record<string, number>);

        this.auditLogs = (logs || []).map((item) => this.mapApiToAuditLog(item));
        this.entityTypeOptions = Array.from(new Set(this.auditLogs.map((log) => log.entityType))).sort((a, b) => a.localeCompare(b));
        if (this.selectedDateFrom || this.selectedDateTo) {
          this.applyFilters();
        } else {
          this.applyFilters();
        }
        this.requestViewUpdate();
        this.scheduleLogsViewportRecalc();
      }
    });
  }

  private requestViewUpdate(): void {
    this.cdRef.detectChanges();
    this.appRef.tick();
  }

  applyFilters(): void {
    this.filteredLogs = this.auditLogs.filter(log => {
      const matchesEntity = this.selectedEntityType === 'all' || log.entityType === this.selectedEntityType;
      const matchesOperation = this.selectedOperationType === 'all' || log.operationType === this.selectedOperationType;
      const matchesDate = this.matchesSelectedDate(log.timestamp);

      return matchesEntity && matchesOperation && matchesDate;
    }).sort((a, b) => {
      const at = new Date(a.timestamp).getTime();
      const bt = new Date(b.timestamp).getTime();
      return bt - at;
    });

    this.currentPage = 1;
    this.scheduleLogsViewportRecalc();
  }

  private scheduleLogsViewportRecalc(): void {
    setTimeout(() => this.recalculateLogsViewportHeight(), 0);
  }

  private recalculateLogsViewportHeight(): void {
    const cards = this.logCards?.toArray() || [];

    if (cards.length <= 10) {
      this.logsViewportMaxHeight = null;
      this.requestViewUpdate();
      return;
    }

    const firstTenCards = cards.slice(0, 10);
    const totalCardHeight = firstTenCards.reduce((sum, card) => sum + card.nativeElement.offsetHeight, 0);
    const gapPx = 24;
    const totalGapHeight = (firstTenCards.length - 1) * gapPx;

    const calculated = totalCardHeight + totalGapHeight + 2;
    const viewportCap = Math.floor(window.innerHeight * 0.68);
    this.logsViewportMaxHeight = Math.min(calculated, viewportCap);
    this.requestViewUpdate();
  }

  getOperationIcon(operation: string): string {
    switch (operation) {
      case 'INGRESO': return '💰';
      case 'CREATE': return '➕';
      case 'UPDATE': return '✏️';
      case 'DELETE': return '🗑️';
      case 'PROFORMA': return '🧾';
      case 'CRONOGRAMA': return '📆';
      case 'ADQUISICION_LOTE': return '🏘️';
      default: return '📋';
    }
  }

  getOperationClass(operation: string): string {
    switch (operation) {
      case 'INGRESO': return 'operation-ingreso';
      case 'CREATE': return 'operation-create';
      case 'UPDATE': return 'operation-update';
      case 'DELETE': return 'operation-delete';
      case 'PROFORMA': return 'operation-proforma';
      case 'CRONOGRAMA': return 'operation-cronograma';
      case 'ADQUISICION_LOTE': return 'operation-lote';
      default: return '';
    }
  }

  getOperationLabel(operation: string): string {
    switch (operation) {
      case 'INGRESO': return 'INGRESO';
      case 'CREATE': return 'CREACIÓN';
      case 'UPDATE': return 'ACTUALIZACIÓN';
      case 'DELETE': return 'ELIMINACIÓN';
      case 'PROFORMA': return 'PROFORMA';
      case 'CRONOGRAMA': return 'CRONOGRAMA';
      case 'ADQUISICION_LOTE': return 'ADQ. LOTE';
      default: return 'ACCIÓN';
    }
  }

  getEntityIcon(entityType: string): string {
    switch (entityType) {
      case 'Proyecto': return '🏢';
      case 'Usuario': return '👤';
      case 'Lote': return '🏘️';
      case 'Manzana': return '🗺️';
      case 'Proforma': return '🧾';
      case 'Parcela': return '📍';
      case 'Etapa': return '🧱';
      default: return '📄';
    }
  }

  private mapApiToAuditLog(item: RegistroAuditoriaApi): AuditLogEntry {
    const entityType = this.extractEntityType(item?.descripcion || '', item?.accion || '');
    const operationType = this.normalizeOperationType(item?.accion || '');
    const entityId = this.extractEntityId(item?.descripcion || '');
    const userDni = this.resolveUserDni(item);
    const userName = this.resolveUserName(item, userDni);
    const entityLabel = this.buildEntityLabel(entityType, entityId, item?.descripcion || '');

    const apiMonto = Number(item?.monto || 0);
    const montoPorMedios = this.parseMontoDesdeMedios(item?.medios || '');
    const montoIngresoNormalizado = montoPorMedios > 0 && (apiMonto <= 0 || montoPorMedios < apiMonto)
      ? montoPorMedios
      : apiMonto;

    return {
      id: item?.id,
      timestamp: item?.fechaHora ? new Date(item.fechaHora) : new Date(),
      userDni,
      userName,
      entityType,
      entityLabel,
      entityId,
      operationType,
      rawDescripcion: item?.descripcion || '',
      ingresoMonto: montoIngresoNormalizado,
      clienteNombre: (item?.clienteNombre || '').trim(),
      clienteDni: (item?.clienteDni || '').trim(),
      medios: item?.medios || '',
      item: (item?.item || '').trim() || this.extractItemLegacy(item?.descripcion || ''),
      loteNumero: item?.loteNumero ?? undefined,
      manzanaNombre: (item?.manzanaNombre || '').trim(),
      parcelaNombre: (item?.parcelaNombre || '').trim(),
      etapaNumero: item?.etapaNumero ?? undefined,
      proyectoNombre: (item?.proyectoNombre || '').trim(),
      description: this.getFriendlyDescription(item?.descripcion || '', operationType, entityType)
    };
  }

  private parseMontoDesdeMedios(medios: string): number {
    const text = (medios || '').trim();
    if (!text) {
      return 0;
    }

    const matches = text.match(/S\/\s*([0-9]+(?:[.,][0-9]{1,2})?)/gi) || [];
    if (matches.length === 0) {
      return 0;
    }

    return matches.reduce((sum, token) => {
      const cleaned = token.replace(/S\//i, '').trim().replace(',', '.');
      const value = Number(cleaned);
      return Number.isFinite(value) ? sum + value : sum;
    }, 0);
  }

  private extractItemLegacy(text: string): string {
    const match = /\bdescrip[^:]*:\s*(.+)$/i.exec(text || '');
    return (match?.[1] || '').trim();
  }

  getIngresoItem(log: AuditLogEntry): string {
    const item = (log?.item || '').trim();
    if (item) {
      return item;
    }

    const raw = (log?.rawDescripcion || '').trim();
    if (raw) {
      const parsed = this.extractItemLegacy(raw);
      if (parsed) {
        return parsed;
      }
    }

    const visible = (log?.description || '').trim();
    const fallback = this.extractItemLegacy(visible);
    return fallback || '-';
  }

  getIngresoTipoLabel(log: AuditLogEntry): string {
    const item = this.getIngresoItem(log).toLowerCase();
    if (item.includes('separac')) {
      return 'Pago de Separación';
    }
    if (item.includes('inicial')) {
      return 'Pago Inicial';
    }

    const numeroCuota = this.extractNumeroCuota(log?.rawDescripcion || log?.description || '');
    const clienteDni = (log?.clienteDni || '').trim();
    if (numeroCuota != null && clienteDni) {
      const cuotaMonto = this.cuotaMontoByClienteYNumero[this.buildCuotaLookupKey(clienteDni, numeroCuota)] || 0;
      if (cuotaMonto > 0) {
        return Number(log?.ingresoMonto || 0) >= cuotaMonto ? 'Pago de Cuota' : 'Amortización';
      }
    }

    const text = ((log?.rawDescripcion || '') + ' ' + (log?.description || '')).toLowerCase();
    if (text.includes('separac')) {
      return 'Pago de Separación';
    }
    if (text.includes('pago inicial')) {
      return 'Pago Inicial';
    }
    if (text.includes('pago de cuota')) {
      return 'Pago de Cuota';
    }
    if (text.includes('amortiz')) {
      return 'Amortización';
    }
    return 'Pago de Cuota';
  }

  private extractNumeroCuota(text: string): number | null {
    const match = /#(\d+)/.exec(text || '');
    return match ? Number(match[1]) : null;
  }

  private buildCuotaLookupKey(clienteDni: string, numeroCuota: number): string {
    return `${clienteDni.trim()}::${numeroCuota}`;
  }

  iniciarEdicionItem(log: AuditLogEntry): void {
    this.editItemId = log.id;
    this.editItemDraft = this.getIngresoItem(log) === '-' ? '' : this.getIngresoItem(log);
    this.editItemMessage = '';
  }

  cancelarEdicionItem(): void {
    this.editItemId = null;
    this.editItemDraft = '';
  }

  guardarEdicionItem(log: AuditLogEntry): void {
    const item = (this.editItemDraft || '').trim().slice(0, 150);

    this.http.put<RegistroAuditoriaApi>(`${environment.apiUrl}/registro-auditoria/${log.id}/item`, { item })
      .pipe(
        catchError((err) => {
          this.showEditItemMessage(err?.error?.message || 'No se pudo editar el item.');
          return of(null);
        })
      )
      .subscribe((updated) => {
        if (!updated) {
          return;
        }

        // Defer UI state mutation to next macrotask to avoid NG0100 in template checks.
        setTimeout(() => {
          const updatedItem = (updated.item || '').trim();

          this.auditLogs = this.auditLogs.map((entry) => {
            if (entry.id !== log.id) {
              return entry;
            }
            return {
              ...entry,
              item: updatedItem,
              rawDescripcion: updated.descripcion || entry.rawDescripcion
            };
          });

          this.filteredLogs = this.filteredLogs.map((entry) => {
            if (entry.id !== log.id) {
              return entry;
            }
            return {
              ...entry,
              item: updatedItem,
              rawDescripcion: updated.descripcion || entry.rawDescripcion
            };
          });

          this.cancelarEdicionItem();
          this.showEditItemMessage('Editado Corecctamente');
          this.cdRef.detectChanges();
        }, 0);
      });
  }

  private showEditItemMessage(message: string): void {
    this.editItemMessage = message;
    if (this.editItemMessageTimeout) {
      clearTimeout(this.editItemMessageTimeout);
    }
    this.editItemMessageTimeout = setTimeout(() => {
      this.editItemMessage = '';
      this.cdRef.detectChanges();
    }, 2400);
  }

  getIngresoLoteInfo(log: AuditLogEntry): string {
    const partes: string[] = [];
    if (log.proyectoNombre) {
      partes.push(`Proyecto: ${log.proyectoNombre}`);
    }
    if (log.etapaNumero != null) {
      partes.push(`Etapa: ${log.etapaNumero}`);
    }
    if (log.parcelaNombre) {
      partes.push(`Parcela: ${log.parcelaNombre}`);
    }
    if (log.manzanaNombre) {
      partes.push(`Manzana: ${log.manzanaNombre}`);
    }
    if (log.loteNumero != null) {
      partes.push(`Lote: ${log.loteNumero}`);
    }

    return partes.length > 0 ? partes.join(' | ') : '-';
  }

  private normalizeOperationType(raw: string): AuditLogEntry['operationType'] {
    const value = (raw || '').toUpperCase();
    if (value === 'INGRESO' || value === 'CREATE' || value === 'UPDATE' || value === 'DELETE' || value === 'PROFORMA' || value === 'CRONOGRAMA' || value === 'ADQUISICION_LOTE') {
      return value;
    }
    return 'OTHER';
  }

  private extractEntityType(description: string, accion?: string): string {
    const normalized = (description || '').toLowerCase();

    if (normalized.includes('proforma')) return 'Proforma';
    if (normalized.includes('proyecto')) return 'Proyecto';
    if (normalized.includes('usuario')) return 'Usuario';
    if (normalized.includes('lote')) return 'Lote';
    if (normalized.includes('cronograma')) return 'Cronograma';
    const match = /Entidad=([^|]+)/i.exec(description || '');
    return (match?.[1] || 'Sistema').trim();
  }

  private extractEntityId(description: string): number | null {
    const match = /\/(\d+)(?:\D|$)/.exec(description || '');
    return match ? Number(match[1]) : null;
  }

  private resolveUserDni(item: RegistroAuditoriaApi): string {
    const fromApi = (item?.usuarioDni || '').trim();
    if (fromApi) {
      return fromApi;
    }

    const fallback = (item?.usuario || '').trim();
    const dniMatch = fallback.match(/\b\d{8}\b/);
    return dniMatch ? dniMatch[0] : '-';
  }

  private resolveUserName(item: RegistroAuditoriaApi, userDni: string): string {
    const fromApi = (item?.usuarioNombre || '').trim();
    if (fromApi) {
      return fromApi;
    }

    const fallback = (item?.usuario || '').trim();
    if (!fallback) {
      return 'ANONIMO';
    }

    if (userDni !== '-' && fallback === userDni) {
      return this.currentUser?.nombres || this.currentUser?.nombre || 'Usuario no identificado';
    }

    return fallback;
  }

  private buildEntityLabel(entityType: string, entityId: number | null, description: string): string {
    if (entityType === 'Proyecto') {
      const projectName = this.extractProjectName(description);
      if (projectName) {
        return `Proyecto: ${projectName}`;
      }

      if (entityId != null && this.projectNameById[entityId]) {
        return `Proyecto: ${this.projectNameById[entityId]}`;
      }
    }

    return entityId ? `${entityType} #${entityId}` : entityType;
  }

  private extractProjectName(description: string): string {
    const text = (description || '').trim();
    if (!text) {
      return '';
    }

    const singleQuoteMatch = /proyecto\s+'([^']+)'/i.exec(text);
    if (singleQuoteMatch?.[1]) {
      return singleQuoteMatch[1].trim();
    }

    const doubleQuoteMatch = /proyecto\s+"([^"]+)"/i.exec(text);
    return doubleQuoteMatch?.[1]?.trim() || '';
  }

  getEntityLabel(log: AuditLogEntry): string {
    return log.entityLabel;
  }

  get pagedLogs(): AuditLogEntry[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredLogs.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredLogs.length / this.pageSize));
  }

  get totalIngresosPeriodo(): number {
    return this.filteredLogs
      .filter((log) => log.operationType === 'INGRESO')
      .reduce((sum, log) => sum + Number(log.ingresoMonto || 0), 0);
  }

  generarPdfIngresos(): void {
    const ingresos = this.filteredLogs.filter((l) => l.operationType === 'INGRESO');
    if (ingresos.length === 0) {
      return;
    }

    const doc = new jsPDF('l', 'mm', 'a4');
    const pageW = 297;
    const pageH = 210;
    const marginX = 10;
    const usableW = pageW - marginX * 2;

    // Header band
    doc.setFillColor(140, 20, 30);
    doc.rect(0, 0, pageW, 22, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('SISAROVI - REGISTRO DE INGRESOS', pageW / 2, 14, { align: 'center' });

    // Period subtitle
    const desde = this.selectedDateFrom ? this.pdfFormatIsoDate(this.selectedDateFrom) : 'Inicio';
    const hasta = this.selectedDateTo ? this.pdfFormatIsoDate(this.selectedDateTo) : 'Hoy';
    doc.setTextColor(50, 50, 50);
    doc.setFontSize(9.5);
    doc.setFont('helvetica', 'normal');
    doc.text(`Registro de ingresos de ${desde} a ${hasta}`, marginX, 30);
    const now = new Date();
    doc.text(
      `Generado el: ${String(now.getDate()).padStart(2, '0')}/${String(now.getMonth() + 1).padStart(2, '0')}/${now.getFullYear()}`,
      pageW - marginX,
      30,
      { align: 'right' }
    );

    // Summary row
    const total = ingresos.reduce((s, l) => s + Number(l.ingresoMonto || 0), 0);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(10.5);
    doc.text(`Total de ingresos: S/ ${total.toFixed(2)}`, marginX, 38);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9.5);
    doc.text(`${ingresos.length} registro(s)`, pageW - marginX, 38, { align: 'right' });

    // Separator line
    doc.setDrawColor(180, 180, 180);
    doc.line(marginX, 41, marginX + usableW, 41);

    // Table columns definition
    const cols: Array<{ title: string; w: number; x: number; align?: 'left' | 'right' }> = [
      { title: 'Fecha', w: 15, x: marginX },
      { title: 'Cliente', w: 33, x: marginX + 15 },
      { title: 'DNI', w: 16, x: marginX + 48 },
      { title: 'Tipo', w: 20, x: marginX + 64 },
      { title: 'Monto', w: 14, x: marginX + 84, align: 'right' },
      { title: 'Item', w: 35, x: marginX + 98 },
      { title: 'Medios', w: 40, x: marginX + 133 },
      { title: 'Efectivo', w: 12, x: marginX + 173, align: 'right' },
      { title: 'Yape', w: 12, x: marginX + 185, align: 'right' },
      { title: 'Transfer.', w: 12, x: marginX + 197, align: 'right' },
      { title: 'Dep.', w: 12, x: marginX + 209, align: 'right' },
      { title: 'Tarjeta', w: 12, x: marginX + 221, align: 'right' },
      { title: 'Lote / Proyecto', w: 44, x: marginX + 233 },
    ];

    const rowMinH = 6;
    const lineHeight = 2.8;
    const cellPadX = 0.9;
    const cellPadY = 1.3;
    let y = 44;

    const drawVerticalSeparators = (yTop: number, yBottom: number) => {
      // Left border
      doc.line(marginX, yTop, marginX, yBottom);
      for (const col of cols) {
        const xRight = col.x + col.w;
        doc.line(xRight, yTop, xRight, yBottom);
      }
    };

    const drawTableHeader = () => {
      const rowH = rowMinH;
      doc.setFillColor(140, 20, 30);
      doc.rect(marginX, y, usableW, rowH, 'F');
      doc.setTextColor(255, 255, 255);
      doc.setFontSize(7.2);
      doc.setFont('helvetica', 'bold');
      for (const col of cols) {
        if (col.align === 'right') {
          doc.text(col.title, col.x + col.w - 1, y + 4.2, { align: 'right' });
        } else {
          doc.text(col.title, col.x + 1, y + 4.2);
        }
      }
      // Header borders for a clear column separation
      doc.setDrawColor(255, 255, 255);
      doc.setLineWidth(0.15);
      drawVerticalSeparators(y, y + rowH);
      doc.line(marginX, y, marginX + usableW, y);
      doc.line(marginX, y + rowH, marginX + usableW, y + rowH);
      y += rowH;
    };

    const wrapCellLines = (value: string, width: number, maxLines: number): string[] => {
      const input = (value || '-').trim() || '-';
      const wrappedRaw = doc.splitTextToSize(input, Math.max(4, width - cellPadX * 2));
      const wrapped = Array.isArray(wrappedRaw) ? wrappedRaw.map((s) => String(s)) : [String(wrappedRaw)];
      if (wrapped.length <= maxLines) {
        return wrapped;
      }

      const clipped = wrapped.slice(0, maxLines);
      const last = (clipped[maxLines - 1] || '').trim();
      clipped[maxLines - 1] = last.length > 0 ? `${last.slice(0, Math.max(1, last.length - 1))}...` : '...';
      return clipped;
    };

    drawTableHeader();

    // Data rows
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(6.5);
    let rowIndex = 0;
    for (const log of ingresos) {
      const desgloseMedios = this.pdfParseMontosPorMedio(log.medios || '');
      const fecha = this.pdfFormatTimestamp(log.timestamp);
      const cliente = log.clienteNombre || '-';
      const dni = (log.clienteDni || '-').substring(0, 12);
      const tipo = this.getIngresoTipoLabel(log);
      const monto = Number(log.ingresoMonto || 0).toFixed(2);
      const item = this.getIngresoItem(log) || '-';
      const medios = log.medios || '-';
      const lote = this.getIngresoLoteInfoShort(log) || '-';

      const rowLines: string[][] = [
        wrapCellLines(fecha, cols[0].w, 1),
        wrapCellLines(cliente, cols[1].w, 2),
        wrapCellLines(dni, cols[2].w, 1),
        wrapCellLines(tipo, cols[3].w, 2),
        wrapCellLines(monto, cols[4].w, 1),
        wrapCellLines(item, cols[5].w, 2),
        wrapCellLines(medios, cols[6].w, 3),
        wrapCellLines(this.pdfAmount(desgloseMedios.efectivo), cols[7].w, 1),
        wrapCellLines(this.pdfAmount(desgloseMedios.yape), cols[8].w, 1),
        wrapCellLines(this.pdfAmount(desgloseMedios.transferencia), cols[9].w, 1),
        wrapCellLines(this.pdfAmount(desgloseMedios.deposito), cols[10].w, 1),
        wrapCellLines(this.pdfAmount(desgloseMedios.tarjeta), cols[11].w, 1),
        wrapCellLines(lote, cols[12].w, 2),
      ];

      const maxLines = rowLines.reduce((max, lines) => Math.max(max, lines.length), 1);
      const rowH = Math.max(rowMinH, cellPadY * 2 + maxLines * lineHeight);

      if (y + rowH > pageH - 14) {
        doc.addPage();
        y = 15;
        drawTableHeader();
      }

      if (rowIndex % 2 === 0) {
        doc.setFillColor(248, 248, 248);
        doc.rect(marginX, y, usableW, rowH, 'F');
      }

      // Draw cell borders row by row
      doc.setDrawColor(205, 205, 205);
      doc.setLineWidth(0.12);
      drawVerticalSeparators(y, y + rowH);
      doc.line(marginX, y, marginX + usableW, y);
      doc.line(marginX, y + rowH, marginX + usableW, y + rowH);

      doc.setTextColor(40, 40, 40);
      for (let ci = 0; ci < cols.length; ci++) {
        const col = cols[ci];
        const lines = rowLines[ci];
        for (let li = 0; li < lines.length; li++) {
          const textY = y + cellPadY + (li + 1) * lineHeight;
          if (col.align === 'right') {
            doc.text(lines[li], col.x + col.w - cellPadX, textY, { align: 'right' });
          } else {
            doc.text(lines[li], col.x + cellPadX, textY);
          }
        }
      }

      y += rowH;
      rowIndex++;
    }

    // Bottom border + total
    doc.setDrawColor(180, 180, 180);
    doc.line(marginX, y, marginX + usableW, y);
    y += 6;
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(10);
    doc.setTextColor(140, 20, 30);
    doc.text(`TOTAL: S/ ${total.toFixed(2)}`, marginX + usableW, y, { align: 'right' });

    const fileName = `ingresos_${desde.replace(/\//g, '-')}_${hasta.replace(/\//g, '-')}.pdf`;
    doc.save(fileName);
  }

  private pdfFormatIsoDate(iso: string): string {
    const parts = iso.split('-');
    return parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : iso;
  }

  private pdfFormatTimestamp(ts: Date | string): string {
    const d = new Date(ts);
    return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
  }

  private pdfTruncate(str: string, maxLen: number): string {
    if (!str || str.length <= maxLen) return str || '';
    return str.substring(0, maxLen - 1) + '\u2026';
  }

  private getIngresoLoteInfoShort(log: AuditLogEntry): string {
    const partes: string[] = [];
    if (log.proyectoNombre) partes.push(log.proyectoNombre);
    if (log.manzanaNombre) partes.push(`Mz.${log.manzanaNombre}`);
    if (log.loteNumero != null) partes.push(`L${log.loteNumero}`);
    return partes.length > 0 ? partes.join(' ') : '-';
  }

  private pdfAmount(value: number): string {
    return value > 0 ? value.toFixed(2) : '-';
  }

  private pdfParseMontosPorMedio(mediosText: string): {
    efectivo: number;
    yape: number;
    transferencia: number;
    deposito: number;
    tarjeta: number;
  } {
    const result = {
      efectivo: 0,
      yape: 0,
      transferencia: 0,
      deposito: 0,
      tarjeta: 0
    };

    const normalized = (mediosText || '').toUpperCase().replace(/DEPÓSITO/g, 'DEPOSITO');
    const regex = /(YAPE|DEPOSITO|TRANSFERENCIA|TARJETA|EFECTIVO)[^0-9]*S\/\s*([0-9]+(?:[.,][0-9]{1,2})?)/g;
    let match: RegExpExecArray | null;

    while ((match = regex.exec(normalized)) !== null) {
      const medio = match[1];
      const monto = Number((match[2] || '0').replace(',', '.'));
      if (!Number.isFinite(monto) || monto <= 0) {
        continue;
      }

      switch (medio) {
        case 'EFECTIVO':
          result.efectivo += monto;
          break;
        case 'YAPE':
          result.yape += monto;
          break;
        case 'TRANSFERENCIA':
          result.transferencia += monto;
          break;
        case 'DEPOSITO':
          result.deposito += monto;
          break;
        case 'TARJETA':
          result.tarjeta += monto;
          break;
      }
    }

    return result;
  }


  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages || page === this.currentPage) {
      return;
    }
    this.currentPage = page;
    this.scheduleLogsViewportRecalc();
  }

  goToNextPage(): void {
    this.goToPage(this.currentPage + 1);
  }

  goToPrevPage(): void {
    this.goToPage(this.currentPage - 1);
  }

  getLogsViewportHeight(): number | null {
    if (this.pagedLogs.length <= 10) {
      return null;
    }
    return this.logsViewportMaxHeight ?? Math.floor(window.innerHeight * 0.68);
  }

  private matchesSelectedDate(timestamp: Date): boolean {
    const date = new Date(timestamp);
    if (Number.isNaN(date.getTime())) {
      return false;
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const asInputDate = `${year}-${month}-${day}`;

    if (this.selectedDateFrom && asInputDate < this.selectedDateFrom) {
      return false;
    }

    if (this.selectedDateTo && asInputDate > this.selectedDateTo) {
      return false;
    }

    return true;
  }

  private getFriendlyDescription(raw: string, operation: AuditLogEntry['operationType'], entityType: string): string {
    const text = (raw || '').trim();
    if (!text) {
      return `Se registró una acción sobre ${entityType}.`;
    }

    if (!text.includes('Entidad=') && !text.includes('Endpoint=')) {
      return this.removeIdFragments(text);
    }

    switch (operation) {
      case 'INGRESO':
        return 'Se registró un ingreso correctamente.';
      case 'CREATE':
        return `Se creó ${entityType.toLowerCase()} correctamente.`;
      case 'UPDATE':
        return `Se actualizó ${entityType.toLowerCase()} correctamente.`;
      case 'DELETE':
        return `Se eliminó ${entityType.toLowerCase()} correctamente.`;
      case 'PROFORMA':
        return 'Se registró una operación de proforma.';
      default:
        return `Se ejecutó una acción en ${entityType.toLowerCase()}.`;
    }
  }

  private removeIdFragments(text: string): string {
    return (text || '')
      .replace(/\s*\(ID\s*\d+\)/gi, '')
      .replace(/\s+/g, ' ')
      .trim();
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleString('es-ES', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const dropdown = target.closest('.profile-dropdown');
    if (!dropdown && this.showProfileMenu) {
      this.showProfileMenu = false;
    }
  }

  @HostListener('window:resize')
  onResize(): void {
    this.scheduleLogsViewportRecalc();
  }

  toggleProfileMenu(): void {
    this.showProfileMenu = !this.showProfileMenu;
  }

  goToProfile(): void {
    this.showProfileMenu = false;
    setTimeout(() => {
      this.router.navigate(['/profile']);
    }, 100);
  }

  goToDashboard(): void {
    this.showProfileMenu = false;
    setTimeout(() => {
      this.router.navigate(['/dashboard']);
    }, 100);
  }

  logout(): void {
    this.authService.logout();
  }
}
