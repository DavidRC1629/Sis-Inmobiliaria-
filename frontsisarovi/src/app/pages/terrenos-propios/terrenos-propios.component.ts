
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TerrenoPropioService } from '../../services/terreno-propio.service';
import { TerrenoPropio } from '../../models/terreno-propio.model';
import { TerrenoPropioFormComponent } from './terreno-propio-form.component';
import { TerrenoPropioDetailComponent } from './terreno-propio-detail.component';
import { AdquisicionTerrenoModalComponent } from './adquisicion-terreno-modal.component';
@Component({
  selector: 'app-terrenos-propios',
  standalone: true,
  imports: [
    CommonModule,
    TerrenoPropioFormComponent,
    TerrenoPropioDetailComponent,
    AdquisicionTerrenoModalComponent
  ],
  templateUrl: './terrenos-propios.component.html',
  styleUrls: ['./terrenos-propios.component.css']
})
export class TerrenosPropiosComponent implements OnInit {
  terrenos: TerrenoPropio[] = [];
  loading = true;

  showCreateModal = false;
  showDetailModal = false;
  showAdquisicionModal = false;
  selectedTerrenoId: number | null = null;

  constructor(private terrenoService: TerrenoPropioService) {}

  ngOnInit(): void {
    this.loadTerrenos();
  }

  loadTerrenos() {
    this.loading = true;
    this.terrenoService.getAll().subscribe({
      next: (data) => {
        this.terrenos = data;
        this.loading = false;
      },
      error: () => {
        this.terrenos = [];
        this.loading = false;
      }
    });
  }

  openCreateModal() {
    this.showCreateModal = true;
  }
  closeCreateModal() {
    this.showCreateModal = false;
  }
  onCreated() {
    this.loadTerrenos();
  }

  openDetailModal(id: number | undefined) {
    if (typeof id === 'number') {
      this.selectedTerrenoId = id;
      this.showDetailModal = true;
    }
  }
  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedTerrenoId = null;
  }

  openAdquisicionModal(id: number | undefined) {
    if (typeof id === 'number') {
      this.selectedTerrenoId = id;
      this.showAdquisicionModal = true;
    }
  }
  closeAdquisicionModal() {
    this.showAdquisicionModal = false;
    this.selectedTerrenoId = null;
  }
}
