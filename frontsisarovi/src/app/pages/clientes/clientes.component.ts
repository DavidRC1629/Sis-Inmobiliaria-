import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize, timeout } from 'rxjs/operators';
import { Cliente, ClienteLoteOption, ClienteProjectSummary, ClienteRequest } from '../../models/cliente.model';
import { Project } from '../../models/project.model';
import { ClienteService } from '../../services/cliente.service';
import { ProjectService } from '../../services/project.service';

type ClientesView = 'cards' | 'project' | 'history';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './clientes.component.html',
  styleUrls: ['./clientes.component.css']
})
export class ClientesComponent implements OnInit {
  private router = inject(Router);
  private clienteService = inject(ClienteService);
  private projectService = inject(ProjectService);
  private cdr = inject(ChangeDetectorRef);

  view: ClientesView = 'cards';
  cardsLoading = false;
  cardsError = '';
  projectClientsLoading = false;
  historyLoading = false;

  projectCards: ClienteProjectSummary[] = [];
  selectedProject: ClienteProjectSummary | null = null;
  selectedProjectClients: Cliente[] = [];
  projectClientsError = '';
  historialClientes: Cliente[] = [];
  private projectClientsCache: Record<number, Cliente[]> = {};

  historyQuery = '';
  historySearchBy: 'nombre' | 'dni' = 'nombre';
  historyPageSize = 15;
  readonly historyPageSizeOptions: number[] = [15, 25];
  historyCurrentPage = 1;

  showEditModal = false;
  editingCliente: Cliente | null = null;
  editLotes: ClienteLoteOption[] = [];
  savingEdit = false;
  private saveEditFallbackTimer: ReturnType<typeof setTimeout> | null = null;

  editForm: ClienteRequest = {
    nombres: '',
    apellidos: '',
    dni: '',
    email: '',
    telefono: '',
    direccion: '',
    tipoRelacion: 'ADQUISICION',
    clienteDesde: '',
    loteId: 0
  };

  notification: { type: 'success' | 'error'; message: string } | null = null;

  ngOnInit(): void {
    this.view = 'cards';
    this.selectedProject = null;
    this.selectedProjectClients = [];

    const cachedSummaries = this.clienteService.getProjectSummariesSnapshot();
    if (cachedSummaries.length > 0) {
      this.projectCards = [...cachedSummaries]
        .sort((a, b) => a.projectNombre.localeCompare(b.projectNombre, 'es', { sensitivity: 'base' }));
    }

    const cachedHistorial = this.clienteService.getHistorialSnapshot('');
    if (cachedHistorial.length > 0) {
      this.historialClientes = this.sortClientes(cachedHistorial);
      this.hydrateProjectCardsFromHistorial();
    }

    const cachedProjects = this.projectService.getCachedProjectsSnapshot();
    if (this.projectCards.length === 0 && cachedProjects.length > 0) {
      this.projectCards = cachedProjects
        .map((project) => ({
          projectId: project.id,
          projectNombre: project.nombre,
          cantidadClientes: 0
        }))
        .sort((a, b) => a.projectNombre.localeCompare(b.projectNombre, 'es', { sensitivity: 'base' }));
    }

    this.loadProjectCards(true);
    this.loadHistorial('');
  }

  goBackDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  goToCards(): void {
    this.view = 'cards';
    this.selectedProject = null;
    this.selectedProjectClients = [];
    this.closeEditModal();
  }

  goToHistory(): void {
    this.view = 'history';
    this.closeEditModal();
    this.historyQuery = '';
    this.historySearchBy = 'nombre';
    this.historyCurrentPage = 1;

    if (this.historialClientes.length === 0) {
      this.seedHistorialFromProjectCache();
    }

    this.loadHistorial('');
  }

  openProject(project: ClienteProjectSummary): void {
    this.selectedProject = project;
    this.view = 'project';
    this.closeEditModal();

    const cached = this.projectClientsCache[project.projectId];
    if (Array.isArray(cached) && cached.length > 0) {
      this.selectedProjectClients = [...cached];
      this.loadProjectClients(project.projectId, true);
      return;
    }

    const persisted = this.clienteService.getClientesByProjectSnapshot(project.projectId);
    if (persisted.length > 0) {
      this.projectClientsCache[project.projectId] = [...persisted];
      this.selectedProjectClients = [...persisted];
      this.syncProjectClientCount(project.projectId, persisted.length);
      this.loadProjectClients(project.projectId, true);
      return;
    }

    this.loadProjectClients(project.projectId, false);
  }

  onCronogramaClick(cliente: Cliente): void {
    this.router.navigate(['/cronogramas'], {
      queryParams: {
        dni: cliente.dni,
        nombres: `${cliente.nombres} ${cliente.apellidos}`
      }
    });
  }

  onLoteInfoClick(): void {
    this.showNotification('success', 'En proceso');
  }

  runHistorySearch(): void {
    this.historyCurrentPage = 1;
    const q = this.historyQuery.trim();
    if (!q) {
      this.loadHistorial('');
      return;
    }

    this.loadHistorial(q);
  }

  clearHistorySearch(): void {
    this.historyQuery = '';
    this.historyCurrentPage = 1;
    this.loadHistorial('');
  }

  onHistoryPageSizeChange(): void {
    this.historyCurrentPage = 1;
  }

  goToHistoryPage(page: number): void {
    if (page < 1 || page > this.totalHistoryPages) {
      return;
    }
    this.historyCurrentPage = page;
  }

  get pagedHistorialClientes(): Cliente[] {
    const start = (this.historyCurrentPage - 1) * this.historyPageSize;
    const end = start + this.historyPageSize;
    return this.historialClientes.slice(start, end);
  }

  get totalHistoryPages(): number {
    const total = this.historialClientes.length;
    return Math.max(1, Math.ceil(total / this.historyPageSize));
  }

  openEditModal(cliente: Cliente): void {
    this.editingCliente = cliente;
    this.editForm = {
      nombres: cliente.nombres,
      apellidos: cliente.apellidos,
      dni: cliente.dni,
      email: cliente.email || '',
      telefono: cliente.telefono,
      direccion: cliente.direccion,
      tipoRelacion: cliente.tipoRelacion,
      clienteDesde: cliente.clienteDesde,
      loteId: cliente.loteId
    };

    this.showEditModal = true;
    this.loadEditLotes(cliente.projectId, cliente.id);
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editingCliente = null;
    this.editLotes = [];
    this.savingEdit = false;
    if (this.saveEditFallbackTimer) {
      clearTimeout(this.saveEditFallbackTimer);
      this.saveEditFallbackTimer = null;
    }
  }

  saveEdit(): void {
    if (!this.editingCliente) {
      return;
    }

    if (!this.editForm.nombres.trim() || !this.editForm.apellidos.trim() || !this.editForm.dni.trim() ||
        !this.editForm.telefono.trim() || !this.editForm.direccion.trim() || !this.editForm.clienteDesde || !this.editForm.loteId) {
      this.showNotification('error', 'Completa los campos obligatorios del cliente.');
      return;
    }

    if (!confirm('¿Confirmas actualizar los datos del cliente?')) {
      return;
    }

    this.savingEdit = true;
    if (this.saveEditFallbackTimer) {
      clearTimeout(this.saveEditFallbackTimer);
    }
    this.saveEditFallbackTimer = setTimeout(() => {
      if (this.savingEdit) {
        this.savingEdit = false;
        this.showNotification('error', 'La actualización tardó demasiado. Intenta nuevamente.');
        this.cdr.detectChanges();
      }
    }, 16000);

    this.clienteService
      .updateCliente(this.editingCliente.id, this.editForm)
      .pipe(
        timeout(15000),
        finalize(() => {
          this.savingEdit = false;
          if (this.saveEditFallbackTimer) {
            clearTimeout(this.saveEditFallbackTimer);
            this.saveEditFallbackTimer = null;
          }
        })
      )
      .subscribe({
        next: (updatedCliente) => {
          this.applyUpdatedClienteLocally(updatedCliente);
          this.showNotification('success', 'Cliente actualizado correctamente.');
          this.closeEditModal();
          this.historyCurrentPage = 1;
          if (this.selectedProject) {
            this.loadProjectClients(this.selectedProject.projectId, true);
          }
          this.loadProjectCards();
          this.loadHistorial(this.historyQuery.trim());
        },
        error: (error) => {
          const message = error?.error?.message || 'No se pudo actualizar el cliente.';
          this.showNotification('error', message);
        }
      });
  }

  deleteCliente(cliente: Cliente): void {
    if (!confirm(`¿Confirmas eliminar al cliente ${cliente.nombres} ${cliente.apellidos}?`)) {
      return;
    }

    this.historyLoading = true;
    this.clienteService
      .deleteCliente(cliente.id)
      .pipe(
        timeout(15000),
        finalize(() => {
          this.historyLoading = false;
        })
      )
      .subscribe({
        next: () => {
          this.showNotification('success', 'Cliente eliminado correctamente.');
          this.loadHistorial(this.historyQuery.trim());
          if (this.selectedProject) {
            this.loadProjectClients(this.selectedProject.projectId);
            this.loadProjectCards();
          }
        },
        error: (error) => {
          const message = error?.error?.message || 'No se pudo eliminar el cliente.';
          this.showNotification('error', message);
        }
      });
  }

  formatClienteDesde(value: string): string {
    if (!value) {
      return '-';
    }
    return new Date(`${value}T00:00:00`).toLocaleDateString('es-PE');
  }

  getHistoryQueryValue(): string {
    const q = this.historyQuery.trim();
    if (!q) {
      return '';
    }

    if (this.historySearchBy === 'dni') {
      return q;
    }

    return q;
  }

  private loadProjectCards(forceRefresh = false): void {
    const hasVisibleData = this.projectCards.length > 0;
    this.cardsLoading = !hasVisibleData;
    this.cardsError = '';

    const summaries$ = this.clienteService.getProjectSummaries()
      .pipe(
        timeout(15000),
        catchError(() => of([] as ClienteProjectSummary[]))
      );

    const projects$ = this.projectService.getAllProjects(forceRefresh)
      .pipe(
        timeout(15000),
        catchError(() => of([] as Project[]))
      );

    forkJoin({ summaries: summaries$, projects: projects$ })
      .pipe(finalize(() => {
        if (!hasVisibleData) {
          this.cardsLoading = false;
        }
      }))
      .subscribe({
        next: ({ summaries, projects }) => {
          const normalizedSummaries = Array.isArray(summaries) ? summaries : [];
          const normalizedProjects = Array.isArray(projects) ? projects : [];

          const summaryByProject = new Map<number, number>(
            normalizedSummaries.map((s) => [s.projectId, Number(s.cantidadClientes || 0)])
          );

          if (normalizedProjects.length > 0) {
            this.projectCards = normalizedProjects
              .map((project) => ({
                projectId: project.id,
                projectNombre: project.nombre,
                cantidadClientes: summaryByProject.get(project.id) ?? 0
              }))
              .sort((a, b) => a.projectNombre.localeCompare(b.projectNombre, 'es', { sensitivity: 'base' }));
          } else {
            this.projectCards = [...normalizedSummaries]
              .sort((a, b) => a.projectNombre.localeCompare(b.projectNombre, 'es', { sensitivity: 'base' }));
          }

          this.cardsError = '';
          if (this.projectCards.length === 0) {
            this.loadHistorial('');
          }
          this.prefetchProjectClients();
          this.cdr.detectChanges();
        },
        error: () => {
          this.cardsError = 'No se pudieron cargar los proyectos. Verifica tu sesión e inténtalo nuevamente.';
          this.showNotification('error', this.cardsError);
          if (this.projectCards.length === 0) {
            this.loadHistorial('');
          }
          this.cdr.detectChanges();
        }
      });
  }

  private loadProjectClients(projectId: number, keepCurrent = false): void {
    const persisted = this.clienteService.getClientesByProjectSnapshot(projectId);
    if (persisted.length > 0) {
      this.selectedProjectClients = [...persisted];
      this.projectClientsCache[projectId] = [...persisted];
      this.syncProjectClientCount(projectId, persisted.length);
      keepCurrent = true;
    }

    const hasVisibleData = keepCurrent || this.selectedProjectClients.length > 0;
    this.projectClientsLoading = !hasVisibleData;
    if (!keepCurrent) {
      this.selectedProjectClients = [];
    }
    this.projectClientsError = '';

    this.clienteService
      .getClientesByProject(projectId)
      .pipe(
        timeout(10000),
        finalize(() => {
          if (!hasVisibleData) {
            this.projectClientsLoading = false;
          }
        })
      )
      .subscribe({
        next: (clientes) => {
          this.selectedProjectClients = this.sortClientes(Array.isArray(clientes) ? clientes : []);
          this.projectClientsCache[projectId] = [...this.selectedProjectClients];
          this.projectClientsError = '';
          this.syncProjectClientCount(projectId, this.selectedProjectClients.length);
        },
        error: (error) => {
          if (!keepCurrent) {
            this.selectedProjectClients = [];
          }
          this.projectClientsError = error?.error?.message || 'No se pudieron cargar los clientes del proyecto.';
          this.showNotification('error', this.projectClientsError);
        }
      });
  }

  private loadHistorial(query = ''): void {
    const q = query || this.getHistoryQueryValue();
    const cached = this.clienteService.getHistorialSnapshot(q);
    const hasVisibleData = cached.length > 0 || (!q && this.historialClientes.length > 0);

    if (cached.length > 0) {
      this.historialClientes = this.sortClientes(cached);
      this.hydrateProjectCardsFromHistorial();
      this.historyCurrentPage = 1;
    }

    this.historyLoading = !hasVisibleData;

    this.clienteService
      .getHistorial(q)
      .pipe(
        timeout(15000),
        finalize(() => {
          if (!hasVisibleData) {
            this.historyLoading = false;
          }
        })
      )
      .subscribe({
        next: (clientes) => {
          const normalized = Array.isArray(clientes) ? clientes : [];
          this.historialClientes = this.sortClientes(normalized);
          this.hydrateProjectCardsFromHistorial();
          this.historyCurrentPage = 1;
        },
        error: () => {
          if (!hasVisibleData) {
            this.historialClientes = [];
            this.showNotification('error', 'No se pudo cargar el historial de clientes.');
          }
        }
      });
  }

  private sortClientes(clientes: Cliente[]): Cliente[] {
    return [...clientes].sort((a, b) => {
      const dateA = new Date(`${a.clienteDesde || ''}T00:00:00`).getTime();
      const dateB = new Date(`${b.clienteDesde || ''}T00:00:00`).getTime();
      if (dateA !== dateB) {
        return dateB - dateA;
      }
      return Number(b.id || 0) - Number(a.id || 0);
    });
  }

  private loadEditLotes(projectId: number, clienteId: number): void {
    this.clienteService
      .getLotesDisponibles(projectId, clienteId)
      .pipe(timeout(15000))
      .subscribe({
        next: (lotes) => {
          this.editLotes = Array.isArray(lotes) ? lotes : [];
        },
        error: () => {
          this.editLotes = [];
          this.showNotification('error', 'No se pudieron cargar los lotes disponibles.');
        }
      });
  }

  private showNotification(type: 'success' | 'error', message: string): void {
    this.notification = { type, message };
    setTimeout(() => {
      this.notification = null;
    }, 3500);
  }

  private syncProjectClientCount(projectId: number, count: number): void {
    this.projectCards = this.projectCards.map((project) =>
      project.projectId === projectId
        ? { ...project, cantidadClientes: count }
        : project
    );

    if (this.selectedProject?.projectId === projectId) {
      this.selectedProject = { ...this.selectedProject, cantidadClientes: count };
    }
  }

  private prefetchProjectClients(): void {
    this.projectCards
      .filter((project) => Number(project.cantidadClientes || 0) > 0)
      .forEach((project) => {
        const projectId = project.projectId;
        if (this.projectClientsCache[projectId]?.length) {
          return;
        }

        this.clienteService
          .getClientesByProject(projectId)
          .pipe(
            timeout(10000),
            catchError(() => of([] as Cliente[]))
          )
          .subscribe((clientes) => {
            const list = this.sortClientes(Array.isArray(clientes) ? clientes : []);
            this.projectClientsCache[projectId] = [...list];
            this.syncProjectClientCount(projectId, list.length);
          });
      });
  }

  private applyUpdatedClienteLocally(updatedCliente: Cliente | null | undefined): void {
    if (!updatedCliente?.id) {
      return;
    }

    const replaceInList = (list: Cliente[]): Cliente[] => {
      const next = list.map((item) => item.id === updatedCliente.id ? updatedCliente : item);
      return this.sortClientes(next);
    };

    if (this.historialClientes.some((item) => item.id === updatedCliente.id)) {
      this.historialClientes = replaceInList(this.historialClientes);
    }

    Object.keys(this.projectClientsCache).forEach((projectIdKey) => {
      const projectId = Number(projectIdKey);
      const list = this.projectClientsCache[projectId] || [];
      if (list.some((item) => item.id === updatedCliente.id)) {
        this.projectClientsCache[projectId] = replaceInList(list);
      }
    });

    if (this.selectedProject) {
      this.selectedProjectClients = replaceInList(this.selectedProjectClients);
      this.syncProjectClientCount(this.selectedProject.projectId, this.selectedProjectClients.length);
    }

    this.hydrateProjectCardsFromHistorial();
    this.cdr.detectChanges();
  }

  private seedHistorialFromProjectCache(): void {
    const clientsById = new Map<number, Cliente>();

    Object.values(this.projectClientsCache)
      .flat()
      .forEach((cliente) => {
        if (cliente?.id != null) {
          clientsById.set(cliente.id, cliente);
        }
      });

    const seeded = Array.from(clientsById.values()).sort((a, b) => {
      const dateA = new Date(`${a.clienteDesde || ''}T00:00:00`).getTime();
      const dateB = new Date(`${b.clienteDesde || ''}T00:00:00`).getTime();
      if (dateA !== dateB) {
        return dateB - dateA;
      }
      return Number(b.id || 0) - Number(a.id || 0);
    });

    if (seeded.length > 0) {
      this.historialClientes = seeded;
      this.hydrateProjectCardsFromHistorial();
    }
  }

  private hydrateProjectCardsFromHistorial(): void {
    if (!Array.isArray(this.historialClientes) || this.historialClientes.length === 0) {
      return;
    }

    const grouped = new Map<number, { projectNombre: string; cantidadClientes: number }>();

    this.historialClientes.forEach((cliente) => {
      const projectId = Number(cliente.projectId || 0);
      if (!projectId) {
        return;
      }

      const current = grouped.get(projectId);
      if (current) {
        current.cantidadClientes += 1;
        return;
      }

      grouped.set(projectId, {
        projectNombre: cliente.projectNombre || `Proyecto ${projectId}`,
        cantidadClientes: 1
      });
    });

    if (grouped.size === 0) {
      return;
    }

    if (this.projectCards.length === 0) {
      this.projectCards = Array.from(grouped.entries())
        .map(([projectId, data]) => ({
          projectId,
          projectNombre: data.projectNombre,
          cantidadClientes: data.cantidadClientes
        }))
        .sort((a, b) => a.projectNombre.localeCompare(b.projectNombre, 'es', { sensitivity: 'base' }));
      return;
    }

    this.projectCards = this.projectCards.map((project) => {
      const fromHistory = grouped.get(project.projectId);
      if (!fromHistory) {
        return project;
      }
      return {
        ...project,
        projectNombre: fromHistory.projectNombre || project.projectNombre,
        cantidadClientes: fromHistory.cantidadClientes
      };
    });

    const existingIds = new Set(this.projectCards.map((project) => project.projectId));
    const missingCards: ClienteProjectSummary[] = Array.from(grouped.entries())
      .filter(([projectId]) => !existingIds.has(projectId))
      .map(([projectId, data]) => ({
        projectId,
        projectNombre: data.projectNombre,
        cantidadClientes: data.cantidadClientes
      }));

    if (missingCards.length > 0) {
      this.projectCards = [...this.projectCards, ...missingCards]
        .sort((a, b) => a.projectNombre.localeCompare(b.projectNombre, 'es', { sensitivity: 'base' }));
    }
  }
}