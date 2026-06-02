import { Component, inject, OnInit, ChangeDetectorRef, HostListener, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';
import { ProjectService } from '../../services/project.service';
import { Project, ProjectRequest } from '../../models/project.model';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-gestion-proyectos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './gestion-proyectos.component.html',
  styleUrls: ['./gestion-proyectos.component.css']
})
export class GestionProyectosComponent implements OnInit {
  private projectService = inject(ProjectService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private ngZone = inject(NgZone);

  projects: Project[] = [];
  userName: string = '';
  showProjects: boolean = true;
  showModal = false;
  isEditing = false;
  currentProjectId: number | null = null;
  isLoading = false;
  viewMode: 'view' | 'edit' | 'delete' = 'view';
  showProfileMenu = false;
  
  // Formulario
  formData: ProjectRequest = {
    nombre: '',
    imagenUrl: '',
    cantidadEtapas: 1
  };
  
  formErrors = {
    nombre: '',
    cantidadEtapas: '',
    general: ''
  };

  selectedFile: File | null = null;
  imagePreview: string | null = null;
  MAX_MANZANAS = 27;

  ngOnInit(): void {
    const cachedProjects = this.projectService.getCachedProjectsSnapshot();
    if (cachedProjects.length > 0) {
      this.projects = cachedProjects;
      this.loadProjects(false, true);
    } else {
      this.loadProjects(false, false);
    }
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
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const dropdown = target.closest('.profile-dropdown');
    if (!dropdown && this.showProfileMenu) {
      this.showProfileMenu = false;
    }
  }

  loadProjects(showLoader = true, forceRefresh = false): void {
    console.log('🚀 Llamando a getAllProjects...');
    this.projectService.getAllProjects(forceRefresh).pipe(
      finalize(() => {
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (projects) => {
        console.log('✅ Proyectos recibidos:', projects);
        this.ngZone.run(() => {
          this.projects = Array.isArray(projects) ? projects : [];
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        console.error('❌ Error cargando proyectos:', err);
        this.ngZone.run(() => {
          this.projects = [];
          this.cdr.detectChanges();
        });
      },
      complete: () => {
        console.log('ℹ️ Observable completado');
      }
    });
  }

  openCreateModal(): void {
    this.isEditing = false;
    this.currentProjectId = null;
    this.resetForm();
    this.showModal = true;
    this.viewMode = 'view';
  }

  setViewMode(mode: 'view' | 'edit' | 'delete'): void {
    // Si ya está en ese modo, volver a modo normal
    if (this.viewMode === mode) {
      this.viewMode = 'view';
    } else {
      this.viewMode = mode;
    }
  }

  openEditModal(project: Project): void {
    this.isEditing = true;
    this.currentProjectId = project.id;
    this.formData = {
      nombre: project.nombre,
      imagenUrl: project.imagenUrl,
      cantidadEtapas: project.cantidadEtapas
    };
    this.imagePreview = project.imagenUrl || null;
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.resetForm();
  }

  resetForm(): void {
    this.formData = {
      nombre: '',
      imagenUrl: '',
      cantidadEtapas: 1
    };
    this.formErrors = {
      nombre: '',
      cantidadEtapas: '',
      general: ''
    };
    this.selectedFile = null;
    this.imagePreview = null;
  }

  validateForm(): boolean {
    let isValid = true;
    this.formErrors = {
      nombre: '',
      cantidadEtapas: '',
      general: ''
    };

    if (!this.formData.nombre || this.formData.nombre.trim() === '') {
      this.formErrors.nombre = 'El nombre del proyecto es obligatorio';
      isValid = false;
    }

    if (!this.formData.cantidadEtapas || this.formData.cantidadEtapas < 1) {
      this.formErrors.cantidadEtapas = 'Debe tener al menos 1 etapa';
      isValid = false;
    }

    return isValid;
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      
      // Crear preview de la imagen
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.imagePreview = e.target.result;
        this.cdr.detectChanges(); // Forzar actualización de la vista
      };
      reader.readAsDataURL(file);
    }
  }

  removeImage(): void {
    this.imagePreview = null;
    this.selectedFile = null;
    this.formData.imagenUrl = '';
    // Limpiar el input file
    const fileInput = document.getElementById('imagen') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  onSubmit(): void {
    if (!this.validateForm()) {
      return;
    }

    this.isLoading = true;

    // Guardar la imagen si está en el preview
    if (this.imagePreview) {
      this.formData.imagenUrl = this.imagePreview;
    }

    console.log('📤 Enviando datos al backend:', {
      isEditing: this.isEditing,
      projectId: this.currentProjectId,
      formData: this.formData
    });

    const operation = this.isEditing && this.currentProjectId
      ? this.projectService.updateProject(this.currentProjectId, this.formData)
      : this.projectService.createProject(this.formData);

    operation.subscribe({
      next: (response) => {
        console.log('✅ Respuesta del backend:', response);
        this.isLoading = false;
        this.closeModal();
        setTimeout(() => {
          this.loadProjects(false, true);
        }, 100);
        this.showSuccessMessage(this.isEditing ? 'actualizado' : 'creado');
      },
      error: (err) => {
        console.error('❌ Error guardando proyecto:', err);
        this.ngZone.run(() => {
          const backendMessage = this.extractBackendErrorMessage(err);
          const isDuplicateName = backendMessage.toLowerCase().includes('ya existe un proyecto con ese nombre')
            || backendMessage.toLowerCase().includes('nombre') && backendMessage.toLowerCase().includes('existe');

          this.formErrors.general = isDuplicateName
            ? 'Ya existe un proyecto con ese nombre. Usa un nombre diferente.'
            : 'Error al guardar el proyecto. ' + backendMessage;

          if (isDuplicateName) {
            this.formErrors.nombre = 'Este nombre ya está registrado';
          }

          this.isLoading = false;
          this.cdr.detectChanges();
          this.showErrorMessage(this.formErrors.general);
        });
      }
    });
  }

  private extractBackendErrorMessage(err: any): string {
    if (!err) {
      return 'Verifica los datos.';
    }

    if (typeof err.error === 'string' && err.error.trim().length > 0) {
      return err.error;
    }

    if (err.error?.message && String(err.error.message).trim().length > 0) {
      return String(err.error.message);
    }

    if (err.message && String(err.message).trim().length > 0) {
      return String(err.message);
    }

    return 'Verifica los datos.';
  }

  showSuccessMessage(action: string): void {
    // Crear un mensaje temporal bonito en lugar de alert
    const message = document.createElement('div');
    message.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
      color: white;
      padding: 1.5rem 2rem;
      border-radius: 12px;
      box-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
      z-index: 10000;
      font-size: 1.1rem;
      font-weight: 600;
      animation: slideInRight 0.4s ease-out;
    `;
    message.innerHTML = `
      <div style="display: flex; align-items: center; gap: 10px;">
        <span style="font-size: 1.5rem;">✓</span>
        <span>Proyecto ${action} exitosamente</span>
      </div>
    `;
    document.body.appendChild(message);

    // Agregar la animación
    const style = document.createElement('style');
    style.textContent = `
      @keyframes slideInRight {
        from {
          transform: translateX(400px);
          opacity: 0;
        }
        to {
          transform: translateX(0);
          opacity: 1;
        }
      }
    `;
    document.head.appendChild(style);

    // Remover después de 3 segundos
    setTimeout(() => {
      message.style.animation = 'slideInRight 0.4s ease-out reverse';
      setTimeout(() => {
        document.body.removeChild(message);
        document.head.removeChild(style);
      }, 400);
    }, 3000);
  }

  deleteProject(project: Project): void {
    // Crear modal de confirmación personalizado
    const confirmDiv = document.createElement('div');
    confirmDiv.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.6);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 10000;
    `;
    
    confirmDiv.innerHTML = `
      <div style="
        background: white;
        border-radius: 15px;
        padding: 2rem;
        max-width: 400px;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
      ">
        <h3 style="color: #333; margin: 0 0 1rem 0;">⚠️ Confirmar Eliminación</h3>
        <p style="color: #666; margin-bottom: 1.5rem;">
          ¿Estás seguro de eliminar el proyecto "<strong>${project.nombre}</strong>"?
          <br><br>
          Esta acción no se puede deshacer.
        </p>
        <div style="display: flex; gap: 1rem;">
          <button id="cancelBtn" style="
            flex: 1;
            padding: 0.8rem;
            border: none;
            border-radius: 8px;
            background: #e0e0e0;
            color: #666;
            font-weight: 600;
            cursor: pointer;
          ">Cancelar</button>
          <button id="confirmBtn" style="
            flex: 1;
            padding: 0.8rem;
            border: none;
            border-radius: 8px;
            background: #f44336;
            color: white;
            font-weight: 600;
            cursor: pointer;
          ">Eliminar</button>
        </div>
      </div>
    `;

    document.body.appendChild(confirmDiv);

    const cancelBtn = confirmDiv.querySelector('#cancelBtn');
    const confirmBtn = confirmDiv.querySelector('#confirmBtn');

    cancelBtn?.addEventListener('click', () => {
      document.body.removeChild(confirmDiv);
    });

    confirmBtn?.addEventListener('click', () => {
      document.body.removeChild(confirmDiv);
      
      this.projectService.deleteProject(project.id).subscribe({
        next: () => {
          this.showSuccessMessage('eliminado');
          this.loadProjects(false, true);
        },
        error: (err) => {
          console.error('Error eliminando proyecto:', err);
          const backendMessage = this.extractBackendErrorMessage(err);
          const message = backendMessage && backendMessage !== 'Verifica los datos.'
            ? backendMessage
            : 'No se pudo eliminar el proyecto';
          this.showErrorMessage(message);
        }
      });
    });
  }

  showErrorMessage(message: string): void {
    const errorDiv = document.createElement('div');
    errorDiv.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      background: linear-gradient(135deg, #f44336 0%, #da190b 100%);
      color: white;
      padding: 1.5rem 2rem;
      border-radius: 12px;
      box-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
      z-index: 10000;
      font-size: 1.1rem;
      font-weight: 600;
      animation: slideInRight 0.4s ease-out;
    `;
    errorDiv.innerHTML = `
      <div style="display: flex; align-items: center; gap: 10px;">
        <span style="font-size: 1.5rem;">⚠️</span>
        <span>${message}</span>
      </div>
    `;
    document.body.appendChild(errorDiv);

    setTimeout(() => {
      errorDiv.style.animation = 'slideInRight 0.4s ease-out reverse';
      setTimeout(() => {
        document.body.removeChild(errorDiv);
      }, 400);
    }, 3000);
  }

  goToProfile(): void {
    this.showProfileMenu = false;
    setTimeout(() => {
      this.router.navigate(['/profile']);
    }, 100);
  }

  goToDashboard(): void {
    this.showProfileMenu = false;
    setTimeout(() => {
      this.router.navigate(['/dashboard']);
    }, 100);
  }

  toggleProfileMenu(): void {
    this.showProfileMenu = !this.showProfileMenu;
  }

  logout(): void {
    this.showProfileMenu = false;
    this.authService.logout();
  }

  viewEtapas(project: Project): void {
    this.router.navigate(['/etapas', project.id]);
  }
}
