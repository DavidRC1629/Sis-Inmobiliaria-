import { Injectable, inject } from '@angular/core';
import { Observable, of, forkJoin } from 'rxjs';
import { catchError, map, shareReplay } from 'rxjs/operators';
import { ClienteService } from './cliente.service';
import { CronogramaService } from './cronograma.service';
import { ProjectService } from './project.service';

@Injectable({
  providedIn: 'root'
})
export class StartupPreloadService {
  private projectService = inject(ProjectService);
  private clienteService = inject(ClienteService);
  private cronogramaService = inject(CronogramaService);

  private preloadRequest$: Observable<void> | null = null;

  preloadCoreData(): Observable<void> {
    if (this.preloadRequest$) {
      return this.preloadRequest$;
    }

    this.preloadRequest$ = forkJoin({
      projects: this.projectService.getAllProjects().pipe(catchError(() => of([]))),
      projectSummaries: this.clienteService.getProjectSummaries().pipe(catchError(() => of([]))),
      historialClientes: this.clienteService.getHistorial('').pipe(catchError(() => of([]))),
      cronogramas: this.cronogramaService.listar().pipe(catchError(() => of([])))
    }).pipe(
      map(() => void 0),
      shareReplay(1)
    );

    return this.preloadRequest$;
  }

  reset(): void {
    this.preloadRequest$ = null;
  }
}