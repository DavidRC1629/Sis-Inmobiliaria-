
import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, tap, map, catchError, of } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/user.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID);
  private apiUrl = environment.apiUrl + '/auth';
  
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  currentUser$ = this.currentUserSubject.asObservable();
  private isBrowser: boolean;

  constructor() {
    this.isBrowser = isPlatformBrowser(this.platformId);
    
    // Verificar si hay un usuario guardado en localStorage (solo en navegador)
    if (this.isBrowser) {
      const token = this.getToken();
      if (token) {
        // Podrías validar el token aquí o cargar el usuario
        const userStr = localStorage.getItem('currentUser');
        if (userStr) {
          const user = JSON.parse(userStr) as User;
          this.currentUserSubject.next(user);
          if (!user.email) {
            this.refreshCurrentUserProfile().subscribe();
          }
        }
      }
    }
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        if (this.isBrowser && response.token) {
          this.saveToken(response.token);
          const user: User = {
            id: 0,
            dni: response.dni,
            email: response.email,
            nombres: response.nombres,
            primerApellido: response.primerApellido,
            segundoApellido: response.segundoApellido,
            role: { id: 0, name: response.role },
            estado: 'ACTIVO'
          };
          this.currentUserSubject.next(user);
          localStorage.setItem('currentUser', JSON.stringify(user));
        }
      })
    );
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, data).pipe(
      tap(response => {
        if (this.isBrowser && response.token) {
          this.saveToken(response.token);
          const user: User = {
            id: 0,
            dni: response.dni,
            email: data.email,
            nombres: response.nombres,
            primerApellido: response.primerApellido,
            segundoApellido: response.segundoApellido,
            role: { id: 0, name: response.role },
            estado: 'PENDIENTE'
          };
          this.currentUserSubject.next(user);
          localStorage.setItem('currentUser', JSON.stringify(user));
        }
      }),
      (source) => new Observable(observer => {
        source.subscribe({
          next: (val) => observer.next(val),
          error: (err) => {
            // Propagar error como { field, message } directamente
            if (err.error && typeof err.error === 'object' && err.error.field && err.error.message) {
              observer.error(err.error);
            } else if (err.error && typeof err.error !== 'object') {
              try {
                const parsed = JSON.parse(err.error);
                if (parsed.field && parsed.message) {
                  observer.error(parsed);
                } else {
                  observer.error(parsed);
                }
              } catch {
                observer.error({ message: err.error });
              }
            } else {
              observer.error(err);
            }
          },
          complete: () => observer.complete()
        });
      })
    );
  }

  requestPasswordRecovery(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/forgot-password/request`, { email });
  }

  confirmPasswordRecovery(email: string, temporaryCode: string, newPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/forgot-password/confirm`, {
      email,
      temporaryCode,
      newPassword
    });
  }

  refreshCurrentUserProfile(): Observable<User | null> {
    return this.http.get<AuthResponse>(`${this.apiUrl}/me`).pipe(
      map(response => {
        const user: User = {
          id: 0,
          dni: response.dni,
          email: response.email,
          nombres: response.nombres,
          primerApellido: response.primerApellido,
          segundoApellido: response.segundoApellido,
          role: { id: 0, name: response.role },
          estado: 'ACTIVO'
        };
        this.currentUserSubject.next(user);
        if (this.isBrowser) {
          localStorage.setItem('currentUser', JSON.stringify(user));
        }
        return user;
      }),
      catchError(() => of(null))
    );
  }

  logout(): void {
    if (this.isBrowser) {
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('currentUser');
    }
    this.router.navigate(['/login']);
    this.currentUserSubject.next(null);
  }

  getToken(): string | null {
    if (this.isBrowser) {
      return localStorage.getItem('jwt_token');
    }
    return null;
  }

  private saveToken(token: string): void {
    if (this.isBrowser) {
      localStorage.setItem('jwt_token', token);
    }
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    return !!token;
  }

  isAdmin(): boolean {
    const user = this.currentUserSubject.value;
    return user?.role?.name === 'ROLE_ADMIN';
  }

  deleteOwnAccount(userId: number, password: string): Observable<void> {
    return this.http.delete<void>(`http://localhost:8080/api/users/${userId}/with-password`, {
      body: { password }
    });
  }
}
