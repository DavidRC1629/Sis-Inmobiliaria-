import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TerrenoPropioService } from '../../services/terreno-propio.service';

@Component({
  selector: 'app-adquisicion-terreno-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './adquisicion-terreno-modal.component.html',
  styleUrls: ['./adquisicion-terreno-modal.component.css']
})
export class AdquisicionTerrenoModalComponent {
  @Input() terrenoId!: number;
  @Output() close = new EventEmitter<void>();
  form: FormGroup;
  loading = false;
  errorMsg = '';

  constructor(private fb: FormBuilder, private terrenoService: TerrenoPropioService) {
    this.form = this.fb.group({
      fecha: ['', Validators.required],
      monto: ['', [Validators.required, Validators.min(1)]],
      descripcion: ['']
    });
  }

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    this.terrenoService.adquirirTerreno(this.terrenoId, this.form.value).subscribe({
      next: () => {
        this.loading = false;
        this.close.emit();
      },
      error: err => {
        this.errorMsg = 'No se pudo registrar la adquisición.';
        this.loading = false;
      }
    });
  }
}
