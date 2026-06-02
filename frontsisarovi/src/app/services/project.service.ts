import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { Project, ProjectRequest } from '../models/project.model';
import { AppRefreshService } from './app-refresh.service';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private readonly cacheKey = 'sisarovi.projects.cache';
  private http = inject(HttpClient);
  private refreshService = inject(AppRefreshService);
  private apiUrl = 'http://localhost:8080/api/projects';
  private projectsCache: Project[] | null = this.readCache();

  getCachedProjectsSnapshot(): Project[] {
    return this.projectsCache ? [...this.projectsCache] : [];
  }

  invalidateProjectsCache(): void {
    this.projectsCache = null;
    this.clearPersistedCache();
  }

  getAllProjects(forceRefresh = false): Observable<Project[]> {
    if (!forceRefresh && this.projectsCache) {
      return of([...this.projectsCache]);
    }

    return this.http.get<Project[]>(this.apiUrl).pipe(
      map((projects) => (Array.isArray(projects) ? projects : [])),
      tap((projects) => {
        this.projectsCache = [...projects];
        this.writeCache(this.projectsCache);
      })
    );
  }

  getProjectById(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.apiUrl}/${id}`);
  }

  getProjectsByUser(userId: number): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.apiUrl}/user/${userId}`);
  }

  createProject(project: ProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.apiUrl, project).pipe(
      tap(() => {
        this.invalidateProjectsCache();
        this.refreshService.notifyChange();
      })
    );
  }

  updateProject(id: number, project: ProjectRequest): Observable<Project> {
    return this.http.put<Project>(`${this.apiUrl}/${id}`, project).pipe(
      tap(() => {
        this.invalidateProjectsCache();
        this.refreshService.notifyChange();
      })
    );
  }

  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => {
        this.invalidateProjectsCache();
        this.refreshService.notifyChange();
      })
    );
  }

  private readCache(): Project[] | null {
    try {
      const raw = sessionStorage.getItem(this.cacheKey);
      if (!raw) {
        return null;
      }

      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed as Project[] : null;
    } catch {
      return null;
    }
  }

  private writeCache(projects: Project[]): void {
    try {
      sessionStorage.setItem(this.cacheKey, JSON.stringify(projects));
    } catch {
      // Ignore storage failures and keep in-memory cache only.
    }
  }

  private clearPersistedCache(): void {
    try {
      sessionStorage.removeItem(this.cacheKey);
    } catch {
      // Ignore storage failures.
    }
  }
}
