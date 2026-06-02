import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {
  private router = inject(Router);
  private authService = inject(AuthService);

  email = '';
  submitted = false;
  loading = false;
  errorMessage = '';
  successMessage = '';

  sendRecoveryMail(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const normalized = this.email.trim().toLowerCase();
    if (!normalized) {
      this.errorMessage = 'Ingrese un correo electrónico.';
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(normalized)) {
      this.errorMessage = 'Ingrese un correo válido.';
      return;
    }

    this.loading = true;
    this.authService.requestPasswordRecovery(normalized)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
      next: () => {
        this.submitted = true;
        this.successMessage = 'Te enviamos un código temporal de 8 caracteres a tu correo. Expira en 10 minutos.';
        this.router.navigate(['/login'], {
          queryParams: {
            recovery: 'sent',
            email: normalized
          }
        });
      },
      error: (err) => {
        this.submitted = false;
        const backendMessage = (err?.error?.message || '').toString();

        if (backendMessage.toLowerCase().includes('resend está en modo de prueba')) {
          this.errorMessage =
            'No se pudo enviar al correo indicado porque Resend está en modo prueba. ' +
            'Debes verificar un dominio en Resend y configurar RESEND_FROM_EMAIL para habilitar envíos a cualquier destinatario.';
          return;
        }

        this.errorMessage = backendMessage || 'El correo ingresado no está registrado en el sistema.';
      }
    });
  }

  goToResetView(): void {
    this.router.navigate(['/change-temporary-password'], {
      queryParams: { email: this.email.trim().toLowerCase() }
    });
  }
}
