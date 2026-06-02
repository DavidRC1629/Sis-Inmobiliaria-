import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { Etapa } from '../../models/etapa.model';
import { ManzanaOption, Parcela } from '../../models/parcela.model';
import { Project } from '../../models/project.model';
import { LiberacionLote } from '../../models/liberacion.model';
import { AuthService } from '../../services/auth.service';
import { AppRefreshService } from '../../services/app-refresh.service';
import { DevolucionCreateRequest } from '../../models/devolucion.model';
import { DevolucionService } from '../../services/devolucion.service';
import { ProjectService } from '../../services/project.service';
import { LiberacionService } from '../../services/liberacion.service';

@Component({
  selector: 'app-liberacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './liberacion.component.clean.html',
  styleUrls: ['./liberacion.component.css']
})
export class LiberacionComponent implements OnInit {
  private router = inject(Router);
  private authService = inject(AuthService);
  private projectService = inject(ProjectService);
  private liberacionService = inject(LiberacionService);
  private devolucionService = inject(DevolucionService);
  private refreshService = inject(AppRefreshService);

  projects: Project[] = [];
  selectedProjectId: number | null = null;
  etapas: Etapa[] = [];
  selectedEtapaId: number | null = null;
  parcelas: Parcela[] = [];
  selectedParcelaId: number | null = null;
  manzanas: ManzanaOption[] = [];
  selectedManzana: string | null = null;

  allLotes: LiberacionLote[] = [];
  lotes: LiberacionLote[] = [];
  private lotesPorProyecto = new Map<number, LiberacionLote[]>();
  loadingProjects = false;
  loadingEtapas = false;
  loadingParcelas = false;
  loadingManzanas = false;
  loadingLotes = false;
  liberandoLoteId: number | null = null;

  notification: { type: 'success' | 'error'; message: string } | null = null;

  devolucionPorLote: Record<number, string> = {};
  devolucionRegistradaPorLote: Record<number, { monto: number; dias: number; descripcion: string }> = {};

  showPasswordModal = false;
  password = '';
  pendingLote: LiberacionLote | null = null;

  showDevolucionModal = false;
  loteParaDevolucion: LiberacionLote | null = null;
  devolucionForm = this.buildDefaultDevolucionForm();

  ngOnInit(): void {
    this.cargarProyectos();
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  logout(): void {
    this.authService.logout();
  }

  cargarProyectos(): void {
    this.loadingProjects = true;
    const cached = this.projectService.getCachedProjectsSnapshot();
    if (cached.length > 0) {
      this.projects = cached.slice().sort((a, b) => this.compareText(a?.nombre, b?.nombre));
      this.loadingProjects = false;
    }

    this.projectService.getAllProjects(true).pipe(
      finalize(() => {
        this.loadingProjects = false;
      })
    ).subscribe({
      next: (projects) => {
        this.projects = (projects || []).slice().sort((a, b) => this.compareText(a?.nombre, b?.nombre));
        this.precargarLotesAdquiridos();
      },
      error: (err) => {
        if (err?.status === 401 || err?.status === 403) {
          this.showNotification('error', 'Tu sesión no tiene permisos para listar proyectos. Vuelve a iniciar sesión.');
          return;
        }

        this.showNotification('error', err?.error?.message || 'No se pudieron cargar los proyectos.');
      }
    });
  }

  onProjectChange(): void {
    this.resetFiltrosDesdeProyecto();
    if (!this.selectedProjectId) {
      return;
    }

    const projectId = this.selectedProjectId;

    this.cargarLotesAdquiridosDelProyecto(projectId);
  }

  onEtapaChange(): void {
    this.resetFiltrosDesdeEtapa();
    if (!this.selectedEtapaId) {
      this.applyFilters();
      return;
    }

    this.loadingParcelas = true;
    this.parcelas = this.construirParcelasDesdeLotes();
    this.loadingParcelas = false;

    this.applyFilters();
  }

  onParcelaChange(): void {
    this.resetFiltrosDesdeParcela();
    if (!this.selectedParcelaId) {
      this.applyFilters();
      return;
    }

    this.loadingManzanas = true;
    this.manzanas = this.construirManzanasDesdeLotes();
    this.loadingManzanas = false;

    this.applyFilters();
  }

  onManzanaChange(): void {
    this.applyFilters();
  }

  limpiarFiltros(): void {
    this.selectedProjectId = null;
    this.resetFiltrosDesdeProyecto();
  }

  abrirDevolucion(lote: LiberacionLote): void {
    this.loteParaDevolucion = lote;
    this.devolucionForm = this.buildDefaultDevolucionForm(lote);
    this.showDevolucionModal = true;
  }

  cerrarDevolucionModal(): void {
    this.showDevolucionModal = false;
    this.loteParaDevolucion = null;
  }

  actualizarFechaFinDevolucion(): void {
    if (!this.devolucionForm.fechaInicio || !this.devolucionForm.dias) {
      this.devolucionForm.fechaFin = this.devolucionForm.fechaInicio;
      return;
    }

    const inicio = new Date(`${this.devolucionForm.fechaInicio}T00:00:00`);
    if (Number.isNaN(inicio.getTime())) {
      return;
    }

    const dias = Math.max(1, Number(this.devolucionForm.dias || 1));
    const fin = new Date(inicio);
    fin.setDate(fin.getDate() + dias - 1);
    this.devolucionForm.fechaFin = fin.toISOString().slice(0, 10);
  }

  confirmarDevolucion(): void {
    if (!this.loteParaDevolucion) {
      return;
    }

    if (!this.devolucionForm.descripcion.trim()) {
      this.showNotification('error', 'La descripción de devolución es obligatoria.');
      return;
    }

    const payload: DevolucionCreateRequest = {
      loteId: this.loteParaDevolucion.loteId,
      loteNumero: this.loteParaDevolucion.loteNumero,
      manzana: this.loteParaDevolucion.manzana,
      parcelaNombre: this.loteParaDevolucion.parcelaNombre,
      etapaNumero: this.loteParaDevolucion.etapaNumero,
      proyectoNombre: this.loteParaDevolucion.projectNombre,
      montoTotal: Number(this.devolucionForm.monto || 0),
      fechaInicio: this.devolucionForm.fechaInicio,
      fechaFinEstimada: this.devolucionForm.fechaFin,
      dias: Number(this.devolucionForm.dias || 1),
      descripcion: this.devolucionForm.descripcion.trim()
    };

    this.devolucionService.crear(payload).subscribe({
      next: (devolucion) => {
        this.devolucionRegistradaPorLote[devolucion.loteId] = {
          monto: Number(devolucion.montoTotal || 0),
          dias: Number(devolucion.dias || 0),
          descripcion: devolucion.descripcion
        };
        this.refreshService.notifyChange();
        this.cerrarDevolucionModal();
        this.showNotification('success', `Devolución registrada para el lote ${devolucion.loteNumero}.`);
      },
      error: (err) => {
        this.showNotification('error', err?.error?.message || 'No se pudo registrar la devolución.');
      }
    });
  }

  liberar(lote: LiberacionLote): void {
    const descripcion = (this.devolucionPorLote[lote.loteId] || '').trim();
    if (!descripcion) {
      this.showNotification('error', `Debes registrar la descripción de devolución para el lote ${lote.loteNumero}.`);
      return;
    }

    if (lote.requierePasswordAdmin) {
      this.pendingLote = lote;
      this.password = '';
      this.showPasswordModal = true;
      return;
    }

    this.ejecutarLiberacion(lote, undefined);
  }

  confirmarLiberacionConPassword(): void {
    if (!this.pendingLote) {
      this.cerrarPasswordModal();
      return;
    }

    const pass = this.password.trim();
    if (!pass) {
      this.showNotification('error', 'Ingresa la contraseña de administrador para continuar.');
      return;
    }

    this.ejecutarLiberacion(this.pendingLote, pass);
    this.cerrarPasswordModal();
  }

  cerrarPasswordModal(): void {
    this.showPasswordModal = false;
    this.password = '';
    this.pendingLote = null;
  }

  private ejecutarLiberacion(lote: LiberacionLote, adminPassword?: string): void {
    const descripcion = (this.devolucionPorLote[lote.loteId] || '').trim();

    this.liberandoLoteId = lote.loteId;
    this.liberacionService.liberarLote(lote.loteId, {
      descripcion,
      adminPassword
    }).subscribe({
      next: () => {
        this.liberandoLoteId = null;
        this.allLotes = this.allLotes.filter((x) => x.loteId !== lote.loteId);
        this.applyFilters();
        delete this.devolucionPorLote[lote.loteId];
        delete this.devolucionRegistradaPorLote[lote.loteId];
        this.refreshService.notifyChange();
        this.showNotification('success', `Lote ${lote.loteNumero} liberado correctamente y disponible para nueva adquisición.`);
      },
      error: (err) => {
        this.liberandoLoteId = null;
        this.showNotification('error', err?.error?.message || 'No se pudo liberar el lote.');
      }
    });
  }

  estadoClass(estadoVisual: string): string {
    const value = (estadoVisual || '').toLowerCase();
    if (value.includes('separ')) {
      return 'estado-separacion';
    }
    if (value.includes('cuotas')) {
      return 'estado-cuotas';
    }
    if (value.includes('al día') || value.includes('al dia')) {
      return 'estado-aldia';
    }
    return 'estado-default';
  }

  formatMoney(value: number | null | undefined): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(Number(value || 0));
  }

  get filtrosCompletos(): boolean {
    return !!this.selectedProjectId && !!this.selectedEtapaId && !!this.selectedParcelaId && !!this.selectedManzana;
  }

  get proyectoSeleccionado(): Project | null {
    return this.projects.find((project) => project.id === this.selectedProjectId) || null;
  }

  get etapaSeleccionada(): Etapa | null {
    return this.etapas.find((etapa) => etapa.id === this.selectedEtapaId) || null;
  }

  get parcelaSeleccionada(): Parcela | null {
    return this.parcelas.find((parcela) => parcela.id === this.selectedParcelaId) || null;
  }

  get manzanaSeleccionada(): string {
    return this.selectedManzana || '';
  }

  get estadoFiltros(): string {
    if (!this.selectedProjectId) {
      return 'Selecciona un proyecto para comenzar.';
    }

    if (!this.selectedEtapaId) {
      return 'Selecciona una etapa para continuar.';
    }

    if (!this.selectedParcelaId) {
      return 'Selecciona una parcela para continuar.';
    }

    if (!this.selectedManzana) {
      return 'Selecciona una manzana para ver los lotes adquiridos.';
    }

    return `${this.lotes.length} lote(s) adquirido(s) disponibles en la selección actual.`;
  }

  trackByLoteId(_: number, lote: LiberacionLote): number {
    return lote.loteId;
  }

  private showNotification(type: 'success' | 'error', message: string): void {
    this.notification = { type, message };
    setTimeout(() => {
      this.notification = null;
    }, 3500);
  }

  private buildDefaultDevolucionForm(lote?: LiberacionLote): { monto: number; fechaInicio: string; fechaFin: string; dias: number; descripcion: string } {
    const today = new Date();
    const inicio = today.toISOString().slice(0, 10);
    const fin = new Date(today);
    fin.setDate(fin.getDate() + 6);

    return {
      monto: lote?.montoPagadoTotal || 0,
      fechaInicio: inicio,
      fechaFin: fin.toISOString().slice(0, 10),
      dias: 7,
      descripcion: ''
    };
  }

  private applyFilters(): void {
    this.lotes = this.allLotes.filter((lote) => this.matchesCurrentSelection(lote));
  }

  private matchesCurrentSelection(lote: LiberacionLote): boolean {
    if (!this.filtrosCompletos) {
      return false;
    }

    if (this.selectedProjectId != null && lote.projectId !== this.selectedProjectId) {
      return false;
    }

    if (this.selectedEtapaId != null && lote.etapaId != null && lote.etapaId !== this.selectedEtapaId) {
      return false;
    }

    if (this.selectedEtapaId != null && lote.etapaId == null && this.etapaSeleccionada?.numeroEtapa != null) {
      if ((lote.etapaNumero || 0) !== this.etapaSeleccionada.numeroEtapa) {
        return false;
      }
    }

    if (this.selectedParcelaId != null && lote.parcelaId != null && lote.parcelaId !== this.selectedParcelaId) {
      return false;
    }

    if (this.selectedParcelaId != null && lote.parcelaId == null && this.parcelaSeleccionada?.nombre) {
      if (this.normalizeText(lote.parcelaNombre) !== this.normalizeText(this.parcelaSeleccionada.nombre)) {
        return false;
      }
    }

    if (this.selectedManzana && this.normalizeText(lote.manzana) !== this.normalizeText(this.selectedManzana)) {
      return false;
    }

    return true;
  }

  private resetFiltrosDesdeProyecto(): void {
    this.etapas = [];
    this.parcelas = [];
    this.manzanas = [];
    this.selectedEtapaId = null;
    this.selectedParcelaId = null;
    this.selectedManzana = null;
    this.allLotes = [];
    this.lotes = [];
    this.devolucionPorLote = {};
    this.liberandoLoteId = null;
    this.loadingEtapas = false;
    this.loadingParcelas = false;
    this.loadingManzanas = false;
    this.loadingLotes = false;
    this.cerrarPasswordModal();
  }

  private resetFiltrosDesdeEtapa(): void {
    this.parcelas = [];
    this.manzanas = [];
    this.selectedParcelaId = null;
    this.selectedManzana = null;
    this.loadingParcelas = false;
    this.loadingManzanas = false;
  }

  private resetFiltrosDesdeParcela(): void {
    this.manzanas = [];
    this.selectedManzana = null;
    this.loadingManzanas = false;
  }

  private normalizeText(value: string | null | undefined): string {
    return (value || '').trim().toLowerCase();
  }

  private compareText(a: string | null | undefined, b: string | null | undefined): number {
    return this.normalizeText(a).localeCompare(this.normalizeText(b), 'es');
  }

  private precargarLotesAdquiridos(): void {
    if (!this.projects.length) {
      return;
    }

    const requests = this.projects.map((project) =>
      this.liberacionService.listarLotesAdquiridos(project.id).pipe(
        catchError(() => of([] as LiberacionLote[]))
      )
    );

    forkJoin(requests).subscribe({
      next: (resultados) => {
        resultados.forEach((lotes, index) => {
          const project = this.projects[index];
          if (project?.id != null) {
            this.lotesPorProyecto.set(project.id, Array.isArray(lotes) ? lotes : []);
          }
        });

        if (this.selectedProjectId != null) {
          this.cargarLotesAdquiridosDelProyecto(this.selectedProjectId);
        }
      }
    });
  }

  private cargarLotesAdquiridosDelProyecto(projectId: number): void {
    const cached = this.lotesPorProyecto.get(projectId);
    if (cached) {
      this.aplicarLotesDelProyecto(projectId, cached);
      return;
    }

    this.liberacionService.listarLotesAdquiridos(projectId).subscribe({
      next: (data) => {
        this.lotesPorProyecto.set(projectId, Array.isArray(data) ? data : []);
        if (this.selectedProjectId === projectId) {
          this.aplicarLotesDelProyecto(projectId, data || []);
        }
      },
      error: (err) => {
        if (this.selectedProjectId !== projectId) {
          return;
        }

        this.showNotification('error', err?.error?.message || 'No se pudieron cargar los lotes adquiridos.');
      }
    });
  }

  private aplicarLotesDelProyecto(projectId: number, lotes: LiberacionLote[]): void {
    if (this.selectedProjectId !== projectId) {
      return;
    }

    this.allLotes = Array.isArray(lotes) ? lotes : [];
    this.etapas = this.construirEtapasDesdeLotes();
    this.applyFilters();
  }

  private construirEtapasDesdeLotes(): Etapa[] {
    const byNumero = new Map<number, Etapa>();

    for (const lote of this.allLotes) {
      const numero = lote?.etapaNumero;
      if (numero == null) {
        continue;
      }

      if (!byNumero.has(numero)) {
        byNumero.set(numero, {
          id: lote?.etapaId != null ? Number(lote.etapaId) : numero,
          numeroEtapa: numero,
          cantidadParcelas: 0,
          projectId: this.selectedProjectId || 0
        });
      }
    }

    return Array.from(byNumero.values()).sort((a, b) => (a.numeroEtapa || 0) - (b.numeroEtapa || 0));
  }

  private construirParcelasDesdeLotes(): Parcela[] {
    const byParcela = new Map<string, Parcela>();
    const etapaNumero = this.etapaSeleccionada?.numeroEtapa;

    if (etapaNumero == null) {
      return [];
    }

    let syntheticId = -1;
    for (const lote of this.allLotes) {
      if ((lote?.etapaNumero || 0) !== etapaNumero) {
        continue;
      }

      const nombre = this.normalizeText(lote?.parcelaNombre || '');
      if (!nombre) {
        continue;
      }

      const key = `${lote?.parcelaId ?? 's'}|${nombre}`;
      if (byParcela.has(key)) {
        continue;
      }

      byParcela.set(key, {
        id: lote?.parcelaId != null ? Number(lote.parcelaId) : syntheticId--,
        nombre: lote.parcelaNombre,
        numManzanas: 0,
        propietario: '',
        cantidadLotes: 0,
        lotesDisponibles: 0,
        etapaId: this.selectedEtapaId || 0
      });
    }

    return Array.from(byParcela.values()).sort((a, b) => this.compareText(a?.nombre, b?.nombre));
  }

  private construirManzanasDesdeLotes(): ManzanaOption[] {
    const set = new Set<string>();
    const manzanas: ManzanaOption[] = [];

    const parcela = this.parcelaSeleccionada;
    const etapaNumero = this.etapaSeleccionada?.numeroEtapa;
    if (!parcela || etapaNumero == null) {
      return [];
    }

    for (const lote of this.allLotes) {
      if ((lote?.etapaNumero || 0) !== etapaNumero) {
        continue;
      }

      const sameParcelaById = parcela.id > 0 && lote?.parcelaId != null && Number(lote.parcelaId) === parcela.id;
      const sameParcelaByName = this.normalizeText(lote?.parcelaNombre) === this.normalizeText(parcela.nombre);
      if (!sameParcelaById && !sameParcelaByName) {
        continue;
      }

      const nombre = (lote?.manzana || '').trim();
      const key = this.normalizeText(nombre);
      if (!key || set.has(key)) {
        continue;
      }

      set.add(key);
      manzanas.push({ id: manzanas.length + 1, nombre });
    }

    return manzanas.sort((a, b) => this.compareText(a?.nombre, b?.nombre));
  }
}
