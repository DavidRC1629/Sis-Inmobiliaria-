import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { StartupPreloadService } from '../../services/startup-preload.service';
import { LoginRequest } from '../../models/user.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private startupPreloadService = inject(StartupPreloadService);

  credentials: LoginRequest = {
    identifier: '',
    password: ''
  };

  error: string | null = null;
  loading = false;
  showPassword = false;

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    console.log('🔵 Login iniciado con identificador:', this.credentials.identifier);
    this.error = null;
    this.loading = true;

    this.authService.login(this.credentials).subscribe({
      next: (response) => {
        console.log('✅ Respuesta del servidor:', response);
        this.loading = false;
        // Si tiene token, es login exitoso
        if (response.token) {
          if (response.requirePasswordChange) {
            const email = (response.email || '').trim().toLowerCase();
            const temporaryCode = this.credentials.password.trim().toUpperCase();

            setTimeout(() => {
              this.cdr.detectChanges();
              this.router.navigate(['/change-temporary-password'], {
                queryParams: {
                  forceChange: 'true',
                  email,
                  temporaryCode
                }
              });
            }, 0);
            return;
          }

          console.log('✅ Token recibido, navegando a dashboard');
          this.startupPreloadService.preloadCoreData().subscribe({
            next: () => {
              setTimeout(() => {
                this.cdr.detectChanges();
                this.router.navigate(['/dashboard']);
              }, 0);
            },
            error: () => {
              setTimeout(() => {
                this.cdr.detectChanges();
                this.router.navigate(['/dashboard']);
              }, 0);
            }
          });
        } else if (response.message) {
          // Si no tiene token pero tiene mensaje, es un error
          console.log('⚠️ Login sin token, mensaje:', response.message);
          this.error = response.message;
        }
      },
      error: (err) => {
        console.error('❌ Error en login:', err);
        // Capturar el mensaje de error del backend
        this.loading = false;
        if (err.error?.message) {
          this.error = err.error.message;
        } else if (err.status === 403) {
          this.error = 'Tu cuenta no está habilitada para iniciar sesión. Contacta al administrador.';
        } else if (err.status === 401) {
          this.error = 'DNI o correo o contraseña incorrectos. Por favor, verifica tus credenciales.';
        } else if (err.status === 0) {
          this.error = 'No se puede conectar con el servidor. Verifica que el backend esté corriendo en el puerto 8080.';
        } else {
          this.error = 'Error desconocido. Intenta nuevamente.';
        }
        this.cdr.detectChanges(); // Fuerza actualización del error
      }
    });
  }
}
