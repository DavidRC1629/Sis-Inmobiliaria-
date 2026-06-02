import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TerrenoPropioService } from '../../services/terreno-propio.service';

@Component({
  selector: 'app-terreno-propio-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './terreno-propio-form.component.html',
  styleUrls: ['./terreno-propio-form.component.css']
})
export class TerrenoPropioFormComponent {
  @Input() visible = false;
  @Output() close = new EventEmitter<void>();
  @Output() created = new EventEmitter<void>();

  errorMsg = '';
  checkingPartida = false;
  partidaExists = false;

  form: any;

  constructor(private fb: FormBuilder, private terrenoService: TerrenoPropioService) {
    this.form = this.fb.group({
      numeroLote: [null, Validators.required],
      calle: [''],
      areaM2: [0],
      perimetro: [0],
      medidaFrente: [0],
      medidaFondo: [0],
      medidaIzquierda: [0],
      medidaDerecha: [0],
      numeroPartida: ['', [Validators.required, Validators.maxLength(8), Validators.pattern('^[0-9]+$')]],
      precio: [null, Validators.required],
      propietarioId: [null, Validators.required],
      imagenUrl: ['']
    });
  }

  onNumeroPartidaInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const sanitized = (input.value || '').replace(/\D/g, '').slice(0, 8);
    if (input.value !== sanitized) {
      input.value = sanitized;
    }
    this.form.get('numeroPartida')?.setValue(sanitized, { emitEvent: false });
    this.partidaExists = false;
  }

  checkNumeroPartida() {
    const partida = (this.form.value.numeroPartida || '').trim();
    this.form.get('numeroPartida')?.setValue(partida, { emitEvent: false });
    if (!partida || this.form.get('numeroPartida')?.invalid) {
      this.partidaExists = false;
      return;
    }
    this.checkingPartida = true;
    this.terrenoService.existsByNumeroPartida(partida).subscribe(exists => {
      this.partidaExists = exists;
      this.checkingPartida = false;
    });
  }

  submit() {
    if (this.form.invalid || this.partidaExists) return;
    this.terrenoService.create(this.form.value).subscribe({
      next: () => {
        this.created.emit();
        this.close.emit();
      },
      error: err => {
        this.errorMsg = err?.error?.message || 'Error al crear terreno';
      }
    });
  }
}
