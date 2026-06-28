import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, catchError, forkJoin, of, takeUntil } from 'rxjs';
import { BuscarClienteModalComponent } from '../buscar-cliente-modal.component';
import { AuthService } from '../../services/auth.service';
import { AppRefreshService } from '../../services/app-refresh.service';
import { ClienteService } from '../../services/cliente.service';
import { ProformaService } from '../../services/proforma.service';
import { ProjectService } from '../../services/project.service';
import { StartupPreloadService } from '../../services/startup-preload.service';
import { User } from '../../models/user.model';
import { APP_BRAND } from '../../config/app-brand.config';

type SidebarGroup = 'proformas' | 'liberacion';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, BuscarClienteModalComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly authService = inject(AuthService);
  private readonly projectService = inject(ProjectService);
  private readonly clienteService = inject(ClienteService);
  private readonly proformaService = inject(ProformaService);
  private readonly startupPreloadService = inject(StartupPreloadService);
  private readonly refreshService = inject(AppRefreshService);
  private readonly router = inject(Router);

  currentUser: User | null = null;
  loadingSummary = true;
  sidebarSearch = '';
  sidebarVisible = true;
  showSearchModal = false;
  appBrand = APP_BRAND;
  
  expandedGroups: Record<SidebarGroup, boolean> = {
    proformas: true,
    liberacion: true
  };

  summaryCards = {
    projects: 0,
    clients: 0,
    lotsSold: 0,
    proformas: 0
  };

  ngOnInit(): void {
    // 1. Cargar usuario
    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe((user) => {
      this.currentUser = user;
    });

    // 2. Carga única inicial
    this.loadSummaryWithCache();

    // 3. Precarga de datos: Se ejecuta una sola vez al iniciar
    this.startupPreloadService.preloadCoreData()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.loadSummary(),
        error: () => console.log('Error en precarga, continuando...')
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // --- MÉTODOS DE NAVEGACIÓN Y UTILIDAD ---

  get isAdmin(): boolean { return this.currentUser?.role?.name === 'ROLE_ADMIN'; }
  get displayName(): string { return this.currentUser?.nombres || 'Usuario'; }
  get initials(): string {
    const first = this.currentUser?.nombres?.charAt(0) || '';
    const last = this.currentUser?.primerApellido?.charAt(0) || '';
    return (first + last).toUpperCase() || 'U';
  }

  toggleGroup(group: SidebarGroup): void { this.expandedGroups[group] = !this.expandedGroups[group]; }
  toggleSidebar(): void { this.sidebarVisible = !this.sidebarVisible; }
  searchClient(): void { this.showSearchModal = true; }
  closeSearchModal(): void { this.showSearchModal = false; }
  logout(): void { this.authService.logout(); }
  refreshNow(): void { this.loadSummary(); }

  // --- NAVEGACIÓN ---
  onClienteSeleccionado(cliente: any): void {
    this.showSearchModal = false;
    this.router.navigate(['/clientes'], { queryParams: { dni: cliente?.dni || '', nombres: cliente?.full_name || cliente?.nombres || '' } });
  }

  goToProfile(): void { this.router.navigate(['/profile']); }
  goToDashboard(): void { this.router.navigate(['/dashboard']); }
  goToGestionUsuarios(): void { this.router.navigate(['/gestion-usuarios']); }
  goToProyectos(): void { this.router.navigate(['/gestion-proyectos']); }
  goToLotesPropios(): void { this.router.navigate(['/terrenos-propios']); }
  goToCronogramas(): void { this.router.navigate(['/cronogramas']); }
  goToClientes(): void { this.router.navigate(['/clientes']); }
  goToLiberacion(): void { this.router.navigate(['/liberacion']); }
  goToControlDevoluciones(): void { this.router.navigate(['/devolucion']); }
  goToProformas(): void { this.router.navigate(['/proformas']); }
  goToHistorialProformas(): void { this.router.navigate(['/proformas/historial']); }
  goToRegistro(): void { this.router.navigate(['/registro']); }
  createProject(): void { this.router.navigate(['/gestion-proyectos']); }

  // --- LÓGICA DE DATOS ---

  private loadSummaryWithCache(): void {
    this.summaryCards = {
      projects: this.projectService.getCachedProjectsSnapshot().length,
      clients: this.clienteService.getProjectSummariesSnapshot().reduce((sum, item) => sum + Number(item?.cantidadClientes || 0), 0),
      lotsSold: this.clienteService.getHistorialSnapshot('').length,
      proformas: this.proformaService.getHistorialCache().length
    };
    this.loadSummary();
  }

  private loadSummary(): void {
    // Si ya estamos cargando, no disparar otra vez (prevención de bucles)
    if (!this.loadingSummary && this.summaryCards.projects !== 0) return;

    this.loadingSummary = true;

    forkJoin({
      projects: this.projectService.getAllProjects(true).pipe(catchError(() => of([]))),
      projectSummaries: this.clienteService.getProjectSummaries().pipe(catchError(() => of([]))),
      clientes: this.clienteService.getHistorial('').pipe(catchError(() => of([]))),
      proformas: this.proformaService.getHistorialProformas().pipe(catchError(() => of([])))
    })
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (data) => {
        const totalClientes = Array.isArray(data.projectSummaries)
          ? data.projectSummaries.reduce((sum, item) => sum + Number(item?.cantidadClientes || 0), 0)
          : 0;

        this.summaryCards = {
          projects: Array.isArray(data.projects) ? data.projects.length : 0,
          clients: totalClientes || (Array.isArray(data.clientes) ? data.clientes.length : 0),
          lotsSold: Array.isArray(data.clientes) ? data.clientes.length : 0,
          proformas: Array.isArray(data.proformas) ? data.proformas.length : 0
        };
        this.loadingSummary = false;
      },
      error: () => {
        this.loadingSummary = false;
      }
    });
  }
}
