import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { User } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/admin/users';
  private allUsersCache: User[] = [];

  getPendingUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/pending`);
  }

  getRejectedWaitingUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/waiting`);
  }

  getActiveUsers(): Observable<User[]> {
    return this.getAllUsers().pipe(
      map(users => users.filter(user => user.estado === 'ACTIVO'))
    );
  }

  getAllUsersSync(): User[] {
    return this.allUsersCache;
  }

  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}`).pipe(
      tap(users => this.allUsersCache = users)
    );
  }

  approveUser(userId: number): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/${userId}/approve`, {});
  }

  rejectUser(userId: number): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/${userId}/reject`, {});
  }

  cancelWaitingPeriod(userId: number): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/${userId}/cancel-wait`, {});
  }

  promoteToAdmin(userId: number): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/${userId}/promote`, {});
  }

  deleteUserWithPassword(userId: number, password: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${userId}/with-password`, {
      body: { password }
    });
  }
}
