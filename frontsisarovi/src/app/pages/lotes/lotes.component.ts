import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy, ApplicationRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { LoteService } from '../../services/lote.service';
import { ParcelaService } from '../../services/parcela.service';
import { ClienteService } from '../../services/cliente.service';
import { ProjectService } from '../../services/project.service';
import { ReniecService } from '../../services/reniec.service';
import { CronogramaService } from '../../services/cronograma.service';
import { EtapaService } from '../../services/etapa.service';
import { Lote, LoteRequest } from '../../models/lote.model';
import { ManzanaOption, Parcela } from '../../models/parcela.model';
import { Cliente, ClienteAdquisicionRequest, PropietarioRequest } from '../../models/cliente.model';
import { Project } from '../../models/project.model';
import { Etapa } from '../../models/etapa.model';
import { ReniecData } from '../../models/user.model';

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
}

interface AdquisicionSelectorState {
  projectId: number | null;
  etapaId: number | null;
  parcelaId: number | null;
  manzanaId: number | null;
  manzana: string;
  loteId: number | null;
}

@Component({
  selector: 'app-lotes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lotes.component.html',
  styleUrls: ['./lotes.component.css'],
  changeDetection: ChangeDetectionStrategy.Default
})
export class LotesComponent implements OnInit {
  private loteService = inject(LoteService);
  private parcelaService = inject(ParcelaService);
  private clienteService = inject(ClienteService);
  private projectService = inject(ProjectService);
  private etapaService = inject(EtapaService);
  private reniecService = inject(ReniecService);
  private cronogramaService = inject(CronogramaService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private appRef = inject(ApplicationRef);

  adquirirLote(lote: Lote): void {
    if (this.isLoteAdquirido(lote)) {
      this.showErrorMessage(`El lote ${lote.numero} ya está adquirido. ${this.propietariosLoteTexto(lote)}`);
      return;
    }

    this.prepareAdquisicionModal();
    this.adquisicionRequiresLoteSelection = false;
    this.selectedLoteForAdquisicion = lote;
    this.aplicarPrecioLotePorDefecto(lote);
    this.showAdquisicionModal = true;
    this.cdr.detectChanges();
  }
  parcelaId!: number;
  parcela: Parcela | null = null;
  lotes: Lote[] = [];
  selectedManzana: string = 'Todas';
  manzanas: string[] = [];
  manzanaOptions: ManzanaOption[] = [];
  loading: boolean = false;
  showModal: boolean = false;
  modalMode: 'create' | 'edit' = 'create';
  modalTitle: string = '';
  currentLote: Lote | null = null;
  manzanaLocked: boolean = false;
  numeroPartidaDuplicada: boolean = false;
  numeroPartidaValidando: boolean = false;
  private numeroPartidaCheckTimeout: any;
  private pendingAdquisicionChecked = false;

  // Búsqueda de clientes
  showSearchModal: boolean = false;
  searchSource: 'USUARIOS' | 'RENIEC' = 'USUARIOS';
  searchQuery: string = '';
  searchResults: SearchClientResult[] = [];
  searchLoading: boolean = false;
  searchError: string = '';
  private searchTimeout: any;
  showProformaActionModal = false;
  selectedClientForAction: SearchClientResult | null = null;
  selectedProformaMode: 'LIBRE' | 'PROYECTO' = 'LIBRE';
  proformaProjects: Project[] = [];
  selectedProformaProjectId: number | null = null;
  loadingProformaProjects = false;

  showAdquisicionModal = false;
  adquisicionSaving = false;
  selectedLoteForAdquisicion: Lote | null = null;
  adquisicionErrors = '';
  adquisicionStep: 1 | 2 = 1;
  viewMode: 'view' | 'edit' | 'delete' = 'view';
  showInfoModal = false;
  selectedLoteForInfo: Lote | null = null;
  infoClientesDelLote: Cliente[] = [];
  clientesPorLote: Record<number, Cliente[]> = {};
  infoLoading = false;
  infoError = '';
  adquisicionRequiresLoteSelection = false;
  adquisicionSelectionLoading = false;
  adquisicionProjects: Project[] = [];
  adquisicionEtapas: Etapa[] = [];
  adquisicionParcelas: Parcela[] = [];
  adquisicionManzanasDisponibles: ManzanaOption[] = [];
  adquisicionLotesDisponibles: Lote[] = [];
  selectedSearchClientForAdquisicion: SearchClientResult | null = null;
  adquisicionSelector: AdquisicionSelectorState = {
    projectId: null,
    etapaId: null,
    parcelaId: null,
    manzanaId: null,
    manzana: '',
    loteId: null
  };
  adquisicionForm: {
    tipoOperacion: 'CONTADO' | 'CREDITO' | 'SEPARACION';
    fechaOperacion: string;
    asesor: string;
    medios: Array<{ id: number; medio: 'YAPE' | 'DEPOSITO' | 'TRANSFERENCIA' | 'TARJETA' | 'EFECTIVO'; monto: number; efectivoEntregado: number }>;
    _nextMedioId: number;
    precioVenta: number;
    montoOperacion: number;
    montoSeparacionObjetivo: number;
    plazoMeses: number;
    interesPorcentaje: number;
    propietariosCount: number;
    propietarios: PropietarioRequest[];
  } = {
    tipoOperacion: 'CONTADO',
    fechaOperacion: '',
    asesor: '',
    medios: [{ id: 0, medio: 'EFECTIVO', monto: 0, efectivoEntregado: 0 }],
    _nextMedioId: 1,
    precioVenta: 0,
    montoOperacion: 0,
    montoSeparacionObjetivo: 2000,
    plazoMeses: 24,
    interesPorcentaje: 10,
    propietariosCount: 1,
    propietarios: []
  };

  adquisicionPrecioVentaInput = '0';
  adquisicionMontoOperacionInput = '0';
  adquisicionMontoSeparacionInput = '2,000';

  formData: LoteRequest = {
    numero: 1,
    calle: '',
    perimetro: 0,
    areaM2: 0,
    medidaFrente: 0,
    medidaIzquierda: 0,
    medidaDerecha: 0,
    medidaFondo: 0,
    numeroPartida: '',
    precioLote: 0,
    manzanaId: null,
    manzana: 'A'
  };

  formErrors: any = {
    numero: '',
    calle: '',
    perimetro: '',
    areaM2: '',
    medidaFrente: '',
    medidaIzquierda: '',
    medidaDerecha: '',
    medidaFondo: '',
    numeroPartida: '',
    precioLote: '',
    manzana: '',
    general: ''
  };

  ngOnInit(): void {
    try {
      const parcelaIdParam = this.route.snapshot.params['parcelaId'];
      this.parcelaId = +parcelaIdParam;
      
      if (this.parcelaId && !isNaN(this.parcelaId)) {
        this.loadParcelaInfo();
        this.loadManzanas();
        this.loadLotes();
      } else {
        console.error('ParcelaId invalido:', parcelaIdParam);
        this.router.navigate(['/dashboard']);
      }
      
      this.route.params.subscribe(params => {
        const newParcelaId = +params['parcelaId'];
        if (newParcelaId !== this.parcelaId && !isNaN(newParcelaId)) {
          this.parcelaId = newParcelaId;
          this.loadParcelaInfo();
          this.loadManzanas();
          this.loadLotes();
        }
      });

      this.tryOpenPendingAdquisicionFromDashboard();
    } catch (error) {
      console.error('Error en ngOnInit:', error);
      this.router.navigate(['/dashboard']);
    }
  }

  loadParcelaInfo(): void {
    this.parcelaService.getParcelaById(this.parcelaId).subscribe({
      next: (parcela: Parcela) => {
        this.parcela = parcela;
        this.generateManzanasFromParcela();
        setTimeout(() => {
          this.cdr.detectChanges();
        }, 0);
      },
      error: (error: any) => {
        console.error('Error al cargar informacion de parcela:', error);
        // Generar manzanas por defecto en caso de error
        this.manzanas = ['Todas', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
        this.showErrorMessage('Error al cargar informacion de la parcela');
      }
    });
  }

  loadManzanas(): void {
    this.parcelaService.getManzanasByParcela(this.parcelaId).subscribe({
      next: (options: ManzanaOption[]) => {
        this.manzanaOptions = options;

        if (options.length > 0) {
          this.manzanas = [
            'Todas',
            ...options
              .map(option => this.normalizeManzana(option.nombre))
              .sort((a, b) => this.compareManzanaLabel(a, b))
          ];
        }

        this.formData.manzanaId = this.findManzanaIdByLabel(this.formData.manzana);
        this.cdr.markForCheck();
      },
      error: (error: any) => {
        console.error('Error al cargar manzanas:', error);
        this.manzanaOptions = [];
      }
    });
  }

  loadLotes(): void {
    this.loading = true;
    this.lotes = [];
    this.cdr.markForCheck();
    
    this.loteService.getLotesByParcela(this.parcelaId).subscribe({
      next: (data: Lote[]) => {
        setTimeout(() => {
          this.lotes = data;
          this.cargarEstadoAdquisicionLotes();
          // NO llamar generateManzanas() - ya se generaron desde la parcela
          this.loading = false;
          this.cdr.markForCheck();
          this.cdr.detectChanges();
        }, 0);
      },
      error: (error: any) => {
        console.error('Error al cargar lotes:', error);
        this.showErrorMessage('Error al cargar los lotes');
        this.loading = false;
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  trackByLoteId(index: number, lote: Lote): number {
    return lote.id;
  }

  generateManzanasFromParcela(): void {
    try {
      if (!this.parcela) {
        console.warn('No hay informacion de parcela disponible');
        return;
      }
      
      const numManzanas = this.parcela.numManzanas || 0;
      const manzanasArray: string[] = ['Todas'];
      
      // Generar letras de manzanas de manera ascendente: A, B, C... F, G, H
      for (let i = 0; i < numManzanas; i++) {
        manzanasArray.push(this.obtenerLetraManzana(i));
      }
      
      this.manzanas = manzanasArray;
      console.log('Manzanas generadas:', this.manzanas);
    } catch (error) {
      console.error('Error al generar manzanas:', error);
      this.manzanas = ['Todas', 'A'];
    }
  }

  obtenerLetraManzana(indice: number): string {
    if (indice < 26) {
      return String.fromCharCode(65 + indice); // A-Z
    } else {
      const primera = Math.floor(indice / 26) - 1;
      const segunda = indice % 26;
      return String.fromCharCode(65 + primera) + String.fromCharCode(65 + segunda);
    }
  }

  generateManzanas(): void {
    const manzanasSet = new Set(this.lotes.map(lote => lote.manzana));
    this.manzanas = ['Todas', ...Array.from(manzanasSet).sort().reverse()];
  }

  // Obtiene lista de manzanas sin "Todas" para mostrar verticalmente
  getManzanasDescendentes(): string[] {
    return this.manzanas
      .filter(m => m !== 'Todas')
      .sort((a, b) => this.compareManzanaLabel(a, b));
  }

  // Obtiene lotes de una manzana especifica
  getLotesByManzana(manzana: string): Lote[] {
    return this.lotes
      .filter(lote => lote.manzana === manzana)
      .sort((a, b) => a.numero - b.numero); // Ordenar por numero ascendente
  }

  filterLotesByManzana(): Lote[] {
    let lotesToShow: Lote[] = [];
    
    if (this.selectedManzana === 'Todas') {
      lotesToShow = [...this.lotes];
    } else {
      lotesToShow = this.lotes.filter(lote => lote.manzana === this.selectedManzana);
    }
    
    // Ordenar de manera descendente por manzana (H, G, F... C, B, A) y luego por numero descendente
    return lotesToShow.sort((a, b) => {
      // Primero comparar por manzana (descendente)
      const manzanaCompare = b.manzana.localeCompare(a.manzana);
      if (manzanaCompare !== 0) {
        return manzanaCompare;
      }
      // Si son de la misma manzana, ordenar por nÃºmero (descendente)
      return b.numero - a.numero;
    });
  }

  openCreateModal(): void {
    this.modalMode = 'create';
    this.modalTitle = 'Nuevo Lote';
    this.resetForm();
    // Bloquear manzana y usar la primera disponible (después de 'Todas')
    const primeraDisponible = this.manzanas.length > 1 ? this.manzanas[1] : 'A';
    this.setSelectedManzana(primeraDisponible);
    this.applySuggestedNumeroForCurrentManzana();
    this.manzanaLocked = true;
    console.log('Nuevo Lote - Manzana bloqueada:', this.manzanaLocked, 'Manzana:', primeraDisponible);
    this.showModal = true;
    this.cdr.detectChanges();
  }

  setViewMode(mode: 'edit' | 'delete'): void {
    this.viewMode = this.viewMode === mode ? 'view' : mode;
  }

  openInfoLote(lote: Lote): void {
    this.selectedLoteForInfo = lote;
    this.showInfoModal = true;
    this.infoLoading = true;
    this.infoError = '';
    this.infoClientesDelLote = [];
    this.refreshInfoModalView();

    if (!lote.projectId) {
      this.infoLoading = false;
      this.infoError = 'No se pudo determinar el proyecto de este lote.';
      this.refreshInfoModalView();
      return;
    }

    this.clienteService.getClientesByProject(lote.projectId).subscribe({
      next: (clientes: Cliente[]) => {
        this.infoClientesDelLote = clientes.filter(cliente => cliente.loteId === lote.id);
        this.reemplazarClientesPorProyecto(clientes);
        this.infoLoading = false;
        this.refreshInfoModalView();
      },
      error: () => {
        this.infoLoading = false;
        this.infoError = 'No se pudo consultar el estado de adquisición del lote.';
        this.refreshInfoModalView();
      }
    });
  }

  closeInfoModal(): void {
    this.showInfoModal = false;
    this.selectedLoteForInfo = null;
    this.infoClientesDelLote = [];
    this.infoLoading = false;
    this.infoError = '';
    this.refreshInfoModalView();
  }

  private refreshInfoModalView(): void {
    this.cdr.markForCheck();
    this.cdr.detectChanges();
    this.appRef.tick();
  }

  private cargarEstadoAdquisicionLotes(): void {
    if (!this.lotes.length) {
      this.clientesPorLote = {};
      this.refreshLotesView();
      return;
    }

    const loteIds = new Set(this.lotes.map((lote) => Number(lote.id)).filter((id) => id > 0));
    const cachedClientes = this.clienteService.getHistorialSnapshot('');
    if (cachedClientes.length > 0) {
      const cachedRelacionados = cachedClientes.filter((cliente) => loteIds.has(Number(cliente?.loteId || 0)));
      this.reemplazarClientesPorProyecto(cachedRelacionados);
    }

    const projectId = this.lotes.find((l) => l.projectId)?.projectId;
    if (!projectId) {
      if (cachedClientes.length === 0) {
        this.clientesPorLote = {};
        this.refreshLotesView();
      }
      return;
    }

    this.clienteService.getClientesByProject(projectId).subscribe({
      next: (clientes) => this.reemplazarClientesPorProyecto(clientes || []),
      error: () => {
        if (cachedClientes.length === 0) {
          this.clientesPorLote = {};
        }
        this.refreshLotesView();
      }
    });
  }

  private reemplazarClientesPorProyecto(clientes: Cliente[]): void {
    const grouped: Record<number, Cliente[]> = {};
    for (const cliente of clientes || []) {
      const loteId = Number(cliente?.loteId || 0);
      if (loteId <= 0) {
        continue;
      }
      if (!grouped[loteId]) {
        grouped[loteId] = [];
      }
      grouped[loteId].push(cliente);
    }
    this.clientesPorLote = grouped;
    this.refreshLotesView();
  }

  private refreshLotesView(): void {
    this.cdr.markForCheck();
    this.cdr.detectChanges();
    queueMicrotask(() => this.appRef.tick());
  }

  isLoteAdquirido(lote: Lote | null): boolean {
    if (!lote) {
      return false;
    }
    return (this.clientesPorLote[Number(lote.id)] || []).length > 0;
  }

  propietariosLoteTexto(lote: Lote | null): string {
    if (!lote) {
      return '';
    }
    const clientes = this.clientesPorLote[Number(lote.id)] || [];
    const nombresUnicos = Array.from(new Set(clientes.map((c) => `${c.nombres} ${c.apellidos}`.trim()).filter(Boolean)));
    return nombresUnicos.join(', ');
  }

  formatMontoConComas(value: number | null | undefined): string {
    const num = Number(value || 0);
    return num.toLocaleString('en-US', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2
    });
  }

  openCreateModalForManzana(manzana: string): void {
    this.modalMode = 'create';
    this.modalTitle = `Nuevo Lote en Manzana ${manzana}`;
    this.resetForm();
    this.setSelectedManzana(manzana);
    this.applySuggestedNumeroForCurrentManzana();
    this.manzanaLocked = true;
    console.log('Manzana bloqueada:', this.manzanaLocked, 'Manzana:', manzana);
    this.showModal = true;
    this.cdr.detectChanges();
  }

  openEditModal(lote: Lote): void {
    this.modalMode = 'edit';
    this.modalTitle = 'Editar Lote';
    this.currentLote = lote;
    this.formData = {
      numero: lote.numero,
      calle: lote.calle,
      perimetro: lote.perimetro,
      areaM2: lote.areaM2,
      medidaFrente: lote.medidaFrente,
      medidaIzquierda: lote.medidaIzquierda,
      medidaDerecha: lote.medidaDerecha,
      medidaFondo: lote.medidaFondo,
      numeroPartida: lote.numeroPartida,
      precioLote: Number(lote.precioLote || 0),
      manzanaId: lote.manzanaId ?? this.findManzanaIdByLabel(lote.manzana),
      manzana: lote.manzana
    };
    this.manzanaLocked = true;
    console.log('Modo edicion - Manzana bloqueada:', this.manzanaLocked, 'Manzana:', lote.manzana);
    this.showModal = true;
    this.cdr.detectChanges();
  }

  closeModal(): void {
    this.showModal = false;
    this.clearNumeroPartidaCheck();
    this.resetForm();
    this.currentLote = null;
    this.manzanaLocked = false;
  }

  resetForm(): void {
    this.formData = {
      numero: 1,
      calle: '',
      perimetro: 0,
      areaM2: 0,
      medidaFrente: 0,
      medidaIzquierda: 0,
      medidaDerecha: 0,
      medidaFondo: 0,
      numeroPartida: '',
      precioLote: 0,
      manzanaId: null,
      manzana: 'A'
    };
    this.formErrors = {
      numero: '',
      calle: '',
      perimetro: '',
      areaM2: '',
      medidaFrente: '',
      medidaIzquierda: '',
      medidaDerecha: '',
      medidaFondo: '',
      numeroPartida: '',
      precioLote: '',
      manzana: '',
      general: ''
    };
    this.numeroPartidaDuplicada = false;
    this.numeroPartidaValidando = false;
  }

  validateForm(): boolean {
    let valid = true;
    this.formErrors = {
      numero: '',
      calle: '',
      perimetro: '',
      areaM2: '',
      medidaFrente: '',
      medidaIzquierda: '',
      medidaDerecha: '',
      medidaFondo: '',
      numeroPartida: '',
      manzana: '',
      general: ''
    };

    if (!this.formData.numero || this.formData.numero < 1) {
      this.formErrors.numero = 'El numero de lote es requerido y debe ser mayor a 0';
      valid = false;
    }

    // Validar si el numero de lote ya existe en esta manzana
    if (this.formData.numero && this.formData.manzana) {
      const loteExistente = this.lotes.find(l => 
        l.manzana === this.formData.manzana && 
        l.numero === this.formData.numero &&
        (this.modalMode === 'create' || l.id !== this.currentLote?.id)
      );
      if (loteExistente) {
        this.formErrors.numero = `Ya existe el Lote ${this.formData.numero} en la Manzana ${this.formData.manzana}`;
        valid = false;
      }
    }

    if (!this.formData.numeroPartida || this.formData.numeroPartida.trim() === '') {
      this.formErrors.numeroPartida = 'El número de partida es obligatorio';
      valid = false;
    }

    const numeroPartida = (this.formData.numeroPartida || '').trim();
    if (numeroPartida && !/^\d+$/.test(numeroPartida)) {
      this.formErrors.numeroPartida = 'El número de partida solo debe contener números';
      valid = false;
    }

    if (numeroPartida && numeroPartida.length > 8) {
      this.formErrors.numeroPartida = 'El número de partida debe tener como máximo 8 dígitos';
      valid = false;
    }

    // Validar si el numero de partida ya existe en la vista actual
    if (numeroPartida) {
      const partidaExistente = this.lotes.find(l => 
        l.numeroPartida === numeroPartida &&
        (this.modalMode === 'create' || l.id !== this.currentLote?.id)
      );
      if (partidaExistente) {
        this.formErrors.numeroPartida = `El Numero de Partida ${numeroPartida} ya esta registrado`;
        valid = false;
      }

      if (this.numeroPartidaDuplicada) {
        this.formErrors.numeroPartida = `El número de partida ${this.formData.numeroPartida.trim()} ya existe y no se puede repetir`;
        valid = false;
      }
    }

    if (!this.formData.precioLote || Number(this.formData.precioLote) <= 0) {
      this.formErrors.precioLote = 'El precio del lote es obligatorio y debe ser mayor a 0';
      valid = false;
    }

    if (!this.formData.calle || this.formData.calle.trim() === '') {
      this.formErrors.calle = 'La calle es requerida';
      valid = false;
    }

    if (!this.formData.manzana || this.formData.manzana.trim() === '') {
      this.formErrors.manzana = 'La manzana es requerida';
      valid = false;
    }

    if (!this.formData.manzanaId) {
      this.formData.manzanaId = this.findManzanaIdByLabel(this.formData.manzana);
    }

    if (!this.formData.manzanaId) {
      this.formErrors.manzana = 'No se pudo identificar la manzana seleccionada';
      valid = false;
    }

    return valid;
  }


  onSubmit(): void {
    this.formData.manzanaId = this.findManzanaIdByLabel(this.formData.manzana);
    this.formData.numeroPartida = (this.formData.numeroPartida || '').trim();
    this.formData.precioLote = Number(this.formData.precioLote || 0);

    if (!this.validateForm()) {
      return;
    }

    this.loading = true;

    if (this.modalMode === 'create') {
      this.loteService.createLote(this.parcelaId, this.formData).subscribe({
        next: (response: Lote) => {
          this.showSuccessMessage('Lote creado exitosamente');
          this.loadLotes();
          this.closeModal();
        },
        error: (error: any) => {
          console.error('Error al crear lote:', error);
          this.formErrors.general = error.error?.message || 'Error al crear el lote';
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    } else {
      if (this.currentLote) {
        this.loteService.updateLote(this.parcelaId, this.currentLote.id, this.formData).subscribe({
          next: (response: Lote) => {
            this.showSuccessMessage('Lote actualizado exitosamente');
            this.loadLotes();
            this.closeModal();
          },
          error: (error: any) => {
            console.error('Error al actualizar lote:', error);
            this.formErrors.general = error.error?.message || 'Error al actualizar el lote';
            this.loading = false;
            this.cdr.detectChanges();
          }
        });
      }
    }
  }

  deleteLote(lote: Lote): void {
    if (confirm(`Esta seguro de eliminar el Lote ${lote.numero} de la Manzana ${lote.manzana}?`)) {
      this.loading = true;
      this.loteService.deleteLote(this.parcelaId, lote.id).subscribe({
        next: () => {
          this.showSuccessMessage('Lote eliminado exitosamente');
          this.loadLotes();
        },
        error: (error: any) => {
          console.error('Error al eliminar lote:', error);
          this.showErrorMessage('Error al eliminar el lote');
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  onNumeroPartidaChange(): void {
    const numeroPartida = (this.formData.numeroPartida || '')
      .replace(/\D/g, '')
      .slice(0, 8)
      .trim();
    this.formData.numeroPartida = numeroPartida;
    this.formErrors.numeroPartida = '';
    this.numeroPartidaDuplicada = false;

    if (!numeroPartida) {
      this.clearNumeroPartidaCheck();
      this.numeroPartidaValidando = false;
      return;
    }

    this.clearNumeroPartidaCheck();
    this.numeroPartidaValidando = true;

    this.numeroPartidaCheckTimeout = setTimeout(() => {
      this.loteService.existsNumeroPartida(this.parcelaId, numeroPartida, this.currentLote?.id).subscribe({
        next: (exists: boolean) => {
          this.numeroPartidaDuplicada = exists;
          this.numeroPartidaValidando = false;
          if (exists) {
            this.formErrors.numeroPartida = `El número de partida ${numeroPartida} ya existe y no se puede repetir`;
          }
          this.cdr.detectChanges();
        },
        error: () => {
          this.numeroPartidaValidando = false;
          this.cdr.detectChanges();
        }
      });
    }, 250);
  }

  private clearNumeroPartidaCheck(): void {
    if (this.numeroPartidaCheckTimeout) {
      clearTimeout(this.numeroPartidaCheckTimeout);
      this.numeroPartidaCheckTimeout = null;
    }
  }

  goBack(): void {
    this.router.navigate(['/gestion-proyectos']);
  }

  buscarCliente(): void {
    this.showSearchModal = true;
    this.searchSource = 'USUARIOS';
    this.searchQuery = '';
    this.searchResults = [];
    this.searchError = '';
    this.searchLoading = false;
    this.refreshSearchModalView();
  }

  closeSearchModal(): void {
    this.showSearchModal = false;
    this.searchQuery = '';
    this.searchResults = [];
    this.searchError = '';
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    this.searchLoading = false;
    this.refreshSearchModalView();
  }

  closeProformaActionModal(): void {
    this.showProformaActionModal = false;
    this.selectedClientForAction = null;
    this.selectedProformaMode = 'LIBRE';
    this.selectedProformaProjectId = null;
  }

  setSearchSource(source: 'USUARIOS' | 'RENIEC'): void {
    this.searchSource = source;
    this.searchQuery = '';
    this.searchResults = [];
    this.searchError = '';
    this.searchLoading = false;
    this.refreshSearchModalView();
  }

  executeSearch(): void {
    const query = this.searchQuery.trim();
    this.searchError = '';
    this.searchResults = [];
    this.refreshSearchModalView();

    if (this.searchSource === 'RENIEC') {
      this.performReniecSearch(query);
      return;
    }

    this.performUsersSearch(query);
  }

  openProformaForClient(result: SearchClientResult): void {
    this.selectedClientForAction = result;
    this.selectedProformaMode = 'LIBRE';
    this.selectedProformaProjectId = null;
    this.closeSearchModal();
    this.showProformaActionModal = true;
    this.loadProformaProjects();
  }

  openAdquisicionForClient(result: SearchClientResult): void {
    this.closeSearchModal();
    this.prepareAdquisicionModal(result);
    this.adquisicionRequiresLoteSelection = true;
    this.showAdquisicionModal = true;
    this.loadAdquisicionProjects();
  }

  goToCronogramas(result: SearchClientResult): void {
    this.closeSearchModal();
    this.router.navigate(['/cronogramas'], {
      queryParams: {
        dni: result.dni,
        nombres: result.fullName
      }
    });
  }

  loadProformaProjects(): void {
    if (this.proformaProjects.length > 0 || this.loadingProformaProjects) {
      return;
    }

    this.loadingProformaProjects = true;
    this.projectService.getAllProjects().subscribe({
      next: (projects) => {
        this.proformaProjects = projects || [];
        this.loadingProformaProjects = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.proformaProjects = [];
        this.loadingProformaProjects = false;
        this.cdr.detectChanges();
      }
    });
  }

  private performUsersSearch(query: string): void {
    if (query.length < 3) {
      this.searchError = 'Ingresa al menos 3 caracteres para buscar en Usuarios.';
      return;
    }

    const normalizedQuery = query.toLowerCase();
    this.searchLoading = true;
    this.refreshSearchModalView();

    forkJoin({
      clientes: this.clienteService.getHistorial().pipe(catchError(() => of([] as Cliente[]))),
      cronogramas: this.cronogramaService.listar(this.buildCronogramaFilter(query)).pipe(catchError(() => of([])))
    }).subscribe({
      next: ({ clientes, cronogramas }) => {
        const clientesFiltrados = this.filterClientesForSearch(clientes || [], normalizedQuery);
        const results = this.mapClientesToSearchResults(clientesFiltrados, cronogramas || []);
        this.applySearchRenderState(results, results.length === 0 ? 'No se encontraron clientes con ese criterio.' : '');
      },
      error: () => {
        this.applySearchRenderState([], 'No se pudo completar la búsqueda de clientes.');
      }
    });
  }

  private filterClientesForSearch(clientes: Cliente[], normalizedQuery: string): Cliente[] {
    return (clientes || []).filter((cliente) => {
      const fullName = `${cliente.nombres || ''} ${cliente.apellidos || ''}`.toLowerCase();
      const dni = String(cliente.dni || '').toLowerCase();
      return fullName.includes(normalizedQuery) || dni.includes(normalizedQuery);
    });
  }

  private mapClientesToSearchResults(clientes: Cliente[], cronogramas: any[]): SearchClientResult[] {
    const byDni = new Map<string, Cliente[]>();

    for (const cliente of clientes || []) {
      const dni = String(cliente?.dni || '').trim();
      if (!dni) {
        continue;
      }

      const grouped = byDni.get(dni) || [];
      grouped.push(cliente);
      byDni.set(dni, grouped);
    }

    return Array.from(byDni.entries()).map(([dni, groupedClientes]) => {
      const first = groupedClientes[0];
      const hasLote = groupedClientes.some((cliente) => Number(cliente.loteId || 0) > 0);

      return {
        source: 'USUARIOS',
        dni,
        nombres: first?.nombres || '',
        apellidos: first?.apellidos || '',
        fullName: `${first?.nombres || ''} ${first?.apellidos || ''}`.trim(),
        estado: 'Cliente',
        existingClientes: groupedClientes,
        hasCronograma: this.hasCronogramaForDni(cronogramas, dni) || hasLote,
        telefono: first?.telefono || '',
        email: first?.email || '',
        direccion: first?.direccion || ''
      };
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
    this.refreshSearchModalView();

    forkJoin({
      reniec: this.reniecService.buscarPorDni(dni),
      clientes: this.clienteService.getHistorial(dni).pipe(catchError(() => of([] as Cliente[]))),
      cronogramas: this.cronogramaService.listar({ dni }).pipe(catchError(() => of([])))
    }).subscribe({
      next: ({ reniec, clientes, cronogramas }) => {
        if (!reniec) {
          this.applySearchRenderState([], 'No se encontró información en Reniec para ese DNI.');
          return;
        }

        this.applySearchRenderState([this.buildReniecSearchResult(reniec, clientes, cronogramas)], '');
      },
      error: (err) => {
        console.error('❌ Error consultando RENIEC en lotes:', this.reniecService.getTechnicalErrorDetail(err), err);
        this.applySearchRenderState([], this.reniecService.getFriendlyErrorMessage(err));
      }
    });
  }

  private applySearchRenderState(results: SearchClientResult[], errorMessage: string): void {
    this.searchResults = results;
    this.searchError = errorMessage;
    this.searchLoading = false;
    this.refreshSearchModalView();
  }

  private refreshSearchModalView(): void {
    this.cdr.markForCheck();
    this.cdr.detectChanges();
    queueMicrotask(() => this.appRef.tick());
  }

  private buildCronogramaFilter(query: string): { dni?: string; nombres?: string } {
    const digits = query.replace(/\D/g, '');
    if (digits.length >= 3) {
      return { dni: digits };
    }
    return { nombres: query };
  }

  private buildReniecSearchResult(reniec: ReniecData, clientes: Cliente[], cronogramas: any[]): SearchClientResult {
    const existingClientes = this.findClientesByDni(clientes, reniec.number);
    const tieneLote = existingClientes.some((cliente) => Number(cliente.loteId || 0) > 0);
    return {
      source: 'RENIEC',
      dni: reniec.number,
      nombres: reniec.name || '',
      apellidos: reniec.surname || '',
      fullName: reniec.full_name || `${reniec.name || ''} ${reniec.surname || ''}`.trim(),
      estado: existingClientes.length > 0 ? 'Registrado en el sistema' : 'Consulta Reniec',
      existingClientes,
      hasCronograma: this.hasCronogramaForDni(cronogramas, reniec.number) || tieneLote,
      telefono: existingClientes[0]?.telefono || '',
      email: existingClientes[0]?.email || '',
      direccion: existingClientes[0]?.direccion || ''
    };
  }

  private findClientesByDni(clientes: Cliente[], dni: string): Cliente[] {
    const normalized = String(dni || '').trim();
    return (clientes || []).filter((cliente) => String(cliente.dni || '').trim() === normalized);
  }

  private hasCronogramaForDni(contratos: any[], dni: string): boolean {
    const normalized = String(dni || '').trim();
    return (contratos || []).some((contrato) => String(contrato?.clienteDni || '').trim() === normalized);
  }

  private loadAdquisicionProjects(): void {
    if (this.adquisicionProjects.length > 0) {
      return;
    }

    this.adquisicionSelectionLoading = true;
    this.projectService.getAllProjects().subscribe({
      next: (projects) => {
        this.adquisicionProjects = projects || [];
        this.adquisicionSelectionLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.adquisicionSelectionLoading = false;
        this.adquisicionErrors = 'No se pudieron cargar los proyectos para la adquisición.';
        this.cdr.detectChanges();
      }
    });
  }

  onAdquisicionProjectChange(): void {
    this.adquisicionSelector.etapaId = null;
    this.adquisicionSelector.parcelaId = null;
    this.adquisicionSelector.manzanaId = null;
    this.adquisicionSelector.manzana = '';
    this.adquisicionSelector.loteId = null;
    this.adquisicionEtapas = [];
    this.adquisicionParcelas = [];
    this.adquisicionManzanasDisponibles = [];
    this.adquisicionLotesDisponibles = [];
    this.selectedLoteForAdquisicion = null;

    if (!this.adquisicionSelector.projectId) {
      return;
    }

    this.adquisicionSelectionLoading = true;
    forkJoin({
      etapas: this.etapaService.getEtapasByProject(this.adquisicionSelector.projectId),
      clientes: this.clienteService.getClientesByProject(this.adquisicionSelector.projectId)
    }).subscribe({
      next: ({ etapas, clientes }) => {
        this.adquisicionEtapas = etapas || [];
        this.reemplazarClientesPorProyecto(clientes || []);
        this.adquisicionSelectionLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.adquisicionSelectionLoading = false;
        this.adquisicionErrors = 'No se pudieron cargar las etapas del proyecto.';
        this.cdr.detectChanges();
      }
    });
  }

  onAdquisicionEtapaChange(): void {
    this.adquisicionSelector.parcelaId = null;
    this.adquisicionSelector.manzanaId = null;
    this.adquisicionSelector.manzana = '';
    this.adquisicionSelector.loteId = null;
    this.adquisicionParcelas = [];
    this.adquisicionManzanasDisponibles = [];
    this.adquisicionLotesDisponibles = [];
    this.selectedLoteForAdquisicion = null;

    if (!this.adquisicionSelector.etapaId) {
      return;
    }

    this.adquisicionSelectionLoading = true;
    this.parcelaService.getParcelasByEtapa(this.adquisicionSelector.etapaId).subscribe({
      next: (parcelas) => {
        this.adquisicionParcelas = parcelas || [];
        this.adquisicionSelectionLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.adquisicionSelectionLoading = false;
        this.adquisicionErrors = 'No se pudieron cargar las parcelas de la etapa.';
        this.cdr.detectChanges();
      }
    });
  }

  onAdquisicionParcelaChange(): void {
    this.adquisicionSelector.manzanaId = null;
    this.adquisicionSelector.manzana = '';
    this.adquisicionSelector.loteId = null;
    this.adquisicionManzanasDisponibles = [];
    this.adquisicionLotesDisponibles = [];
    this.selectedLoteForAdquisicion = null;

    if (!this.adquisicionSelector.parcelaId) {
      return;
    }

    this.adquisicionSelectionLoading = true;
    forkJoin({
      manzanas: this.parcelaService.getManzanasByParcela(this.adquisicionSelector.parcelaId),
      lotes: this.loteService.getLotesByParcela(this.adquisicionSelector.parcelaId)
    }).subscribe({
      next: ({ manzanas, lotes }) => {
        this.adquisicionManzanasDisponibles = manzanas || [];
        this.adquisicionLotesDisponibles = lotes || [];
        this.adquisicionSelectionLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.adquisicionSelectionLoading = false;
        this.adquisicionErrors = 'No se pudo cargar la información de la parcela seleccionada.';
        this.cdr.detectChanges();
      }
    });
  }

  onAdquisicionManzanaChange(): void {
    const match = this.adquisicionManzanasDisponibles.find((manzana) => manzana.id === this.adquisicionSelector.manzanaId);
    this.adquisicionSelector.manzana = match ? this.normalizeManzana(match.nombre) : '';
    this.adquisicionSelector.loteId = null;
    this.selectedLoteForAdquisicion = null;
  }

  onAdquisicionLoteChange(): void {
    const lote = this.getFilteredAdquisicionLotes().find((item) => item.id === this.adquisicionSelector.loteId) || null;
    if (!lote) {
      this.selectedLoteForAdquisicion = null;
      return;
    }

    if (this.isLoteAdquirido(lote)) {
      this.adquisicionErrors = `El lote ya está adquirido por: ${this.propietariosLoteTexto(lote)}`;
      this.selectedLoteForAdquisicion = null;
      this.adquisicionSelector.loteId = null;
      this.cdr.detectChanges();
      return;
    }

    const project = this.adquisicionProjects.find((item) => item.id === this.adquisicionSelector.projectId);
    const parcela = this.adquisicionParcelas.find((item) => item.id === this.adquisicionSelector.parcelaId);
    const etapa = this.adquisicionEtapas.find((item) => item.id === this.adquisicionSelector.etapaId);

    this.selectedLoteForAdquisicion = {
      ...lote,
      projectId: this.adquisicionSelector.projectId || lote.projectId,
      projectNombre: project?.nombre || lote.projectNombre,
      etapaNumero: etapa?.numeroEtapa || lote.etapaNumero,
      parcelaId: this.adquisicionSelector.parcelaId || lote.parcelaId,
      parcelaNombre: parcela?.nombre || lote.parcelaNombre,
      manzanaId: this.adquisicionSelector.manzanaId || lote.manzanaId,
      manzana: this.adquisicionSelector.manzana || lote.manzana
    };

    this.aplicarPrecioLotePorDefecto(this.selectedLoteForAdquisicion);
    this.adquisicionErrors = '';
  }

  getFilteredAdquisicionLotes(): Lote[] {
    if (!this.adquisicionSelector.manzana) {
      return [];
    }

    return this.adquisicionLotesDisponibles
      .filter((lote) => this.normalizeManzana(lote.manzana) === this.adquisicionSelector.manzana)
      .filter((lote) => !this.isLoteAdquirido(lote))
      .sort((a, b) => a.numero - b.numero);
  }

  onAdquisicionTipoOperacionChange(): void {
    if (this.adquisicionForm.tipoOperacion === 'CONTADO') {
      this.aplicarPrecioLotePorDefecto(this.selectedLoteForAdquisicion);
      this.adquisicionForm.montoOperacion = Number(this.adquisicionForm.precioVenta || 0);
    }
    this.syncAdquisicionMontosInput();
  }

  onAdquisicionPrecioVentaChange(): void {
    if (this.adquisicionForm.tipoOperacion === 'CONTADO') {
      this.adquisicionForm.montoOperacion = Number(this.adquisicionForm.precioVenta || 0);
    }
    this.syncAdquisicionMontosInput();
  }

  onAdquisicionPrecioVentaInputChange(value: string): void {
    this.adquisicionForm.precioVenta = this.parseFormattedMonto(value);
    if (this.adquisicionForm.tipoOperacion === 'CONTADO') {
      this.adquisicionForm.montoOperacion = Number(this.adquisicionForm.precioVenta || 0);
    }
    this.syncAdquisicionMontosInput();
  }

  onAdquisicionMontoOperacionInputChange(value: string): void {
    this.adquisicionForm.montoOperacion = this.parseFormattedMonto(value);
    this.syncAdquisicionMontosInput();
  }

  addAdquisicionMedio(): void {
    this.adquisicionForm.medios.push({
      id: this.adquisicionForm._nextMedioId++,
      medio: 'EFECTIVO',
      monto: 0,
      efectivoEntregado: 0
    });
  }

  removeAdquisicionMedio(id: number): void {
    this.adquisicionForm.medios = this.adquisicionForm.medios.filter(m => m.id !== id);
    if (this.adquisicionForm.medios.length === 0) {
      this.addAdquisicionMedio();
    }
  }

  totalAdquisicionMedios(): number {
    return (this.adquisicionForm.medios || []).reduce((sum, m) => sum + Number(m.monto || 0), 0);
  }

  vueltoAdquisicionEfectivo(medio: { monto: number; efectivoEntregado: number }): number {
    return Math.max(0, Number(medio.efectivoEntregado || 0) - Number(medio.monto || 0));
  }

  private buildAdquisicionMediosTexto(): string {
    const mediosValidos = (this.adquisicionForm.medios || [])
      .filter((m) => Number(m.monto || 0) > 0)
      .map((m) => {
        const monto = Number(m.monto || 0).toFixed(2);
        if (m.medio === 'EFECTIVO' && Number(m.efectivoEntregado || 0) > 0) {
          const entregado = Number(m.efectivoEntregado || 0);
          const vuelto = this.vueltoAdquisicionEfectivo(medioFrom(m));
          if (vuelto > 0) {
            return `EFECTIVO S/ ${monto} (con S/ ${entregado.toFixed(2)}, vuelto S/ ${vuelto.toFixed(2)})`;
          }
          return `EFECTIVO S/ ${monto} (con S/ ${entregado.toFixed(2)})`;
        }
        return `${m.medio} S/ ${monto}`;
      });

    function medioFrom(m: { monto: number; efectivoEntregado: number }) {
      return { monto: Number(m.monto || 0), efectivoEntregado: Number(m.efectivoEntregado || 0) };
    }

    return mediosValidos.join(' | ').slice(0, 500);
  }

  private aplicarPrecioLotePorDefecto(lote: Lote | null): void {
    const precioLote = Number(lote?.precioLote || 0);
    if (precioLote <= 0) {
      return;
    }

    if (!this.adquisicionForm.precioVenta || this.adquisicionForm.precioVenta <= 0 || this.adquisicionForm.tipoOperacion === 'CONTADO') {
      this.adquisicionForm.precioVenta = precioLote;
    }

    if (this.adquisicionForm.tipoOperacion === 'CONTADO') {
      this.adquisicionForm.montoOperacion = this.adquisicionForm.precioVenta;
      return;
    }

    if (!this.adquisicionForm.montoOperacion || this.adquisicionForm.montoOperacion <= 0) {
      this.adquisicionForm.montoOperacion = precioLote;
    }

    this.syncAdquisicionMontosInput();
  }

  private syncAdquisicionMontosInput(): void {
    this.adquisicionPrecioVentaInput = this.formatMontoInput(this.adquisicionForm.precioVenta);
    this.adquisicionMontoOperacionInput = this.formatMontoInput(this.adquisicionForm.montoOperacion);
    this.adquisicionMontoSeparacionInput = this.formatMontoInput(this.adquisicionForm.montoSeparacionObjetivo);
  }

  private formatMontoInput(value: number | null | undefined): string {
    const numeric = Number(value ?? 0);
    if (!Number.isFinite(numeric)) {
      return '0';
    }

    return numeric.toLocaleString('en-US', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2
    });
  }

  private parseFormattedMonto(rawValue: string | number | null | undefined): number {
    const raw = String(rawValue ?? '').trim();
    if (!raw) {
      return 0;
    }

    const cleaned = raw.replace(/,/g, '').replace(/[^\d.]/g, '');
    const [integerPart, ...decimalParts] = cleaned.split('.');
    const normalized = decimalParts.length > 0
      ? `${integerPart}.${decimalParts.join('').slice(0, 2)}`
      : integerPart;
    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  continuarConProformaYAdquisicion(): void {
    if (!this.selectedClientForAction) {
      return;
    }

    if (this.selectedProformaMode === 'PROYECTO' && !this.selectedProformaProjectId) {
      this.showErrorMessage('Selecciona un proyecto para continuar.');
      return;
    }

    const nombres = (this.selectedClientForAction.nombres || '').trim();
    const apellidos = (this.selectedClientForAction.apellidos || '').trim();

    const queryParams: any = {
      quickAction: '1',
      mode: this.selectedProformaMode === 'LIBRE' ? 'libre' : 'proyecto',
      dni: this.selectedClientForAction.dni,
      nombres,
      apellidos
    };

    if (this.selectedProformaMode === 'PROYECTO' && this.selectedProformaProjectId) {
      queryParams.projectId = this.selectedProformaProjectId;
    }

    this.closeProformaActionModal();
    this.router.navigate(['/proformas'], { queryParams });
  }

  showSuccessMessage(message: string): void {
    const successDiv = document.createElement('div');
    successDiv.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
      color: white;
      padding: 1.5rem 2rem;
      border-radius: 12px;
      box-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
      z-index: 10000;
      font-size: 1.1rem;
      font-weight: 600;
      animation: slideInRight 0.4s ease-out;
    `;
    successDiv.innerHTML = `
      <div style="display: flex; align-items: center; gap: 10px;">
        <span style="font-size: 1.5rem;">✅</span>
        <span>${message}</span>
      </div>
    `;
    document.body.appendChild(successDiv);

    setTimeout(() => {
      successDiv.style.animation = 'slideInRight 0.4s ease-out reverse';
      setTimeout(() => {
        document.body.removeChild(successDiv);
      }, 400);
    }, 3000);
  }

  showErrorMessage(message: string): void {
    const errorDiv = document.createElement('div');
    errorDiv.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      background: linear-gradient(135deg, #f44336 0%, #da190b 100%);
      color: white;
      padding: 1.5rem 2rem;
      border-radius: 12px;
      box-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
      z-index: 10000;
      font-size: 1.1rem;
      font-weight: 600;
      animation: slideInRight 0.4s ease-out;
    `;
    errorDiv.innerHTML = `
      <div style="display: flex; align-items: center; gap: 10px;">
        <span style="font-size: 1.5rem;">⚠️</span>
        <span>${message}</span>
      </div>
    `;
    document.body.appendChild(errorDiv);

    setTimeout(() => {
      errorDiv.style.animation = 'slideInRight 0.4s ease-out reverse';
      setTimeout(() => {
        document.body.removeChild(errorDiv);
      }, 400);
    }, 3000);
  }

  private setSelectedManzana(manzana: string): void {
    const normalized = this.normalizeManzana(manzana);
    this.formData.manzana = normalized;
    this.formData.manzanaId = this.findManzanaIdByLabel(normalized);
  }

  getSuggestedNumeroLabel(): string {
    if (this.modalMode !== 'create') {
      return '';
    }

    const manzana = this.normalizeManzana(this.formData.manzana);
    if (!manzana) {
      return '';
    }

    return `Sugerido automáticamente: Lote ${this.getNextSuggestedLoteNumero(manzana)}`;
  }

  private applySuggestedNumeroForCurrentManzana(): void {
    const manzana = this.normalizeManzana(this.formData.manzana);
    if (!manzana) {
      this.formData.numero = 1;
      return;
    }

    this.formData.numero = this.getNextSuggestedLoteNumero(manzana);
  }

  private getNextSuggestedLoteNumero(manzana: string): number {
    const lotesManzana = this.lotes
      .filter(lote => this.normalizeManzana(lote.manzana) === manzana)
      .map(lote => Number(lote.numero || 0))
      .filter(numero => numero > 0);

    if (lotesManzana.length === 0) {
      return 1;
    }

    return Math.max(...lotesManzana) + 1;
  }

  private findManzanaIdByLabel(manzana: string): number | null {
    const normalized = this.normalizeManzana(manzana);
    if (!normalized || this.manzanaOptions.length === 0) {
      return null;
    }

    const match = this.manzanaOptions.find(option => this.normalizeManzana(option.nombre) === normalized);
    return match?.id ?? null;
  }

  private normalizeManzana(value: string): string {
    if (!value) {
      return '';
    }

    const normalized = value.trim().toUpperCase();
    return normalized.startsWith('MANZANA ') ? normalized.substring('MANZANA '.length).trim() : normalized;
  }

  private compareManzanaLabel(a: string, b: string): number {
    return a.localeCompare(b, 'es', { numeric: true, sensitivity: 'base' });
  }

  closeAdquisicionModal(): void {
    this.showAdquisicionModal = false;
    this.adquisicionSaving = false;
    this.selectedLoteForAdquisicion = null;
    this.selectedSearchClientForAdquisicion = null;
    this.adquisicionErrors = '';
    this.adquisicionStep = 1;
    this.adquisicionRequiresLoteSelection = false;
    this.adquisicionPrecioVentaInput = '0';
    this.adquisicionMontoOperacionInput = '0';
    this.adquisicionMontoSeparacionInput = '2,000';
    this.resetAdquisicionSelector();
  }

  private prepareAdquisicionModal(result?: SearchClientResult): void {
    this.adquisicionErrors = '';
    this.adquisicionSaving = false;
    this.adquisicionStep = 1;
    this.selectedSearchClientForAdquisicion = result || null;
    this.resetAdquisicionSelector();

    const propietario = result ? this.createPrefilledPropietario(result) : this.createEmptyPropietario();
    this.adquisicionForm = {
      tipoOperacion: 'CONTADO',
      fechaOperacion: this.getTodayDateISO(),
      asesor: '',
      medios: [{ id: 0, medio: 'EFECTIVO', monto: 0, efectivoEntregado: 0 }],
      _nextMedioId: 1,
      precioVenta: 0,
      montoOperacion: 0,
      montoSeparacionObjetivo: 2000,
      plazoMeses: 24,
      interesPorcentaje: 10,
      propietariosCount: 1,
      propietarios: [propietario]
    };
    this.syncAdquisicionMontosInput();
  }

  private resetAdquisicionSelector(): void {
    this.adquisicionSelectionLoading = false;
    this.adquisicionEtapas = [];
    this.adquisicionParcelas = [];
    this.adquisicionManzanasDisponibles = [];
    this.adquisicionLotesDisponibles = [];
    this.adquisicionSelector = {
      projectId: null,
      etapaId: null,
      parcelaId: null,
      manzanaId: null,
      manzana: '',
      loteId: null
    };
  }

  private createPrefilledPropietario(result: SearchClientResult): PropietarioRequest {
    return {
      nombres: result.nombres || '',
      apellidos: result.apellidos || '',
      dni: result.dni || '',
      email: result.email || '',
      telefono: result.telefono || '',
      direccion: result.direccion || ''
    };
  }

  private tryOpenPendingAdquisicionFromDashboard(): void {
    if (this.pendingAdquisicionChecked) {
      return;
    }
    this.pendingAdquisicionChecked = true;

    const raw = sessionStorage.getItem('sisarovi_pending_adquisicion_client');
    if (!raw) {
      return;
    }

    sessionStorage.removeItem('sisarovi_pending_adquisicion_client');

    try {
      const data = JSON.parse(raw) as Partial<SearchClientResult>;
      const dni = String(data?.dni || '').trim();
      if (!dni) {
        return;
      }

      const normalized: SearchClientResult = {
        source: data?.source === 'RENIEC' ? 'RENIEC' : 'USUARIOS',
        dni,
        nombres: String(data?.nombres || '').trim(),
        apellidos: String(data?.apellidos || '').trim(),
        fullName: String(data?.fullName || `${data?.nombres || ''} ${data?.apellidos || ''}`).trim(),
        estado: 'Pendiente de adquisición',
        existingClientes: [],
        hasCronograma: false,
        telefono: String(data?.telefono || '').trim(),
        email: String(data?.email || '').trim(),
        direccion: String(data?.direccion || '').trim()
      };

      this.openAdquisicionForClient(normalized);
    } catch (error) {
      console.warn('No se pudo procesar cliente pendiente para adquisición:', error);
    }
  }

  goToOperacionStep(): void {
    this.adquisicionErrors = '';
    this.normalizeOwnersLimits();

    if (!this.selectedLoteForAdquisicion) {
      this.adquisicionErrors = 'Debes seleccionar proyecto, etapa, parcela, manzana y lote antes de continuar.';
      return;
    }

    if (this.adquisicionForm.propietarios.length < 1 || this.adquisicionForm.propietarios.length > 4) {
      this.adquisicionErrors = 'Debes registrar entre 1 y 4 propietarios.';
      return;
    }

    const invalid = this.adquisicionForm.propietarios.some(owner =>
      !owner.nombres?.trim() ||
      !owner.apellidos?.trim() ||
      !owner.dni?.trim() ||
      !owner.telefono?.trim() ||
      !owner.direccion?.trim()
    );

    if (invalid) {
      this.adquisicionErrors = 'Completa todos los campos obligatorios de cada propietario para continuar.';
      return;
    }

    const invalidLengths = this.adquisicionForm.propietarios.some(owner =>
      (owner.nombres || '').trim().length > 40 ||
      (owner.dni || '').trim().length > 8 ||
      (owner.telefono || '').trim().length > 9
    );

    if (invalidLengths) {
      this.adquisicionErrors = 'Nombres máximo 40 caracteres, DNI máximo 8 dígitos y Teléfono máximo 9 dígitos.';
      return;
    }

    this.adquisicionStep = 2;
    this.cdr.detectChanges();
  }

  backToPropietariosStep(): void {
    this.adquisicionStep = 1;
    this.adquisicionErrors = '';
    this.cdr.detectChanges();
  }

  onPropietariosCountChange(): void {
    const count = Math.max(1, Math.min(4, Number(this.adquisicionForm.propietariosCount) || 1));
    this.adquisicionForm.propietariosCount = count;

    const next: PropietarioRequest[] = [];
    for (let i = 0; i < count; i++) {
      next.push(this.adquisicionForm.propietarios[i] || this.createEmptyPropietario());
    }
    this.adquisicionForm.propietarios = next;
  }

  onOwnerNombresChange(index: number, value: string): void {
    if (!this.adquisicionForm.propietarios[index]) {
      return;
    }
    this.adquisicionForm.propietarios[index].nombres = (value || '').slice(0, 40);
  }

  onOwnerDniChange(index: number, value: string): void {
    if (!this.adquisicionForm.propietarios[index]) {
      return;
    }
    this.adquisicionForm.propietarios[index].dni = (value || '').replace(/\D/g, '').slice(0, 8);
  }

  onOwnerTelefonoChange(index: number, value: string): void {
    if (!this.adquisicionForm.propietarios[index]) {
      return;
    }
    this.adquisicionForm.propietarios[index].telefono = (value || '').replace(/\D/g, '').slice(0, 9);
  }

  guardarAdquisicion(): void {
    this.adquisicionErrors = '';
    this.normalizeOwnersLimits();
    this.adquisicionForm.precioVenta = this.parseFormattedMonto(this.adquisicionPrecioVentaInput);
    this.adquisicionForm.montoOperacion = this.parseFormattedMonto(this.adquisicionMontoOperacionInput);
    this.syncAdquisicionMontosInput();

    if (!this.selectedLoteForAdquisicion) {
      this.adquisicionErrors = 'No se encontró el lote seleccionado.';
      return;
    }

    if (this.isLoteAdquirido(this.selectedLoteForAdquisicion)) {
      this.adquisicionErrors = `El lote ya está adquirido por: ${this.propietariosLoteTexto(this.selectedLoteForAdquisicion)}`;
      return;
    }

    if (!this.adquisicionForm.fechaOperacion) {
      this.adquisicionErrors = 'La fecha de operación es obligatoria.';
      return;
    }

    if (!this.adquisicionForm.precioVenta || this.adquisicionForm.precioVenta <= 0) {
      this.adquisicionErrors = 'El precio de venta debe ser mayor que 0.';
      return;
    }

    if (!this.adquisicionForm.montoOperacion || this.adquisicionForm.montoOperacion <= 100) {
      this.adquisicionErrors = 'El monto de operación debe ser mayor que 100.';
      return;
    }

    if (this.adquisicionForm.tipoOperacion === 'CONTADO') {
      const precioVenta = Number(this.adquisicionForm.precioVenta || 0);
      const montoOperacion = Number(this.adquisicionForm.montoOperacion || 0);
      if (Math.abs(precioVenta - montoOperacion) > 0.01) {
        this.adquisicionErrors = 'En Pago al Contado, el monto inicial debe ser exactamente igual al precio de venta.';
        return;
      }
    }

    const totalMedios = this.totalAdquisicionMedios();
    if (totalMedios <= 0) {
      this.adquisicionErrors = 'Debes agregar al menos un medio de pago con monto válido.';
      return;
    }
    if (Math.abs(totalMedios - Number(this.adquisicionForm.montoOperacion || 0)) > 0.01) {
      this.adquisicionErrors = `El total de medios (${totalMedios.toFixed(2)}) debe ser igual al monto de operación (${Number(this.adquisicionForm.montoOperacion || 0).toFixed(2)}).`;
      return;
    }

    if (this.adquisicionForm.tipoOperacion === 'SEPARACION') {
      if (Number(this.adquisicionForm.montoSeparacionObjetivo) !== 2000) {
        this.adquisicionErrors = 'El monto de separación debe ser exactamente S/ 2000.';
        return;
      }
    }

    if (this.adquisicionForm.tipoOperacion !== 'CONTADO' && this.adquisicionForm.plazoMeses < 1) {
      this.adquisicionErrors = 'El plazo en meses debe ser mayor o igual a 1.';
      return;
    }

    if (this.adquisicionForm.propietarios.length < 1 || this.adquisicionForm.propietarios.length > 4) {
      this.adquisicionErrors = 'Debes registrar entre 1 y 4 propietarios.';
      return;
    }

    const invalid = this.adquisicionForm.propietarios.some(owner =>
      !owner.nombres?.trim() ||
      !owner.apellidos?.trim() ||
      !owner.dni?.trim() ||
      !owner.telefono?.trim() ||
      !owner.direccion?.trim()
    );

    if (invalid) {
      this.adquisicionErrors = 'Completa todos los campos obligatorios de cada propietario.';
      return;
    }

    const invalidLengths = this.adquisicionForm.propietarios.some(owner =>
      (owner.nombres || '').trim().length > 40 ||
      (owner.dni || '').trim().length > 8 ||
      (owner.telefono || '').trim().length > 9
    );

    if (invalidLengths) {
      this.adquisicionErrors = 'Nombres máximo 40 caracteres, DNI máximo 8 dígitos y Teléfono máximo 9 dígitos.';
      return;
    }

    const payload: ClienteAdquisicionRequest = {
      loteId: this.selectedLoteForAdquisicion.id,
      tipoOperacion: this.adquisicionForm.tipoOperacion,
      fechaOperacion: this.adquisicionForm.fechaOperacion,
      asesor: (this.adquisicionForm.asesor || '').trim().slice(0, 120) || undefined,
      medios: this.buildAdquisicionMediosTexto() || undefined,
      precioVenta: Number(this.adquisicionForm.precioVenta),
      montoOperacion: Number(this.adquisicionForm.montoOperacion),
      montoSeparacionObjetivo: 2000,
      plazoMeses: Number(this.adquisicionForm.plazoMeses),
      interesPorcentaje: Number(this.adquisicionForm.interesPorcentaje),
      propietarios: this.adquisicionForm.propietarios.map(owner => ({
        nombres: owner.nombres.trim(),
        apellidos: owner.apellidos.trim(),
        dni: owner.dni.trim(),
        email: owner.email?.trim() ? owner.email.trim() : null,
        telefono: owner.telefono.trim(),
        direccion: owner.direccion.trim()
      }))
    };

    this.adquisicionSaving = true;
    this.clienteService.registrarAdquisicion(payload).subscribe({
      next: () => {
        this.showSuccessMessage('Cliente registrado correctamente. Redirigiendo a cronograma...');
        this.irACronogramasConDatosAdquisicion();
        this.closeAdquisicionModal();
      },
      error: (error: any) => {
        this.adquisicionSaving = false;
        this.adquisicionErrors = error?.error?.message || 'No se pudo registrar la adquisición/separación.';
        this.cdr.detectChanges();
      }
    });
  }

  getTipoOperacionLabel(): string {
    switch (this.adquisicionForm.tipoOperacion) {
      case 'CONTADO': return 'Pago al Contado';
      case 'CREDITO': return 'Pago al Crédito';
      case 'SEPARACION': return 'Separación';
      default: return '';
    }
  }

  getPrecioVentaTipoLabel(): string {
    switch (this.adquisicionForm.tipoOperacion) {
      case 'CONTADO':
        return 'Contado';
      case 'CREDITO':
        return 'Crédito';
      case 'SEPARACION':
        return 'Separación';
      default:
        return 'Contado';
    }
  }

  private irACronogramasConDatosAdquisicion(): void {
    const primerPropietario = this.adquisicionForm.propietarios[0] || this.createEmptyPropietario();
    const nombresCompletos = `${(primerPropietario.nombres || '').trim()} ${(primerPropietario.apellidos || '').trim()}`.trim();
    const dni = (primerPropietario.dni || '').trim();

    // Pre-fetch to warm the cache so cronogramas page shows data immediately (no loading spinner)
    this.cronogramaService.listar({ dni }).pipe(catchError(() => of([]))).subscribe(() => {
      this.router.navigate(['/cronogramas'], {
        queryParams: { dni, nombres: nombresCompletos }
      });
    });
  }

  private createEmptyPropietario(): PropietarioRequest {
    return {
      nombres: '',
      apellidos: '',
      dni: '',
      email: '',
      telefono: '',
      direccion: ''
    };
  }

  private normalizeOwnersLimits(): void {
    this.adquisicionForm.propietarios = this.adquisicionForm.propietarios.map(owner => ({
      ...owner,
      nombres: (owner.nombres || '').slice(0, 40),
      dni: (owner.dni || '').replace(/\D/g, '').slice(0, 8),
      telefono: (owner.telefono || '').replace(/\D/g, '').slice(0, 9)
    }));
    this.cdr.detectChanges();
  }

  private getTodayDateISO(): string {
    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
}

