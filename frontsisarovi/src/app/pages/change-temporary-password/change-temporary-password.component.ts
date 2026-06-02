import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Router, RouterModule } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-change-temporary-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './change-temporary-password.component.html',
  styleUrls: ['./change-temporary-password.component.css']
})
export class ChangeTemporaryPasswordComponent {
  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  email = '';
  temporaryCode = '';
  forceChange = false;
  newPassword = '';
  confirmPassword = '';

  showNewPassword = false;
  showConfirmPassword = false;

  formError = '';
  successMessage = '';

  constructor() {
    this.route.queryParamMap.subscribe(params => {
      this.forceChange = (params.get('forceChange') || '').toLowerCase() === 'true';
      this.email = (params.get('email') || '').trim().toLowerCase();
      this.temporaryCode = (params.get('temporaryCode') || '').trim().toUpperCase();
    });
  }

  toggleNewPassword(): void {
    this.showNewPassword = !this.showNewPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  submitChange(): void {
    this.formError = '';
    this.successMessage = '';

    if (!this.newPassword || !this.confirmPassword) {
      this.formError = 'Complete todos los campos.';
      return;
    }

    if (!this.email || !this.temporaryCode) {
      this.formError = 'Complete todos los campos.';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.formError = 'Las contraseñas deben coincidir.';
      return;
    }

    this.authService
      .confirmPasswordRecovery(this.email.trim().toLowerCase(), this.temporaryCode.trim().toUpperCase(), this.newPassword)
      .subscribe({
        next: (response) => {
          this.successMessage = response?.message || 'Contraseña actualizada correctamente.';
          this.newPassword = '';
          this.confirmPassword = '';

          if (this.forceChange) {
            this.authService.logout();
            this.router.navigate(['/login']);
          }
        },
        error: (err) => {
          this.formError = err?.error?.message || 'No se pudo actualizar la contraseña.';
        }
      });
  }
}
