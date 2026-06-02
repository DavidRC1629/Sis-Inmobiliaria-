import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { UserManagementService } from '../../services/user-management.service';
import { User } from '../../models/user.model';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-gestion-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './gestion-usuarios.component.html',
  styleUrls: ['./gestion-usuarios.component.css']
})
export class GestionUsuariosComponent implements OnInit {
  private userService = inject(UserManagementService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  currentTab: 'pendientes' | 'espera' | 'activos' | 'todos' = 'pendientes';
  userName: string = '';
  pendingUsers: User[] = [];
  waitingUsers: User[] = [];
  activeUsers: User[] = [];
  allUsers: User[] = [];
  isAdmin: boolean = false;
  showActionModal = false;
  actionType: 'approve' | 'reject' | 'cancel-wait' | null = null;
  actionUser: User | null = null;
  actionError = '';
  
  showDeleteModal = false;
  deletePassword = '';
  deleteError = '';
  userToDelete: number | null = null;
  isLoading = false;

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    // Load current user's name for header display
    try {
      const userStr = localStorage.getItem('currentUser');
      if (userStr) {
        const u = JSON.parse(userStr);
        this.userName = `${u.nombres || ''} ${u.primerApellido || ''}`.trim();
      }
    } catch (e) {
      // ignore
    }
    this.refreshAllLists();
  }

  onTabChange(tab: 'pendientes' | 'espera' | 'activos' | 'todos') {
    this.currentTab = tab;
    this.refreshAllLists();
  }

  refreshAllLists(): void {
    this.loadPendingUsers();
    this.loadWaitingUsers();
    this.loadActiveUsers();
    this.loadAllUsers();
  }

  loadAllUsers(): void {
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.allUsers = users;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Error cargando todos los usuarios:', err);
        this.cdr.detectChanges();
      }
    });
  }

  loadPendingUsers(): void {
    this.isLoading = true;
    this.userService.getPendingUsers().subscribe({
      next: (users) => {
        this.pendingUsers = users;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Error cargando usuarios pendientes:', err);
        this.cdr.detectChanges();
      }
    });
  }

  loadWaitingUsers(): void {
    this.isLoading = true;
    this.userService.getRejectedWaitingUsers().subscribe({
      next: (users) => {
        this.waitingUsers = users;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Error cargando usuarios en espera:', err);
        this.cdr.detectChanges();
      }
    });
  }

  loadActiveUsers(): void {
    this.isLoading = true;
    this.userService.getActiveUsers().subscribe({
      next: (users) => {
        this.activeUsers = users;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Error cargando usuarios activos:', err);
        this.cdr.detectChanges();
      }
    });
  }

  openActionModal(type: 'approve' | 'reject' | 'cancel-wait', user: User): void {
    this.actionType = type;
    this.actionUser = user;
    this.actionError = '';
    this.showActionModal = true;
  }

  confirmAction(): void {
    if (!this.actionUser || !this.actionType) {
      return;
    }

    const userId = this.actionUser.id;
    const action = this.actionType;

    const request$ = action === 'approve'
      ? this.userService.approveUser(userId)
      : action === 'reject'
        ? this.userService.rejectUser(userId)
        : this.userService.cancelWaitingPeriod(userId);

    request$.subscribe({
      next: (updatedUser) => {
        if (action === 'approve') {
          this.pendingUsers = this.pendingUsers.filter(user => user.id !== userId);
          this.waitingUsers = this.waitingUsers.filter(user => user.id !== userId);
          this.updateUserInCollection(this.allUsers, { ...this.actionUser!, ...updatedUser, estado: 'ACTIVO', rejectionExpiresAt: null });
          this.currentTab = 'activos';
        }

        if (action === 'reject') {
          this.pendingUsers = this.pendingUsers.filter(user => user.id !== userId);
          this.updateUserInCollection(this.allUsers, updatedUser);
          this.updateUserInCollection(this.waitingUsers, updatedUser);
          this.currentTab = 'espera';
        }

        if (action === 'cancel-wait') {
          this.waitingUsers = this.waitingUsers.filter(user => user.id !== userId);
          this.updateUserInCollection(this.allUsers, updatedUser);
        }

        this.refreshAllLists();

        this.closeActionModal();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.actionError = err?.error?.message || err?.message || 'No se pudo completar la acción';
        console.error('Error ejecutando acción:', err);
        this.cdr.detectChanges();
      }
    });
  }

  closeActionModal(): void {
    this.showActionModal = false;
    this.actionType = null;
    this.actionUser = null;
    this.actionError = '';
  }

  approveUser(user: User): void {
    this.openActionModal('approve', user);
  }

  rejectUser(user: User): void {
    this.openActionModal('reject', user);
  }

  cancelWaitingPeriod(user: User): void {
    this.openActionModal('cancel-wait', user);
  }

  promoteUser(userId: number): void {
    if (confirm('¿Promover este usuario a Administrador?')) {
      this.userService.promoteToAdmin(userId).subscribe({
        next: () => {
          alert('Usuario promovido a Administrador');
          this.loadActiveUsers();
          if (this.isAdmin && this.currentTab === 'todos') {
            this.loadAllUsers();
          }
          this.cdr.detectChanges();
        },
        error: (err) => {
          const message = err?.error?.message || err?.message || 'No se pudo promover al usuario';
          alert(message);
        }
      });
    }
  }

  deleteUser(userId: number): void {
    this.userToDelete = userId;
    this.showDeleteModal = true;
    this.deletePassword = '';
    this.deleteError = '';
  }

  confirmDelete(): void {
    if (!this.deletePassword) {
      this.deleteError = 'Debes ingresar tu contraseña';
      return;
    }

    if (this.userToDelete) {
      this.userService.deleteUserWithPassword(this.userToDelete, this.deletePassword).subscribe({
        next: () => {
          alert('Usuario eliminado exitosamente');
          this.closeModal();
          this.currentTab = 'espera';
          this.refreshAllLists();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error eliminando usuario:', err);
          this.deleteError = err?.error?.message || err?.message || 'Contraseña incorrecta o error al eliminar';
        }
      });
    }
  }

  closeModal(): void {
    this.showDeleteModal = false;
    this.deletePassword = '';
    this.deleteError = '';
    this.userToDelete = null;
  }

  private updateUserInCollection(collection: User[], updatedUser: User): void {
    const index = collection.findIndex(user => user.id === updatedUser.id);
    if (index >= 0) {
      collection[index] = { ...collection[index], ...updatedUser };
    } else {
      collection.unshift(updatedUser);
    }
  }

  getWaitingLabel(user: User): string {
    if (!user.rejectionExpiresAt) {
      return 'Espera sin fecha';
    }

    const expiresAt = new Date(user.rejectionExpiresAt);
    const now = new Date();
    const diffMs = expiresAt.getTime() - now.getTime();
    const diffHours = Math.max(1, Math.ceil(diffMs / (1000 * 60 * 60)));

    return `Puede volver a solicitar en ${diffHours} hora(s)`;
  }

  getInitials(user: User): string {
    const first = user.nombres?.charAt(0) || '';
    const last = user.primerApellido?.charAt(0) || '';
    return (first + last).toUpperCase();
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  logout(): void {
    this.authService.logout();
  }
}
