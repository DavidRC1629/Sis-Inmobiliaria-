import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ParcelaService } from '../../services/parcela.service';
import { Parcela, ParcelaRequest } from '../../models/parcela.model';

@Component({
  selector: 'app-parcelas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './parcelas.component.html',
  styleUrls: ['./parcelas.component.css'],
  changeDetection: ChangeDetectionStrategy.Default
})
export class ParcelasComponent implements OnInit {
  etapaId!: number;
  parcelas: Parcela[] = [];
  loading: boolean = false;
  showModal: boolean = false;
  modalMode: 'create' | 'edit' = 'create';
  modalTitle: string = '';
  currentParcela: Parcela | null = null;

  formData: ParcelaRequest = {
    nombre: '',
    numManzanas: 1,
    propietario: ''
  };

  formErrors: any = {
    nombre: '',
    numManzanas: '',
    propietario: '',
    general: ''
  };

  readonly MIN_MANZANAS = 1;
  readonly MAX_MANZANAS = 27;

  constructor(
    private parcelaService: ParcelaService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const etapaIdParam = this.route.snapshot.params['etapaId'];
    this.etapaId = +etapaIdParam;
    
    if (this.etapaId && !isNaN(this.etapaId)) {
      this.loadParcelas();
    }
    
    this.route.params.subscribe(params => {
      const newEtapaId = +params['etapaId'];
      if (newEtapaId !== this.etapaId) {
        this.etapaId = newEtapaId;
        this.loadParcelas();
      }
    });
  }

  loadParcelas(): void {
    this.loading = true;
    this.parcelas = [];
    this.cdr.markForCheck();
    
    this.parcelaService.getParcelasByEtapa(this.etapaId).subscribe({
      next: (data: Parcela[]) => {
        setTimeout(() => {
          this.parcelas = data;
          this.loading = false;
          this.cdr.markForCheck();
          this.cdr.detectChanges();
        }, 0);
      },
      error: (error: any) => {
        console.error('Error al cargar parcelas:', error);
        this.showErrorMessage('Error al cargar las parcelas');
        this.loading = false;
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  trackByParcelaId(index: number, parcela: Parcela): number {
    return parcela.id;
  }

  openCreateModal(): void {
    this.modalMode = 'create';
    this.modalTitle = '➕ Nueva Parcela';
    this.resetForm();
    this.showModal = true;
  }

  openEditModal(parcela: Parcela): void {
    this.modalMode = 'edit';
    this.modalTitle = '✏️ Editar Parcela';
    this.currentParcela = parcela;
    this.formData = {
      nombre: parcela.nombre,
      numManzanas: parcela.numManzanas,
      propietario: parcela.propietario
    };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.resetForm();
    this.currentParcela = null;
  }

  resetForm(): void {
    this.formData = {
      nombre: '',
      numManzanas: 1,
      propietario: ''
    };
    this.formErrors = {
      nombre: '',
      numManzanas: '',
      propietario: '',
      general: ''
    };
  }

  validateForm(): boolean {
    let valid = true;
    this.formErrors = {
      nombre: '',
      numManzanas: '',
      propietario: '',
      general: ''
    };

    if (!this.formData.nombre || this.formData.nombre.trim() === '') {
      this.formErrors.nombre = 'El nombre es requerido';
      valid = false;
    }

    if (!this.formData.numManzanas || this.formData.numManzanas < this.MIN_MANZANAS || this.formData.numManzanas > this.MAX_MANZANAS) {
      this.formErrors.numManzanas = `El número de manzanas debe estar entre ${this.MIN_MANZANAS} y ${this.MAX_MANZANAS}`;
      valid = false;
    }

    if (!this.formData.propietario || this.formData.propietario.trim() === '') {
      this.formErrors.propietario = 'El propietario es requerido';
      valid = false;
    }

    return valid;
  }

  onManzanasInput(event: any): void {
    const value = parseInt(event.target.value);
    if (value > this.MAX_MANZANAS) {
      this.formData.numManzanas = this.MAX_MANZANAS;
      event.target.value = this.MAX_MANZANAS;
    } else if (value < this.MIN_MANZANAS && event.target.value !== '') {
      this.formData.numManzanas = this.MIN_MANZANAS;
      event.target.value = this.MIN_MANZANAS;
    }
  }

  getManzanasText(): string {
    const manzanas = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'Ñ', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'];
    if (this.formData.numManzanas <= 0) return '';
    if (this.formData.numManzanas > 27) return 'Máximo 27 manzanas';
    return manzanas.slice(0, this.formData.numManzanas).join(', ');
  }

  onSubmit(): void {
    if (!this.validateForm()) {
      return;
    }

    this.loading = true;

    if (this.modalMode === 'create') {
      this.parcelaService.createParcela(this.etapaId, this.formData).subscribe({
        next: (response: Parcela) => {
          this.showSuccessMessage('Parcela creada exitosamente');
          this.loadParcelas();
          this.closeModal();
        },
        error: (error: any) => {
          console.error('Error al crear parcela:', error);
          this.formErrors.general = error.error?.message || 'Error al crear la parcela';
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    } else {
      if (this.currentParcela) {
        this.parcelaService.updateParcela(this.etapaId, this.currentParcela.id, this.formData).subscribe({
          next: (response: Parcela) => {
            this.showSuccessMessage('Parcela actualizada exitosamente');
            this.loadParcelas();
            this.closeModal();
          },
          error: (error: any) => {
            console.error('Error al actualizar parcela:', error);
            this.formErrors.general = error.error?.message || 'Error al actualizar la parcela';
            this.loading = false;
            this.cdr.detectChanges();
          }
        });
      }
    }
  }

  deleteParcela(parcela: Parcela): void {
    if (confirm(`¿Está seguro de eliminar la Parcela "${parcela.nombre}"?\n\nEsta acción eliminará también todos los lotes asociados.`)) {
      this.loading = true;
      this.parcelaService.deleteParcela(this.etapaId, parcela.id).subscribe({
        next: () => {
          this.showSuccessMessage('Parcela eliminada exitosamente');
          this.loadParcelas();
        },
        error: (error: any) => {
          console.error('Error al eliminar parcela:', error);
          this.showErrorMessage('Error al eliminar la parcela');
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  viewLotes(parcela: Parcela): void {
    this.router.navigate(['/lotes', parcela.id]);
  }

  goBack(): void {
    this.router.navigate(['/etapas', this.parcelas[0]?.etapaId || 1]);
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
