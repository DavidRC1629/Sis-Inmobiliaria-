import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ProformaService } from '../../services/proforma.service';
import { ProformaItem } from '../../models/proforma.model';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user.model';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-proformas-historial',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './proformas-historial.component.html',
  styleUrls: ['./proformas-historial.component.css', '../dashboard/dashboard.component.css']
})
export class ProformasHistorialComponent implements OnInit {
  proformas: ProformaItem[] = [];
  todasLasProformas: ProformaItem[] = [];
  buscarVisible = false;
  tipoBusqueda: 'codigo' | 'cliente' = 'codigo';
  terminoBusqueda = '';
  selectedFecha = '';
  errorBusqueda = '';
  showDropdown = false;
  currentUser: User | null = null;
  private historialRetryDone = false;
  readonly pageSize = 40;
  currentPage = 1;
  private pendingSearch: { q: string; tipo: 'codigo' | 'cliente' } | null = null;

  constructor(
    private proformaService: ProformaService,
    private router: Router,
    private authService: AuthService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((user) => {
      this.currentUser = user;
    });

    this.route.queryParams.subscribe((params) => {
      const q = (params['q'] || '').trim();
      const tipo = params['tipo'] === 'cliente' ? 'cliente' : 'codigo';
      if (q.length >= 3) {
        this.buscarVisible = true;
        this.terminoBusqueda = q;
        this.tipoBusqueda = tipo;
        this.pendingSearch = { q, tipo };
      }
    });

    const cached = this.proformaService.getHistorialCache();
    if (cached.length > 0) {
      this.todasLasProformas = cached;
      this.setFilteredProformas(this.aplicarFiltroFecha(cached));
      if (this.pendingSearch) {
        this.buscarAhora();
        this.pendingSearch = null;
      }
    }

    this.cargarHistorial();
  }

  cargarHistorial(): void {
    this.proformaService.getHistorialProformas().subscribe({
      next: (data) => {
        const items = this.normalizarHistorial(data);
        if (items.length > 0) {
          this.todasLasProformas = items;
          if (this.pendingSearch) {
            this.buscarAhora();
            this.pendingSearch = null;
          } else {
            this.setFilteredProformas(this.aplicarFiltroFecha(items));
          }
          this.errorBusqueda = '';
          this.historialRetryDone = false;
          return;
        }

        this.cargarHistorialFallback();
      },
      error: () => {
        this.cargarHistorialFallback();
      }
    });
  }

  private async cargarHistorialFallback(): Promise<void> {
    try {
      const response = await fetch(`${environment.apiUrl}/proformas/historial?_t=${Date.now()}`, {
        method: 'GET',
        cache: 'no-store'
      });

      if (!response.ok) {
        this.errorBusqueda = 'No se pudo cargar el historial en este momento.';
        return;
      }

      const data = await response.json();
      const items = this.normalizarHistorial(data);

      if (items.length > 0) {
        this.todasLasProformas = items;
        if (this.pendingSearch) {
          this.buscarAhora();
          this.pendingSearch = null;
        } else {
          this.setFilteredProformas(this.aplicarFiltroFecha(items));
        }
        this.errorBusqueda = '';
        this.historialRetryDone = false;
        return;
      }

      if (this.todasLasProformas.length > 0) {
        if (this.pendingSearch) {
          this.buscarAhora();
          this.pendingSearch = null;
        } else {
          this.setFilteredProformas(this.aplicarFiltroFecha(this.todasLasProformas));
        }
        this.errorBusqueda = '';
        this.historialRetryDone = false;
        return;
      }

      if (!this.historialRetryDone) {
        this.historialRetryDone = true;
        setTimeout(() => this.cargarHistorial(), 700);
        return;
      }

      this.errorBusqueda = 'No hay proformas registradas para mostrar.';
    } catch {
      if (this.todasLasProformas.length > 0) {
        this.setFilteredProformas(this.aplicarFiltroFecha(this.todasLasProformas));
        this.errorBusqueda = '';
        this.historialRetryDone = false;
        return;
      }

      if (!this.historialRetryDone) {
        this.historialRetryDone = true;
        setTimeout(() => this.cargarHistorial(), 700);
        return;
      }
        this.errorBusqueda = 'No se pudo cargar el historial en este momento.';
    }
  }

  private normalizarHistorial(data: any): ProformaItem[] {
    if (Array.isArray(data)) {
      return data as ProformaItem[];
    }
    if (Array.isArray(data?.content)) {
      return data.content as ProformaItem[];
    }
    if (Array.isArray(data?.items)) {
      return data.items as ProformaItem[];
    }
    return [];
  }

  formatearFechaCreacion(createdAt?: string): string {
    if (!createdAt) {
      return '-';
    }

    const normalized = createdAt.includes('T') ? createdAt : createdAt.replace(' ', 'T');
    const date = new Date(normalized);

    if (Number.isNaN(date.getTime())) {
      return createdAt;
    }

    const pad = (value: number) => value.toString().padStart(2, '0');
    return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }

  toggleBuscar(): void {
    this.buscarVisible = !this.buscarVisible;
    this.errorBusqueda = '';
    this.terminoBusqueda = '';
    if (!this.buscarVisible) {
      this.cargarHistorial();
    }
  }

  onTerminoChange(): void {
    this.errorBusqueda = '';
    if (this.terminoBusqueda.trim().length === 0) {
      this.setFilteredProformas(this.aplicarFiltroFecha(this.todasLasProformas));
    }
  }

  onFechaChange(): void {
    this.errorBusqueda = '';
    const q = this.normalizarTextoBusqueda(this.terminoBusqueda);

    if (q.length >= 3) {
      this.buscarAhora();
      return;
    }

    this.setFilteredProformas(this.aplicarFiltroFecha(this.todasLasProformas));
  }

  buscarAhora(): void {
    this.errorBusqueda = '';
    const q = this.normalizarTextoBusqueda(this.terminoBusqueda);

    if (q.length === 0) {
      this.setFilteredProformas(this.aplicarFiltroFecha(this.todasLasProformas));
      return;
    }

    if (q.length < 3) {
      return;
    }

    if (this.tipoBusqueda === 'codigo') {
      const resultados = this.todasLasProformas.filter((item) => {
        const codigo = this.normalizarCodigo(item?.codigo || '');
        return codigo.includes(this.normalizarCodigo(q));
      });

      this.setFilteredProformas(this.aplicarFiltroFecha(resultados));

      if (this.proformas.length === 0) {
        this.errorBusqueda = 'No existe el código ingresado relacionado a una proforma.';
      }
      return;
    }

    const resultados = this.todasLasProformas.filter((item) => {
      const cliente = this.normalizarTextoBusqueda(item?.clienteNombre || '');
      const dni = (item?.clienteDni || '').toLowerCase();
      return cliente.includes(q) || dni.includes(q.toLowerCase());
    });

    this.setFilteredProformas(this.aplicarFiltroFecha(resultados));

    if (this.proformas.length === 0) {
      this.errorBusqueda = 'No se encontraron proformas para ese criterio.';
    }
  }

  cambiarTipoBusqueda(tipo: 'codigo' | 'cliente'): void {
    this.tipoBusqueda = tipo;
    this.errorBusqueda = '';
    this.terminoBusqueda = '';
    this.setFilteredProformas(this.aplicarFiltroFecha(this.todasLasProformas));
  }

  get pagedProformas(): ProformaItem[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.proformas.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.proformas.length / this.pageSize));
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages || page === this.currentPage) {
      return;
    }
    this.currentPage = page;
  }

  goToNextPage(): void {
    this.goToPage(this.currentPage + 1);
  }

  goToPrevPage(): void {
    this.goToPage(this.currentPage - 1);
  }

  private setFilteredProformas(items: ProformaItem[]): void {
    this.proformas = [...(items || [])].sort((a, b) => {
      const aTime = this.toTimestamp(a?.createdAt);
      const bTime = this.toTimestamp(b?.createdAt);
      return bTime - aTime;
    });
    this.currentPage = 1;
  }

  private toTimestamp(value?: string): number {
    if (!value) {
      return 0;
    }

    const normalized = value.includes('T') ? value : value.replace(' ', 'T');
    const time = new Date(normalized).getTime();
    return Number.isNaN(time) ? 0 : time;
  }

  private aplicarFiltroFecha(items: ProformaItem[]): ProformaItem[] {
    if (!this.selectedFecha) {
      return [...(items || [])];
    }

    return (items || []).filter((item) => {
      const raw = item?.createdAt;
      if (!raw) {
        return false;
      }

      const normalized = raw.includes('T') ? raw : raw.replace(' ', 'T');
      const date = new Date(normalized);

      if (Number.isNaN(date.getTime())) {
        return false;
      }

      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}` === this.selectedFecha;
    });
  }

  private normalizarTextoBusqueda(value: string): string {
    return (value || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/\s+/g, ' ')
      .trim()
      .toUpperCase();
  }

  private normalizarCodigo(value: string): string {
    return this.normalizarTextoBusqueda(value).replace(/[^A-Z0-9]/g, '');
  }

  irAProformas(): void {
    this.router.navigate(['/proformas']);
  }

  toggleDropdown(): void {
    this.showDropdown = !this.showDropdown;
  }

  goToPerfil(): void {
    this.showDropdown = false;
    this.router.navigate(['/profile']);
  }

  logout(): void {
    this.showDropdown = false;
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  verPdf(proforma: ProformaItem): void {
    this.proformaService.verPdfProforma(proforma.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 10000);
      },
      error: () => {
        this.errorBusqueda = 'No se pudo abrir el PDF de la proforma.';
      }
    });
  }
}
