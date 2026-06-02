
import { Component, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ReniecService } from '../../services/reniec.service';
import { RegisterRequest } from '../../models/user.model';


@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  private authService = inject(AuthService);
  private reniecService = inject(ReniecService);
  private router = inject(Router);
  private cdRef = inject(ChangeDetectorRef);

  showPassword = false;
  showConfirmPassword = false;
  loading = false;
  error: string | null = null;
  success: string | null = null;
  fieldErrors: Record<string, string> = {};

  registerData: RegisterRequest = {
    nombres: '',
    dni: '',
    email: '',
    password: '',
    primerApellido: '',
    segundoApellido: ''
  };
  toastError: string | null = null;
  get passwordMismatch(): boolean {
    return (
      this.registerData.password !== this.confirmPassword &&
      this.confirmPassword.length > 0
    );
  }
  confirmPassword: string = '';

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPasswordVisibility() {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onFieldChange(field?: string): void {
    if (field && this.fieldErrors[field as string]) {
      delete this.fieldErrors[field as string];
    }
    this.error = null;
  }

  onSubmit(): void {
    // First validate against RENIEC
    if (!this.registerData.dni || this.registerData.dni.length !== 8) {
      this.fieldErrors['dni'] = 'DNI debe tener 8 dígitos';
      return;
    }

    if (!this.registerData.primerApellido.trim()) {
      this.fieldErrors['primerApellido'] = 'Primer apellido es requerido';
      return;
    }

    if (!this.registerData.segundoApellido.trim()) {
      this.fieldErrors['segundoApellido'] = 'Segundo apellido es requerido';
      return;
    }

    if (!this.registerData.nombres.trim()) {
      this.fieldErrors['nombres'] = 'Nombres son requeridos';
      return;
    }

    const normalizedEmail = this.registerData.email?.trim().toLowerCase();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!normalizedEmail) {
      this.fieldErrors['email'] = 'Correo es requerido';
      return;
    }
    if (!emailRegex.test(normalizedEmail)) {
      this.fieldErrors['email'] = 'Correo no tiene un formato válido';
      return;
    }
    this.registerData.email = normalizedEmail;

    if (this.registerData.password !== this.confirmPassword) {
      this.error = 'Las contraseñas no coinciden';
      return;
    }

    this.loading = true;
    this.error = null;
    this.success = null;

    // Validate against RENIEC first
    this.reniecService.buscarPorDni(this.registerData.dni).subscribe({
      next: (res) => {
        const reniecData = Array.isArray(res) ? res[0] : res;
        
        if (!reniecData) {
          this.fieldErrors['dni'] = 'No se encontró registro en RENIEC para este DNI';
          this.loading = false;
          this.cdRef.detectChanges();
          return;
        }

        // Normalize and validate data
        const reniecNombres = (reniecData.name || reniecData.nombres || '').trim().toUpperCase();
        const reniecApellidos = (reniecData.surname || reniecData.apellidoPaterno || '').trim().toUpperCase();
        const userNombres = this.registerData.nombres.trim().toUpperCase();
        const userPrimerApellido = this.registerData.primerApellido.trim().toUpperCase();
        const userSegundoApellido = this.registerData.segundoApellido.trim().toUpperCase();

        // Validar nombres: flexible - si el nombre ingresado está contenido en los nombres de RENIEC
        const nombresParts: string[] = reniecNombres.split(/\s+/).filter(Boolean);
        const userNombresParts: string[] = userNombres.split(/\s+/).filter(Boolean);
        
        let nombresCoinciden = false;
        if (userNombresParts.length > 0) {
          nombresCoinciden = userNombresParts.every((userPart: string) => 
            nombresParts.some((reniecPart: string) => reniecPart === userPart)
          );
        }

        if (!nombresCoinciden) {
          this.fieldErrors['nombres'] = 'Los nombres no coinciden con tu registro en RENIEC';
          this.loading = false;
          this.cdRef.detectChanges();
          return;
        }

        // Validar apellidos por separado - cada uno debe estar en RENIEC
        const apellidosParts: string[] = reniecApellidos.split(/\s+/).filter(Boolean);
        
        // Validar primer apellido
        const primerApellidoEnReniec = apellidosParts.some((part: string) => part === userPrimerApellido);
        if (!primerApellidoEnReniec) {
          this.fieldErrors['primerApellido'] = 'El primer apellido no coincide con tu registro en RENIEC';
          this.loading = false;
          this.cdRef.detectChanges();
          return;
        }

        // Validar segundo apellido
        const segundoApellidoEnReniec = apellidosParts.some((part: string) => part === userSegundoApellido);
        if (!segundoApellidoEnReniec) {
          this.fieldErrors['segundoApellido'] = 'El segundo apellido no coincide con tu registro en RENIEC';
          this.loading = false;
          this.cdRef.detectChanges();
          return;
        }

        // All validations passed, proceed with registration
        this.fieldErrors = {};
        this.authService.register(this.registerData).subscribe({
          next: (response: any) => {
            this.loading = false;
            this.success = response.message || 'Registro exitoso';
            setTimeout(() => {
              this.router.navigate(['/login']);
            }, 1500);
          },
          error: (err: any) => {
            this.loading = false;
            this.fieldErrors = {};
            const backendMessage = err?.error?.message || err?.message || '';
            const normalizedMessage = String(backendMessage).toLowerCase();

            if (err?.field && err?.message) {
              this.fieldErrors[err.field] = err.message;
            } else if (normalizedMessage.includes('correo') && (normalizedMessage.includes('ya existe') || normalizedMessage.includes('ya se encuentra'))) {
              this.fieldErrors['email'] = 'El correo que colocaste ya se encuentra registrado';
              this.error = 'El correo que colocaste ya se encuentra registrado';
            } else if (err?.error?.message) {
              this.error = err.error.message;
            } else if (err?.message) {
              this.error = err.message;
            } else if (err?.status === 0) {
              this.error = 'No se pudo conectar con el servidor. Verifica que el backend esté levantado y que no haya problemas de conexión (CORS).';
            } else {
              this.error = 'Error desconocido al registrar. Intenta nuevamente.';
            }
            this.registerData.password = '';
            this.confirmPassword = '';
            this.cdRef.detectChanges();
          }
        });
      },
      error: (err) => {
        this.loading = false;
        console.error('❌ Error validando RENIEC en registro:', this.reniecService.getTechnicalErrorDetail(err), err);
        this.fieldErrors['dni'] = this.reniecService.getFriendlyErrorMessage(err);
        this.cdRef.detectChanges();
      }
    });
  }
}
