import { CommonModule } from '@angular/common';
import { Component, ChangeDetectorRef, EventEmitter, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Cliente } from '../models/cliente.model';
import { ClienteService } from '../services/cliente.service';
import { CronogramaService } from '../services/cronograma.service';
import { EtapaService } from '../services/etapa.service';
import { ParcelaService } from '../services/parcela.service';
import { LoteService } from '../services/lote.service';
import { ProjectService } from '../services/project.service';
import { ReniecService } from '../services/reniec.service';
import { Etapa } from '../models/etapa.model';
import { Lote } from '../models/lote.model';
import { Parcela } from '../models/parcela.model';
import { Project } from '../models/project.model';

interface SearchClientResult {
  source: 'USUARIOS' | 'RENIEC';
  dni: string;
  nombres: string;
  apellidos: string;
  fullName: string;
  estado: string;
  existingClientes: Cliente[];
  hasCronograma: boolean;
  telefono?: string;
  email?: string | null;
  direccion?: string;
  projectId?: number | null;
  projectNombre?: string;
  etapaNumero?: number | null;
  parcelaNombre?: string;
  manzana?: string;
  loteNumero?: number | null;
}

@Component({
  selector: 'app-buscar-cliente-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './buscar-cliente-modal.component.html',
  styleUrls: ['./buscar-cliente-modal.component.css']
})
export class BuscarClienteModalComponent {
  @Output() clienteSeleccionado = new EventEmitter<SearchClientResult>();
  @Output() cerrar = new EventEmitter<void>();

  private readonly clienteService = inject(ClienteService);
  private readonly cronogramaService = inject(CronogramaService);
  private readonly etapaService = inject(EtapaService);
  private readonly parcelaService = inject(ParcelaService);
  private readonly reniecService = inject(ReniecService);
  private readonly router = inject(Router);
  private readonly loteService = inject(LoteService);
  private readonly projectService = inject(ProjectService);
  private readonly cdr = inject(ChangeDetectorRef);

  searchSource: 'USUARIOS' | 'RENIEC' = 'USUARIOS';
  searchQuery = '';
  searchResults: SearchClientResult[] = [];
  searchLoading = false;
  searchError = '';

  showProformaActionModal = false;
  selectedClientForAction: SearchClientResult | null = null;
  selectedProformaMode: 'LIBRE' | 'PROYECTO' = 'LIBRE';
  proformaProjects: Project[] = [];
  selectedProformaProjectId: number | null = null;
  loadingProformaProjects = false;

  showAdquisicionModal = false;
  selectedClientForAdquisicion: SearchClientResult | null = null;
  adquisicionSelectionLoading = false;
  adquisicionProjects: Project[] = [];
  adquisicionEtapas: Etapa[] = [];
  adquisicionParcelas: Parcela[] = [];
  adquisicionLotesDisponibles: Lote[] = [];
  adquisicionSelector: { projectId: number | null; etapaId: number | null; parcelaId: number | null; loteId: number | null } = {
    projectId: null,
    etapaId: null,
    parcelaId: null,
    loteId: null
  };

  private searchDebounceTimer: ReturnType<typeof setTimeout> | null = null;

  setSearchSource(source: 'USUARIOS' | 'RENIEC'): void {
    this.searchSource = source;
    this.searchError = '';
    this.searchResults = [];
    this.cancelDebounce();

    if (this.searchSource === 'RENIEC') {
      this.searchQuery = this.searchQuery.replace(/\D/g, '').slice(0, 8);
    }

    if (this.canAutoSearch(this.searchQuery)) {
      this.scheduleAutoSearch();
    }
  }

  onSearchQueryChange(value: string): void {
    this.searchQuery = this.normalizeSearchInput(value);
    this.searchError = '';
    this.searchResults = [];
    this.cancelDebounce();

    if (this.canAutoSearch(this.searchQuery)) {
      this.scheduleAutoSearch();
    }
  }

  onSearchInput(event: Event): void {
    const el = event.target as HTMLInputElement;
    const raw = String(el.value || '');
    const normalized = this.normalizeSearchInput(raw);

    if (normalized !== raw) {
      el.value = normalized;
    }

    this.onSearchQueryChange(normalized);
  }

  executeSearch(): void {
    this.cancelDebounce();
    const query = this.searchQuery.trim();
    this.searchError = '';
    this.searchResults = [];

    if (this.searchSource === 'RENIEC') {
      this.performReniecSearch(query);
      return;
    }

    this.performUsersSearch(query);
  }

  seleccionarCliente(cliente: SearchClientResult): void {
    this.clienteSeleccionado.emit(cliente);
    this.close();
  }

  openProformaForClient(cliente: SearchClientResult): void {
    this.selectedClientForAction = cliente;
    this.selectedProformaMode = 'LIBRE';
    this.selectedProformaProjectId = null;
    this.showProformaActionModal = true;
    this.loadProformaProjects();
  }

  closeProformaActionModal(): void {
    this.showProformaActionModal = false;
    this.selectedClientForAction = null;
    this.selectedProformaMode = 'LIBRE';
    this.selectedProformaProjectId = null;
  }

  confirmProformaAction(): void {
    if (!this.selectedClientForAction) {
      return;
    }

    const nombres = (this.selectedClientForAction.nombres || '').trim();
    const apellidos = (this.selectedClientForAction.apellidos || '').trim();
    const selectedProject = this.proformaProjects.find((project) => project.id === this.selectedProformaProjectId);

    this.router.navigate(['/proformas'], {
      queryParams: {
        quickAction: '1',
        mode: this.selectedProformaMode === 'LIBRE' ? 'libre' : 'proyecto',
        dni: this.selectedClientForAction.dni,
        nombres,
        apellidos,
        projectId: this.selectedProformaMode === 'PROYECTO' && selectedProject ? selectedProject.id : '',
        projectNombre: this.selectedProformaMode === 'PROYECTO' && selectedProject ? selectedProject.nombre : '',
        etapaNumero: this.selectedClientForAction.etapaNumero || '',
        parcelaNombre: this.selectedClientForAction.parcelaNombre || '',
        manzanaNombre: this.selectedClientForAction.manzana || '',
        loteNumero: this.selectedClientForAction.loteNumero || ''
      }
    });

    this.closeProformaActionModal();
    this.close();
  }

  openAdquisicionForClient(cliente: SearchClientResult): void {
    this.selectedClientForAdquisicion = cliente;
    this.showAdquisicionModal = true;
    this.resetAdquisicionSelector();
    this.loadAdquisicionProjects();

    const existing = cliente.existingClientes[0];
    if (existing?.projectId) {
      this.adquisicionSelector.projectId = existing.projectId;
      this.onAdquisicionProjectChange();
      this.adquisicionSelector.etapaId = existing.etapaNumero ? null : this.adquisicionSelector.etapaId;
    }
  }

  closeAdquisicionModal(): void {
    this.showAdquisicionModal = false;
    this.selectedClientForAdquisicion = null;
    this.resetAdquisicionSelector();
  }

  confirmarAdquisicionDesdeBusqueda(): void {
    if (!this.selectedClientForAdquisicion || !this.adquisicionSelector.parcelaId) {
      return;
    }

    const target = this.adquisicionSelector.parcelaId;
    sessionStorage.setItem('sisarovi_pending_adquisicion_client', JSON.stringify({
      source: this.selectedClientForAdquisicion.source,
      dni: this.selectedClientForAdquisicion.dni,
      nombres: this.selectedClientForAdquisicion.nombres,
      apellidos: this.selectedClientForAdquisicion.apellidos,
      fullName: this.selectedClientForAdquisicion.fullName,
      telefono: this.selectedClientForAdquisicion.telefono || '',
      email: this.selectedClientForAdquisicion.email || '',
      direccion: this.selectedClientForAdquisicion.direccion || ''
    }));

    this.closeAdquisicionModal();
    this.close();
    this.router.navigate(['/lotes', target]);
  }

  verCronograma(cliente: SearchClientResult): void {
    const existingCliente = cliente.existingClientes[0] || null;
    this.close();
    this.router.navigate(['/cronogramas'], {
      queryParams: {
        dni: cliente.dni,
        nombres: cliente.fullName,
        projectId: cliente.projectId || '',
        etapaNumero: cliente.etapaNumero || '',
        parcelaNombre: cliente.parcelaNombre || '',
        manzana: cliente.manzana || '',
        loteNumero: cliente.loteNumero || '',
        loteId: existingCliente?.loteId || ''
      }
    });
  }

  async adquirirLote(cliente: SearchClientResult): Promise<void> {
    const baseCliente = cliente.existingClientes[0];
    if (!baseCliente) {
      this.searchError = 'Para adquirir un lote primero debe existir un cliente registrado en el sistema.';
      return;
    }

    try {
      const parcelaId = await this.resolveParcelaId(baseCliente.projectId, baseCliente.etapaNumero, baseCliente.parcelaNombre);
      if (!parcelaId) {
        this.searchError = 'No se pudo ubicar la parcela asociada al cliente para iniciar la adquisición.';
        return;
      }

      sessionStorage.setItem('sisarovi_pending_adquisicion_client', JSON.stringify({
        source: cliente.source,
        dni: cliente.dni,
        nombres: cliente.nombres,
        apellidos: cliente.apellidos,
        fullName: cliente.fullName,
        telefono: cliente.telefono || '',
        email: cliente.email || '',
        direccion: cliente.direccion || ''
      }));

      this.close();
      this.router.navigate(['/lotes', parcelaId]);
    } catch {
      this.searchError = 'No se pudo preparar la adquisición del lote en este momento.';
    }
  }

  realizarProforma(cliente: SearchClientResult): void {
    const clienteBase = cliente.existingClientes[0] || null;
    this.close();
    this.router.navigate(['/proformas'], {
      queryParams: {
        quickAction: '1',
        mode: 'libre',
        dni: cliente.dni,
        nombres: cliente.nombres || cliente.fullName,
        apellidos: cliente.apellidos || '',
        projectId: clienteBase?.projectId ? String(clienteBase.projectId) : '',
        projectNombre: clienteBase?.projectNombre || '',
        etapaNumero: clienteBase?.etapaNumero ? String(clienteBase.etapaNumero) : '',
        parcelaNombre: clienteBase?.parcelaNombre || '',
        manzanaNombre: clienteBase?.manzana || '',
        loteNumero: clienteBase?.loteNumero ? String(clienteBase.loteNumero) : ''
      }
    });
  }

  close(): void {
    this.cancelDebounce();
    this.cerrar.emit();
  }

  getDni(cliente: SearchClientResult | any): string {
    return String(cliente?.dni || '').trim();
  }

  getNombreCompleto(cliente: SearchClientResult | any): string {
    return String(cliente?.fullName || `${this.getNombres(cliente)} ${this.getApellidos(cliente)}`.trim()).trim();
  }

  getNombres(cliente: SearchClientResult | any): string {
    return String(cliente?.nombres || '').trim();
  }

  getApellidos(cliente: SearchClientResult | any): string {
    return String(cliente?.apellidos || '').trim();
  }

  private performUsersSearch(query: string): void {
    if (query.length < 3) {
      this.searchError = 'Ingresa al menos 3 caracteres para buscar en Usuarios.';
      return;
    }

    this.searchLoading = true;
    this.cdr.markForCheck();
    
    forkJoin({
      clientes: this.clienteService.getHistorial(query).pipe(catchError(() => of([] as Cliente[]))),
      cronogramas: this.cronogramaService.listar(this.buildCronogramaFilter(query)).pipe(catchError(() => of([])))
    }).subscribe({
      next: ({ clientes, cronogramas }) => {
        const resultados = this.mapClientesToSearchResults(clientes || [], cronogramas || []);
        this.searchResults = resultados;
        this.searchError = resultados.length === 0 ? 'No se encontraron clientes con ese criterio.' : '';
        this.searchLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.searchLoading = false;
        this.searchError = 'No se pudo completar la búsqueda de clientes.';
        this.cdr.markForCheck();
      }
    });
  }

  private performReniecSearch(query: string): void {
    const dni = query.replace(/\D/g, '').slice(0, 8);
    this.searchQuery = dni;

    if (dni.length !== 8) {
      this.searchError = 'Debes ingresar un DNI válido de 8 dígitos para Reniec.';
      return;
    }

    this.searchLoading = true;
    this.cdr.markForCheck();
    
    forkJoin({
      reniec: this.reniecService.buscarPorDni(dni).pipe(catchError(() => of(null))),
      clientes: this.clienteService.getHistorial(dni).pipe(catchError(() => of([] as Cliente[]))),
      cronogramas: this.cronogramaService.listar({ dni }).pipe(catchError(() => of([])))
    }).subscribe({
      next: ({ reniec, clientes, cronogramas }) => {
        const reniecItems = Array.isArray(reniec) ? reniec : (reniec ? [reniec] : []);
        const reniecResults = reniecItems.map((item) => this.normalizarReniec(item, clientes || [], cronogramas || []));
        const resultados = reniecResults.length > 0 ? reniecResults : this.mapClientesToSearchResults(clientes || [], cronogramas || []);
        this.searchResults = resultados;
        this.searchError = resultados.length === 0 ? 'No existe el DNI ingresado.' : '';
        this.searchLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.searchLoading = false;
        this.searchError = error?.status === 404
          ? 'No existe el DNI ingresado.'
          : this.reniecService.getFriendlyErrorMessage(error);
        this.cdr.markForCheck();
      }
    });
  }

  private mapClientesToSearchResults(clientes: Cliente[], cronogramas: any[]): SearchClientResult[] {
    const grouped = new Map<string, Cliente[]>();

    for (const cliente of clientes || []) {
      const dni = String(cliente?.dni || '').trim();
      if (!dni) {
        continue;
      }

      const bucket = grouped.get(dni) || [];
      bucket.push(cliente);
      grouped.set(dni, bucket);
    }

    return Array.from(grouped.entries()).map(([dni, groupedClientes]) => {
      const first = groupedClientes[0];
      const hasLote = groupedClientes.some((cliente) => Number(cliente?.loteId || 0) > 0);

      return {
        source: 'USUARIOS',
        dni,
        nombres: String(first?.nombres || '').trim(),
        apellidos: String(first?.apellidos || '').trim(),
        fullName: `${first?.nombres || ''} ${first?.apellidos || ''}`.trim(),
        estado: hasLote ? 'Registrado en el sistema' : 'Cliente',
        existingClientes: groupedClientes,
        hasCronograma: hasLote || this.hasCronogramaForDni(cronogramas, dni),
        telefono: String(first?.telefono || '').trim(),
        email: first?.email || '',
        direccion: String(first?.direccion || '').trim(),
        projectId: first?.projectId || null,
        projectNombre: first?.projectNombre || '',
        etapaNumero: first?.etapaNumero || null,
        parcelaNombre: first?.parcelaNombre || '',
        manzana: first?.manzana || '',
        loteNumero: first?.loteNumero || null
      };
    });
  }

  private normalizarReniec(cliente: any, clientes: Cliente[], cronogramas: any[]): SearchClientResult {
    const dni = String(cliente?.dni || cliente?.number || '').trim();
    const nombres = String(cliente?.nombres || cliente?.name || '').trim();
    const apellidos = String(cliente?.surname || `${cliente?.apellidoPaterno || ''} ${cliente?.apellidoMaterno || ''}`.trim()).trim();
    const existingClientes = this.findClientesByDni(clientes, dni);
    const clienteBase = existingClientes[0] || null;
    const hasLote = existingClientes.some((item) => Number(item?.loteId || 0) > 0);

    return {
      source: 'RENIEC',
      dni,
      nombres,
      apellidos,
      fullName: String(cliente?.full_name || `${nombres} ${apellidos}`.trim()).trim(),
      estado: existingClientes.length > 0 ? 'Registrado en el sistema' : 'Consulta RENIEC',
      existingClientes,
      hasCronograma: hasLote || this.hasCronogramaForDni(cronogramas, dni),
      telefono: String(clienteBase?.telefono || '').trim(),
      email: clienteBase?.email || '',
      direccion: String(clienteBase?.direccion || '').trim(),
      projectId: clienteBase?.projectId || null,
      projectNombre: clienteBase?.projectNombre || '',
      etapaNumero: clienteBase?.etapaNumero || null,
      parcelaNombre: clienteBase?.parcelaNombre || '',
      manzana: clienteBase?.manzana || '',
      loteNumero: clienteBase?.loteNumero || null
    };
  }

  private findClientesByDni(clientes: Cliente[], dni: string): Cliente[] {
    const normalized = this.normalizeText(dni);
    return (clientes || []).filter((cliente) => this.normalizeText(cliente?.dni || '') === normalized);
  }

  private loadProformaProjects(): void {
    if (this.loadingProformaProjects) {
      return;
    }

    const cachedProjects = this.projectService.getCachedProjectsSnapshot();
    if (cachedProjects.length > 0) {
      this.proformaProjects = cachedProjects;
    }

    this.loadingProformaProjects = true;
    this.projectService.getAllProjects().subscribe({
      next: (projects) => {
        this.proformaProjects = projects || [];
        this.loadingProformaProjects = false;
      },
      error: () => {
        if (this.proformaProjects.length === 0) {
          this.proformaProjects = cachedProjects;
        }
        this.loadingProformaProjects = false;
      }
    });
  }

  private loadAdquisicionProjects(): void {
    if (this.adquisicionSelectionLoading) {
      return;
    }

    const cachedProjects = this.projectService.getCachedProjectsSnapshot();
    if (cachedProjects.length > 0) {
      this.adquisicionProjects = cachedProjects;
    }

    this.adquisicionSelectionLoading = true;
    this.projectService.getAllProjects().subscribe({
      next: (projects) => {
        this.adquisicionProjects = projects || [];
        this.adquisicionSelectionLoading = false;
      },
      error: () => {
        if (this.adquisicionProjects.length === 0) {
          this.adquisicionProjects = cachedProjects;
        }
        this.adquisicionSelectionLoading = false;
      }
    });
  }

  onAdquisicionProjectChange(): void {
    this.adquisicionSelector.etapaId = null;
    this.adquisicionSelector.parcelaId = null;
    this.adquisicionEtapas = [];
    this.adquisicionParcelas = [];
    this.adquisicionLotesDisponibles = [];

    if (!this.adquisicionSelector.projectId) {
      return;
    }

    this.adquisicionSelectionLoading = true;
    this.etapaService.getEtapasByProject(this.adquisicionSelector.projectId).subscribe({
      next: (etapas) => {
        this.adquisicionEtapas = etapas || [];
        this.adquisicionSelectionLoading = false;
      },
      error: () => {
        this.adquisicionEtapas = [];
        this.adquisicionSelectionLoading = false;
      }
    });
  }

  onAdquisicionEtapaChange(): void {
    this.adquisicionSelector.parcelaId = null;
    this.adquisicionParcelas = [];
    this.adquisicionLotesDisponibles = [];

    if (!this.adquisicionSelector.etapaId) {
      return;
    }

    this.adquisicionSelectionLoading = true;
    this.parcelaService.getParcelasByEtapa(this.adquisicionSelector.etapaId).subscribe({
      next: (parcelas) => {
        this.adquisicionParcelas = parcelas || [];
        this.adquisicionSelectionLoading = false;
      },
      error: () => {
        this.adquisicionParcelas = [];
        this.adquisicionSelectionLoading = false;
      }
    });
  }

  onAdquisicionParcelaChange(): void {
    this.adquisicionSelector.loteId = null;
    this.adquisicionLotesDisponibles = [];

    if (!this.adquisicionSelector.parcelaId) {
      return;
    }

    this.adquisicionSelectionLoading = true;
    this.loteService.getLotesByParcela(this.adquisicionSelector.parcelaId).subscribe({
      next: (lotes) => {
        this.adquisicionLotesDisponibles = (lotes || []).filter((lote) => !lote.adquirido);
        this.adquisicionSelectionLoading = false;
      },
      error: () => {
        this.adquisicionLotesDisponibles = [];
        this.adquisicionSelectionLoading = false;
      }
    });
  }

  private resetAdquisicionSelector(): void {
    this.adquisicionSelector = {
      projectId: null,
      etapaId: null,
      parcelaId: null,
      loteId: null
    };
    this.adquisicionEtapas = [];
    this.adquisicionParcelas = [];
    this.adquisicionLotesDisponibles = [];
  }

  private hasCronogramaForDni(contratos: any[], dni: string): boolean {
    const normalized = this.normalizeText(dni);
    return (contratos || []).some((contrato) => this.normalizeText(contrato?.dni || contrato?.documento || '') === normalized);
  }

  private buildCronogramaFilter(query: string): { dni?: string; nombres?: string } {
    const digits = query.replace(/\D/g, '');
    if (digits.length >= 3) {
      return { dni: digits };
    }
    return { nombres: query.trim() };
  }

  private canAutoSearch(query: string): boolean {
    return this.searchSource === 'USUARIOS' && query.trim().length >= 3;
  }

  private scheduleAutoSearch(): void {
    this.cancelDebounce();
    this.searchDebounceTimer = setTimeout(() => {
      this.executeSearch();
    }, 350);
  }

  private cancelDebounce(): void {
    if (this.searchDebounceTimer) {
      clearTimeout(this.searchDebounceTimer);
      this.searchDebounceTimer = null;
    }
  }

  private normalizeText(value: string): string {
    return String(value || '').trim().toLowerCase();
  }

  private normalizeSearchInput(value: string): string {
    const raw = String(value || '');

    if (this.searchSource === 'RENIEC') {
      return raw.replace(/\D/g, '').slice(0, 8);
    }

    const trimmed = raw.trim();
    if (/^\d+$/.test(trimmed)) {
      return trimmed.slice(0, 8);
    }

    return raw;
  }

  private async resolveParcelaId(projectId: number, etapaNumero: number, parcelaNombre: string): Promise<number | null> {
    if (!projectId || !etapaNumero || !parcelaNombre) {
      return null;
    }

    const etapas = await firstValueFrom(this.etapaService.getEtapasByProject(projectId).pipe(catchError(() => of([]))));
    const etapa = (etapas || []).find((item) => Number(item?.numeroEtapa || 0) === Number(etapaNumero));
    if (!etapa?.id) {
      return null;
    }

    const parcelas = await firstValueFrom(this.parcelaService.getParcelasByEtapa(etapa.id).pipe(catchError(() => of([]))));
    const parcela = (parcelas || []).find((item) => this.normalizeText(item?.nombre || '') === this.normalizeText(parcelaNombre));
    return parcela?.id || null;
  }
}
