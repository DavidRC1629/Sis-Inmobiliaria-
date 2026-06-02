import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EtapaService } from '../../services/etapa.service';
import { Etapa, EtapaRequest } from '../../models/etapa.model';

@Component({
  selector: 'app-etapas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './etapas.component.html',
  styleUrls: ['./etapas.component.css'],
  changeDetection: ChangeDetectionStrategy.Default
})
export class EtapasComponent implements OnInit {
  projectId!: number;
  etapas: Etapa[] = [];
  loading: boolean = false;
  showModal: boolean = false;
  modalMode: 'create' | 'edit' = 'create';
  modalTitle: string = '';
  currentEtapa: Etapa | null = null;

  formData: EtapaRequest = {
    numeroEtapa: 1
  };

  formErrors: any = {
    numeroEtapa: '',
    general: ''
  };

  constructor(
    private etapaService: EtapaService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    console.log('🚀 EtapasComponent ngOnInit iniciado');
    
    // Obtener projectId inmediatamente
    const projectIdParam = this.route.snapshot.params['projectId'];
    this.projectId = +projectIdParam;
    console.log('🔵 Project ID detectado:', this.projectId);
    
    if (this.projectId && !isNaN(this.projectId)) {
      console.log('✅ ProjectId válido, cargando etapas...');
      this.loadEtapas();
    } else {
      console.error('❌ ProjectId inválido:', projectIdParam);
    }
    
    // También suscribirse a cambios futuros de la ruta
    this.route.params.subscribe(params => {
      const newProjectId = +params['projectId'];
      if (newProjectId !== this.projectId) {
        this.projectId = newProjectId;
        console.log('🔄 ProjectId cambió a:', this.projectId);
        this.loadEtapas();
      }
    });
  }

  loadEtapas(): void {
    console.log('📡 loadEtapas() iniciado para proyecto:', this.projectId);
    
    this.loading = true;
    this.etapas = [];
    this.cdr.markForCheck();
    
    console.log('🌐 Haciendo petición HTTP GET a:', `http://localhost:8080/api/projects/${this.projectId}/etapas`);
    
    this.etapaService.getEtapasByProject(this.projectId).subscribe({
      next: (data: Etapa[]) => {
        console.log('✅ Respuesta exitosa del backend');
        console.log('📊 Cantidad de etapas recibidas:', data.length);
        console.log('📦 Datos completos:', data);
        
        setTimeout(() => {
          this.etapas = data;
          this.loading = false;
          console.log('✅ Estado actualizado - loading:', this.loading, 'etapas.length:', this.etapas.length);
          this.cdr.markForCheck();
          this.cdr.detectChanges();
        }, 0);
      },
      error: (error: any) => {
        console.error('❌ Error al cargar etapas:', error);
        this.showErrorMessage('Error al cargar las etapas');
        this.loading = false;
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  openCreateModal(): void {
    this.modalMode = 'create';
    this.modalTitle = '➕ Nueva Etapa';
    this.resetForm();
    this.showModal = true;
  }

  openEditModal(etapa: Etapa): void {
    this.modalMode = 'edit';
    this.modalTitle = '✏️ Editar Etapa';
    this.currentEtapa = etapa;
    this.formData = {
      numeroEtapa: etapa.numeroEtapa
    };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.resetForm();
    this.currentEtapa = null;
  }

  resetForm(): void {
    this.formData = {
      numeroEtapa: 1
    };
    this.formErrors = {
      numeroEtapa: '',
      general: ''
    };
  }

  validateForm(): boolean {
    let valid = true;
    this.formErrors = {
      numeroEtapa: '',
      general: ''
    };

    if (!this.formData.numeroEtapa || this.formData.numeroEtapa < 1) {
      this.formErrors.numeroEtapa = 'El número de etapa es requerido y debe ser mayor a 0';
      valid = false;
    }

    return valid;
  }

  onSubmit(): void {
    if (!this.validateForm()) {
      return;
    }

    this.loading = true;

    if (this.modalMode === 'create') {
      this.etapaService.createEtapa(this.projectId, this.formData).subscribe({
        next: (response: Etapa) => {
          this.showSuccessMessage('Etapa creada exitosamente');
          this.loadEtapas();
          this.closeModal();
        },
        error: (error: any) => {
          console.error('Error al crear etapa:', error);
          // Capturar el mensaje del backend
          let errorMessage = 'El número de Etapa ya existe';
          if (error.error && error.error.message) {
            errorMessage = error.error.message;
          } else if (error.message) {
            errorMessage = error.message;
          }
          
          this.formErrors.general = errorMessage;
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    } else {
      if (this.currentEtapa) {
        this.etapaService.updateEtapa(this.projectId, this.currentEtapa.id, this.formData).subscribe({
          next: (response: Etapa) => {
            this.showSuccessMessage('Etapa actualizada exitosamente');
            this.loadEtapas();
            this.closeModal();
          },
          error: (error: any) => {
            console.error('Error al actualizar etapa:', error);
            // Capturar el mensaje del backend
            let errorMessage = 'El número de Etapa ya existe';
            if (error.error && error.error.message) {
              errorMessage = error.error.message;
            } else if (error.message) {
              errorMessage = error.message;
            }
            
            this.formErrors.general = errorMessage;
            this.loading = false;
            this.cdr.detectChanges();
          }
        });
      }
    }
  }

  deleteEtapa(etapa: Etapa): void {
    if (confirm(`¿Está seguro de eliminar la Etapa ${etapa.numeroEtapa}?\n\nEsta acción eliminará también todas las parcelas y lotes asociados.`)) {
      this.loading = true;
      this.etapaService.deleteEtapa(this.projectId, etapa.id).subscribe({
        next: () => {
          this.showSuccessMessage('Etapa eliminada exitosamente');
          this.loadEtapas();
        },
        error: (error: any) => {
          console.error('Error al eliminar etapa:', error);
          this.showErrorMessage('Error al eliminar la etapa');
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  viewParcelas(etapa: Etapa): void {
    this.router.navigate(['/parcelas', etapa.id]);
  }

  goBack(): void {
    this.router.navigate(['/gestion-proyectos']);
  }

  trackByEtapaId(index: number, etapa: Etapa): number {
    return etapa.id;
  }

  showSuccessMessage(message: string): void {
    const successDiv = document.createElement('div');
    successDiv.style.cssText = `
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
    successDiv.innerHTML = `
      <div style="display: flex; align-items: center; gap: 10px;">
        <span style="font-size: 1.5rem;">✅</span>
        <span>${message}</span>
      </div>
    `;
    document.body.appendChild(successDiv);

    setTimeout(() => {
      successDiv.style.animation = 'slideInRight 0.4s ease-out reverse';
      setTimeout(() => {
        document.body.removeChild(successDiv);
      }, 400);
    }, 3000);
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
}
