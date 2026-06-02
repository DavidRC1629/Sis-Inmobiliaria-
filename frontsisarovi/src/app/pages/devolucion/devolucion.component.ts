import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AppRefreshService } from '../../services/app-refresh.service';
import { AuthService } from '../../services/auth.service';
import { Devolucion, DevolucionPagoCreateRequest } from '../../models/devolucion.model';
import { DevolucionService } from '../../services/devolucion.service';

@Component({
  selector: 'app-devolucion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './devolucion.component.html',
  styleUrls: ['./devolucion.component.css']
})
export class DevolucionComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly devolucionService = inject(DevolucionService);
  private readonly refreshService = inject(AppRefreshService);

  devoluciones: Devolucion[] = [];
  selectedEstado: 'TODAS' | 'EN_CURSO' | 'COMPLETADA' = 'EN_CURSO';
  selectedDevolucion: Devolucion | null = null;
  loading = false;
  loadingPago = false;
  notification: { type: 'success' | 'error'; message: string } | null = null;

  showPagoModal = false;
  pagoForm: DevolucionPagoCreateRequest = this.buildDefaultPagoForm();

  ngOnInit(): void {
    this.cargarDevoluciones();
    this.refreshService.refresh$.pipe(takeUntil(this.destroy$)).subscribe(() => this.cargarDevoluciones(false));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  logout(): void {
    this.authService.logout();
  }

  cambiarEstado(estado: 'TODAS' | 'EN_CURSO' | 'COMPLETADA'): void {
    this.selectedEstado = estado;
    this.cargarDevoluciones();
  }

  seleccionar(devolucion: Devolucion): void {
    this.selectedDevolucion = devolucion;
  }

  abrirPago(devolucion: Devolucion): void {
    this.selectedDevolucion = devolucion;
    this.pagoForm = this.buildDefaultPagoForm();
    this.showPagoModal = true;
  }

  cerrarPago(): void {
    this.showPagoModal = false;
  }

  guardarPago(): void {
    if (!this.selectedDevolucion) {
      return;
    }

    if (!this.pagoForm.monto || this.pagoForm.monto <= 0) {
      this.showNotification('error', 'Ingresa un monto válido para el pago de devolución.');
      return;
    }

    this.loadingPago = true;
    this.devolucionService.registrarPago(this.selectedDevolucion.id, this.pagoForm).subscribe({
      next: (actualizada) => {
        this.loadingPago = false;
        this.showPagoModal = false;
        this.actualizarLista(actualizada);
        this.selectedDevolucion = actualizada;
        this.showNotification('success', `Pago de devolución registrado para el lote ${actualizada.loteNumero}.`);
      },
      error: (err) => {
        this.loadingPago = false;
        this.showNotification('error', err?.error?.message || 'No se pudo registrar el pago de devolución.');
      }
    });
  }

  formatMoney(value: number | null | undefined): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(Number(value || 0));
  }

  progressWidth(devolucion: Devolucion): string {
    return `${Math.max(0, Math.min(100, Number(devolucion.progreso || 0)))}%`;
  }

  medioPagoOptions = ['EFECTIVO', 'YAPE', 'PLIN', 'TRANSFERENCIA', 'DEPOSITO'];

  private cargarDevoluciones(reselect = true): void {
    this.loading = true;
    this.devolucionService.listar(this.selectedEstado).subscribe({
      next: (devoluciones) => {
        this.loading = false;
        this.devoluciones = Array.isArray(devoluciones) ? devoluciones : [];
        if (reselect) {
          this.selectedDevolucion = this.devoluciones[0] || null;
        }
      },
      error: () => {
        this.loading = false;
        this.devoluciones = [];
        this.showNotification('error', 'No se pudieron cargar las devoluciones.');
      }
    });
  }

  private actualizarLista(actualizada: Devolucion): void {
    const index = this.devoluciones.findIndex((item) => item.id === actualizada.id);
    if (index >= 0) {
      const next = [...this.devoluciones];
      next[index] = actualizada;
      this.devoluciones = next;
    } else {
      this.devoluciones = [actualizada, ...this.devoluciones];
    }
  }

  private buildDefaultPagoForm(): DevolucionPagoCreateRequest {
    const today = new Date().toISOString().slice(0, 10);
    return {
      monto: 0,
      fechaPago: today,
      descripcion: '',
      medioPago: 'EFECTIVO'
    };
  }

  private showNotification(type: 'success' | 'error', message: string): void {
    this.notification = { type, message };
    setTimeout(() => {
      this.notification = null;
    }, 3500);
  }
}
