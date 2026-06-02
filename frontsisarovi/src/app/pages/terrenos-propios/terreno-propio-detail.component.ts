import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CurrencyPipe, NgIf, CommonModule } from '@angular/common';
import { TerrenoPropio } from '../../models/terreno-propio.model';
import { TerrenoPropioService } from '../../services/terreno-propio.service';

@Component({
  selector: 'app-terreno-propio-detail',
  standalone: true,
  imports: [CommonModule, NgIf, CurrencyPipe],
  templateUrl: './terreno-propio-detail.component.html',
  styleUrls: ['./terreno-propio-detail.component.css']
})
export class TerrenoPropioDetailComponent implements OnInit {
  @Input() terrenoId!: number;
  @Output() close = new EventEmitter<void>();
  terreno?: TerrenoPropio;
  loading = false;
  errorMsg = '';

  constructor(private terrenoService: TerrenoPropioService) {}

  ngOnInit() {
    this.fetch();
  }

  fetch() {
    this.loading = true;
    this.terrenoService.getById(this.terrenoId).subscribe({
      next: t => {
        this.terreno = t;
        this.loading = false;
      },
      error: err => {
        this.errorMsg = 'No se pudo cargar el terreno.';
        this.loading = false;
      }
    });
  }

  closeDetail() {
    this.close.emit();
  }
}
