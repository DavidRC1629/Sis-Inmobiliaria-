import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { concatMap } from 'rxjs/operators';
import { finalize } from 'rxjs/operators';
import { CronogramaContrato, CronogramaCuota, CronogramaEstado, CronogramaFiltro, CronogramaPagoCuota } from '../../models/cronograma.model';
import { CronogramaService } from '../../services/cronograma.service';

@Component({
  selector: 'app-cronogramas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cronogramas.component.html',
  styleUrls: ['./cronogramas.component.css']
})
export class CronogramasComponent implements OnInit {

  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cronogramaService = inject(CronogramaService);
  private cdr = inject(ChangeDetectorRef);

  loading = false;
  contratos: CronogramaContrato[] = [];
  selectedContrato: CronogramaContrato | null = null;
  selectedSection: 'contratos' | 'inicial' | null = null;
  activeDetailTab: 'separacion' | 'cronograma' | 'pagos' = 'cronograma';
  notification: { type: 'success' | 'error'; message: string } | null = null;

  filtroEstadoUI: 'todos' | 'pagado' | 'moroso' | 'en_curso' = 'todos';
  filtroInicialUI: 'todos' | 'moroso' | 'en_curso' = 'todos';
  asesorEditing = false;
  asesorEditValue = '';
  pagandoEnCurso = false;
  confirmacionPago = {
    open: false,
    requerido: 0,
    recibido: 0,
    aplicable: 0,
    fechaPago: '',
    mediosResumen: ''
  };

  filtros: CronogramaFiltro = {
    dni: '',
    nombres: '',
    estado: undefined
  };

  pagoSeparacion = {
    monto: 0,
    fechaPago: ''
  };

  pagoCuota: Record<number, { monto: number; fechaPago: string }> = {};

  amortizarModal: {
    open: boolean;
    modo: 'amortizar' | 'pago-cuota';
    tipo: 'cuota' | 'separacion' | '';
    cuotaTarget: CronogramaCuota | null;
    fechaPago: string;
    montoAmortizar: number;
    montoFijo: boolean;
    descripcion: string;
    medios: Array<{ id: number; medio: string; monto: number; efectivoEntregado: number }>;
    _nextId: number;
  } = {
    open: false,
    modo: 'amortizar',
    tipo: '',
    cuotaTarget: null,
    fechaPago: '',
    montoAmortizar: 0,
    montoFijo: false,
    descripcion: '',
    medios: [],
    _nextId: 0
  };

  readonly maxPreviewCuotas = 60;

  // Paginación para contratos y en proceso de inicial
  contratosPage = 1;
  inicialPage = 1;
  readonly pageSize = 20;

  get contratosConCronogramaPaginados(): CronogramaContrato[] {
    const start = (this.contratosPage - 1) * this.pageSize;
    return this.contratosConCronograma.slice(start, start + this.pageSize);
  }

  get contratosConCronogramaTotalPages(): number {
    return Math.ceil(this.contratosConCronograma.length / this.pageSize) || 1;
  }

  get contratosEnInicialFiltradosPaginados(): CronogramaContrato[] {
    const start = (this.inicialPage - 1) * this.pageSize;
    return this.contratosEnInicialFiltrados.slice(start, start + this.pageSize);
  }

  get contratosEnInicialTotalPages(): number {
    return Math.ceil(this.contratosEnInicialFiltrados.length / this.pageSize) || 1;
  }

  setContratosPage(page: number) {
    this.contratosPage = page;
  }
  setInicialPage(page: number) {
    this.inicialPage = page;
  }

  ngOnInit(): void {
    const qp = this.route.snapshot.queryParamMap;
    this.filtros.dni = qp.get('dni') || '';
    this.filtros.nombres = qp.get('nombres') || '';
    const projectIdRaw = qp.get('projectId');
    const etapaNumeroRaw = qp.get('etapaNumero');
    const parcelaNombre = (qp.get('parcelaNombre') || '').trim();
    const manzana = (qp.get('manzana') || '').trim();
    const loteNumeroRaw = qp.get('loteNumero');

    if (projectIdRaw) {
      this.filtros.projectId = Number(projectIdRaw);
    }
    if (etapaNumeroRaw) {
      this.filtros.etapaNumero = Number(etapaNumeroRaw);
    }
    if (parcelaNombre) {
      this.filtros.parcelaNombre = parcelaNombre;
    }
    if (manzana) {
      this.filtros.manzana = manzana;
    }
    if (loteNumeroRaw) {
      this.filtros.loteId = Number(loteNumeroRaw);
    }

    const cached = this.cronogramaService.getCachedListSnapshot(this.filtros);
    if (cached.length > 0) {
      this.contratos = [...cached];
      if (cached.length === 1) {
        this.seleccionarContrato(cached[0]);
      }
    }

    this.buscar();
  }

  goBack(): void {
    this.router.navigate(['/clientes']);
  }

  buscar(): void {
    const cached = this.cronogramaService.getCachedListSnapshot(this.filtros);
    const hasVisibleData = cached.length > 0 || this.contratos.length > 0;

    if (cached.length > 0) {
      this.contratos = [...cached];
      if (this.selectedContrato) {
        this.selectedContrato = this.contratos.find(c => c.id === this.selectedContrato?.id) || this.selectedContrato;
      } else if (this.contratos.length === 1) {
        this.seleccionarContrato(this.contratos[0]);
      }
    }

    this.loading = !hasVisibleData;
    this.cronogramaService
      .listar(this.filtros)
      .pipe(finalize(() => {
        if (!hasVisibleData) {
          this.loading = false;
        }
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (data) => {
          this.contratos = Array.isArray(data) ? data : [];
          if (this.selectedContrato) {
            this.selectedContrato = this.contratos.find(c => c.id === this.selectedContrato?.id) || null;
          } else if (this.contratos.length === 1) {
            this.seleccionarContrato(this.contratos[0]);
          }
        },
        error: (error) => {
          if (!hasVisibleData) {
            this.contratos = [];
            this.selectedContrato = null;
            this.showNotification('error', error?.error?.message || 'No se pudieron cargar los cronogramas.');
          }
        }
      });
  }

  limpiar(): void {
    this.filtros = { dni: '', nombres: '', estado: undefined };
    this.buscar();
  }

  cerrarDetalle(): void {
    this.selectedContrato = null;
    this.selectedSection = null;
    this.activeDetailTab = 'cronograma';
  }

  get contratosConCronograma(): CronogramaContrato[] {
    const base = this.contratos.filter((contrato) => this.separacionPendiente(contrato) <= 0);
    switch (this.filtroEstadoUI) {
      case 'pagado': return base.filter(c => this.isPagado(c));
      case 'moroso': return base.filter(c => this.isMoroso(c));
      case 'en_curso': return base.filter(c => this.isEnCurso(c));
      default: return base;
    }
  }

  get contratosEnInicial(): CronogramaContrato[] {
    return this.contratos.filter((contrato) => this.separacionPendiente(contrato) > 0);
  }

  get contratosEnInicialFiltrados(): CronogramaContrato[] {
    const base = this.contratosEnInicial;
    switch (this.filtroInicialUI) {
      case 'moroso': return base.filter(c => this.isInicialMoroso(c));
      case 'en_curso': return base.filter(c => this.isSeparacionEnCurso(c));
      default: return base;
    }
  }

  seleccionarContrato(contrato: CronogramaContrato, section?: 'contratos' | 'inicial'): void {
    const resolvedSection = section || (this.separacionPendiente(contrato) > 0 ? 'inicial' : 'contratos');
    if (this.selectedContrato?.id === contrato.id && this.selectedSection === resolvedSection) {
      this.cerrarDetalle();
      return;
    }

    this.selectedContrato = contrato;
    this.selectedSection = resolvedSection;
    this.activeDetailTab = this.defaultDetailTab(contrato);
    this.pagoSeparacion = { monto: 0, fechaPago: this.todayISO() };
    this.pagoCuota = {};
    (contrato.cuotas || []).forEach((cuota) => {
      this.pagoCuota[cuota.id] = { monto: 0, fechaPago: this.todayISO() };
    });
  }

  get contratosPanelActivo(): CronogramaContrato[] {
    return this.selectedSection === 'inicial' ? this.contratosEnInicialFiltrados : this.contratosConCronograma;
  }

  get panelActivoTitulo(): string {
    return this.selectedSection === 'inicial' ? 'En Proceso de Inicial' : 'Contratos';
  }

  get panelActivoDescripcion(): string {
    return this.selectedSection === 'inicial'
      ? 'Clientes que aún completan su separación'
      : 'Clientes con cronograma activo';
  }

  esContratoSeleccionado(contrato: CronogramaContrato | null, section?: 'contratos' | 'inicial'): boolean {
    if (!contrato || !this.selectedContrato) {
      return false;
    }

    if (section) {
      return this.selectedContrato.id === contrato.id && this.selectedSection === section;
    }

    return this.selectedContrato.id === contrato.id;
  }

  registrarPagoSeparacion(): void {
    if (!this.selectedContrato) {
      return;
    }

    const monto = Number(this.pagoSeparacion.monto || 0);
    if (monto <= 0) {
      this.showNotification('error', 'Ingresa un monto válido para pago de separación.');
      return;
    }

    this.cronogramaService.registrarPagoSeparacion(this.selectedContrato.id, {
      monto,
      fechaPago: this.pagoSeparacion.fechaPago || undefined
    }).subscribe({
      next: (updated) => {
        this.actualizarContrato(updated);
        this.showNotification('success', 'Pago de separación registrado.');
      },
      error: (error) => this.showNotification('error', error?.error?.message || 'No se pudo registrar el pago de separación.')
    });
  }

  registrarPagoCuota(cuota: CronogramaCuota): void {
    if (!this.selectedContrato) {
      return;
    }

    const form = this.pagoCuota[cuota.id] || { monto: 0, fechaPago: this.todayISO() };
    const monto = Number(form.monto || 0);
    if (monto <= 0) {
      this.showNotification('error', 'Ingresa un monto válido para la cuota.');
      return;
    }

    this.cronogramaService.registrarPagoCuota(cuota.id, {
      monto,
      fechaPago: form.fechaPago || undefined
    }).subscribe({
      next: (updated) => {
        this.actualizarContrato(updated);
        this.showNotification('success', 'Pago de cuota registrado.');
      },
      error: (error) => this.showNotification('error', error?.error?.message || 'No se pudo registrar el pago de cuota.')
    });
  }

  estadoLabel(estado: CronogramaEstado): string {
    switch (estado) {
      case 'AL_DIA': return 'Al día';
      case 'DEUDOR': return 'Deudor';
      case 'SEPARACION_EN_CURSO': return 'Separación en curso';
      default: return estado;
    }
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(Number(value || 0));
  }

  formatDate(value?: string | null): string {
    if (!value) {
      return '-';
    }

    const date = this.parseLocalDate(value);
    if (!date) {
      return value;
    }

    return new Intl.DateTimeFormat('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }).format(date);
  }

  monthLabel(value?: string | null): string {
    if (!value) {
      return '-';
    }

    const date = this.parseLocalDate(value);
    if (!date) {
      return value;
    }

    return new Intl.DateTimeFormat('es-PE', {
      month: 'short',
      year: 'numeric'
    }).format(date).replace('.', '').toUpperCase();
  }

  contratoEstadoClass(estado: CronogramaEstado): string {
    switch (estado) {
      case 'AL_DIA': return 'ok';
      case 'DEUDOR': return 'deudor';
      case 'SEPARACION_EN_CURSO': return 'sep';
      default: return '';
    }
  }

  displayEstadoLabel(contrato: CronogramaContrato | null): string {
    if (!contrato) return '';
    if (contrato.estado === 'SEPARACION_EN_CURSO' && this.separacionPendiente(contrato) > 0) {
      return 'Separación en Curso';
    }
    if (this.isPagado(contrato)) {
      return 'Pagado';
    }
    if ((contrato.cuotas?.length ?? 0) > 0 && this.separacionPendiente(contrato) <= 0 && contrato.saldoPendienteActual > 0) {
      return 'Pago de cuotas en curso';
    }
    if (contrato.estado === 'AL_DIA' && (contrato.cuotas?.length ?? 0) > 0 && contrato.saldoPendienteActual > 0) {
      return 'Pago de cuotas en curso';
    }
    return this.estadoLabel(contrato.estado);
  }

  displayEstadoClass(contrato: CronogramaContrato | null): string {
    if (!contrato) return '';
    if (contrato.estado === 'SEPARACION_EN_CURSO' && this.separacionPendiente(contrato) > 0) {
      return 'sep';
    }
    if (this.isPagado(contrato)) {
      return 'pagado';
    }
    if ((contrato.cuotas?.length ?? 0) > 0 && this.separacionPendiente(contrato) <= 0 && contrato.saldoPendienteActual > 0) {
      return 'mensual';
    }
    if (contrato.estado === 'AL_DIA' && (contrato.cuotas?.length ?? 0) > 0 && contrato.saldoPendienteActual > 0) {
      return 'mensual';
    }
    return this.contratoEstadoClass(contrato.estado);
  }

  isPagado(contrato: CronogramaContrato | null): boolean {
    if (!contrato) return false;
    return contrato.saldoPendienteActual <= 0 && this.separacionPendiente(contrato) <= 0;
  }

  isMoroso(contrato: CronogramaContrato | null): boolean {
    if (!contrato) return false;
    if (this.separacionPendiente(contrato) > 0) return false;
    if (this.isPagado(contrato)) return false;
    return contrato.estado === 'DEUDOR' || (contrato.cuotas || []).some(c => c.diasRetraso > 0);
  }

  isEnCurso(contrato: CronogramaContrato | null): boolean {
    if (!contrato) return false;
    return (contrato.cuotas?.length ?? 0) > 0
      && this.separacionPendiente(contrato) <= 0
      && contrato.saldoPendienteActual > 0;
  }

  isInicialMoroso(contrato: CronogramaContrato | null): boolean {
    if (!contrato) return false;
    return this.separacionPendiente(contrato) > 0 && this.separacionDiasRetraso(contrato) > 0;
  }

  isSeparacionEnCurso(contrato: CronogramaContrato | null): boolean {
    if (!contrato) return false;
    return this.separacionPendiente(contrato) > 0 && this.separacionDiasRetraso(contrato) <= 0;
  }

  setFiltroEstadoUI(filtro: 'todos' | 'pagado' | 'moroso' | 'en_curso'): void {
    this.filtroEstadoUI = filtro;
    this.cdr.detectChanges();
  }

  setFiltroInicialUI(filtro: 'todos' | 'moroso' | 'en_curso'): void {
    this.filtroInicialUI = filtro;
    this.cdr.detectChanges();
  }

  separacionFechaVencimiento(contrato: CronogramaContrato | null): string | null {
    if (!contrato) return null;
    const fechasPago = (contrato.pagosSeparacion || [])
      .map((pago) => (pago.fechaPago || '').trim())
      .filter((fecha) => !!this.parseLocalDate(fecha));
    const primerPago = fechasPago.sort((a, b) => a.localeCompare(b))[0];
    const base = primerPago || contrato.fechaOperacion;
    if (!base) return null;
    const fechaBase = this.parseLocalDate(base);
    if (!fechaBase) return null;
    return this.formatLocalDateISO(this.addDaysLocal(fechaBase, 40));
  }

  separacionDiasRetraso(contrato: CronogramaContrato | null): number {
    if (!contrato) return 0;
    if (this.separacionPendiente(contrato) <= 0) return 0;
    const venc = this.separacionFechaVencimiento(contrato);
    if (!venc) return 0;
    const hoy = this.parseLocalDate(this.todayISO());
    const vencDate = this.parseLocalDate(venc);
    if (!hoy || !vencDate) return 0;
    return Math.max(0, this.diffCalendarDays(vencDate, hoy));
  }

  isSeparacionMorosa(contrato: CronogramaContrato | null): boolean {
    return this.separacionDiasRetraso(contrato) > 0;
  }

  resumenPlan(contrato: CronogramaContrato | null): string {
    if (!contrato) {
      return '-';
    }

    if (this.separacionPendiente(contrato) > 0) {
      return `Inicial: ${this.formatMoney(contrato.montoSeparacionAcumulado)} de ${this.formatMoney(contrato.montoSeparacionObjetivo)}`;
    }

    const totalCuotas = contrato.cuotas?.length || contrato.plazoMeses || 0;
    if (totalCuotas <= 0) {
      return 'Sin cuotas programadas';
    }

    return `Plan: ${totalCuotas} cuotas | Mensual: ${this.formatMoney(this.montoMensualPlan(contrato))}`;
  }

  montoMensualPlan(contrato: CronogramaContrato | null): number {
    if (!contrato) {
      return 0;
    }

    const cuotas = this.cuotasCredito(contrato);
    if (cuotas.length > 0) {
      return Number(cuotas[0].monto || 0);
    }

    return Number(contrato.montoCuotaReferencial || 0);
  }

  montoCuotaContratoVisual(cuota: CronogramaCuota | null, contrato: CronogramaContrato | null): number {
    if (!cuota) {
      return 0;
    }

    const cuotaBase = this.montoCuotaBase(cuota, contrato);
    const estado = (cuota.estadoPago || '').toUpperCase();

    // En PENDIENTE/PARCIAL mostramos el faltante real de la cuota.
    if (estado !== 'PAGADA') {
      return Math.max(0, cuotaBase - Number(cuota.montoPagado || 0));
    }

    return cuotaBase;
  }

  private montoCuotaBase(cuota: CronogramaCuota | null, contrato: CronogramaContrato | null): number {
    if (!cuota) {
      return 0;
    }

    if (!contrato) {
      return Number(cuota.montoCuota || 0);
    }

    const cuotaVisual = this.cuotasCredito(contrato)
      .find((item) => Number(item.numero) === Number(cuota.numeroCuota));

    return cuotaVisual ? Number(cuotaVisual.monto || 0) : Number(cuota.montoCuota || 0);
  }

  cuotaEstadoClass(cuota: CronogramaCuota): string {
    const estado = (cuota?.estadoPago || '').toUpperCase();
    if (estado === 'PAGADA') {
      return 'ok';
    }
    if (estado === 'PARCIAL') {
      return 'parcial';
    }
    return cuota?.diasRetraso > 0 ? 'deudor' : 'pendiente';
  }

  cuotaEstadoClassFromDetalle(estadoPago: string, diasRetraso: number): string {
    const estado = (estadoPago || '').toUpperCase();
    if (estado === 'PAGADA') {
      return 'ok';
    }
    if (estado === 'PARCIAL') {
      return 'parcial';
    }
    return diasRetraso > 0 ? 'deudor' : 'pendiente';
  }

  cuotasPagadasCount(contrato: CronogramaContrato | null): number {
    if (!contrato?.cuotas?.length) {
      return 0;
    }
    return contrato.cuotas.filter((c) => c.estadoPago === 'PAGADA').length;
  }

  mostrarProgresoCuotas(contrato: CronogramaContrato | null): boolean {
    return !!contrato && this.separacionPendiente(contrato) <= 0 && (contrato.cuotas?.length || 0) > 0;
  }

  montoTotalCuotasPlan(contrato: CronogramaContrato | null): number {
    if (!contrato) {
      return 0;
    }
    return this.cuotasCredito(contrato).reduce((total, cuota) => total + Number(cuota.monto || 0), 0);
  }

  montoPagadoCuotas(contrato: CronogramaContrato | null): number {
    if (!contrato?.cuotas?.length) {
      return 0;
    }
    return contrato.cuotas.reduce((total, cuota) => total + Number(cuota.montoPagado || 0), 0);
  }

  avanceCuotasPct(contrato: CronogramaContrato | null): number {
    const total = this.montoTotalCuotasPlan(contrato);
    if (total <= 0) {
      return 0;
    }
    const pagado = this.montoPagadoCuotas(contrato);
    return Math.max(0, Math.min(100, Math.round((pagado / total) * 100)));
  }

  avanceSeparacionPct(contrato: CronogramaContrato | null): number {
    if (!contrato) {
      return 0;
    }
    const objetivo = Number(contrato.montoSeparacionObjetivo || 0);
    if (objetivo <= 0) {
      return 0;
    }
    const acumulado = Number(contrato.montoSeparacionAcumulado || 0);
    return Math.max(0, Math.min(100, Math.round((acumulado / objetivo) * 100)));
  }

  separacionPendiente(contrato: CronogramaContrato | null): number {
    if (!contrato) {
      return 0;
    }
    return Math.max(0, Number(contrato.montoSeparacionObjetivo || 0) - Number(contrato.montoSeparacionAcumulado || 0));
  }

  saldoVisible(contrato: CronogramaContrato | null): number {
    if (!contrato) {
      return 0;
    }

    const faltanteSeparacion = this.separacionPendiente(contrato);
    
    // Si aún falta pagar separación, mostrar solo esa cantidad
    if (faltanteSeparacion > 0) {
      return faltanteSeparacion;
    }

    // Si separación está completa, mostrar el total del cronograma a pagar
    return this.totalCreditoConInteres(contrato);
  }

  mostrarInfoCredito(contrato: CronogramaContrato | null): boolean {
    if (!contrato) {
      return false;
    }
    // Solo mostrar cronograma si: es a crédito Y separación está 100% pagada
    return contrato.tipoOperacion !== 'CONTADO' && this.separacionPendiente(contrato) <= 0;
  }

  saldoBaseCredito(contrato: CronogramaContrato | null): number {
    if (!contrato) {
      return 0;
    }

    if (this.separacionPendiente(contrato) > 0) {
      const pagadoAlCompletarSeparacion = Math.max(
        Number(contrato.montoPagadoTotal || 0),
        Number(contrato.montoSeparacionObjetivo || 0)
      );
      return Math.max(0, Number(contrato.precioVenta || 0) - pagadoAlCompletarSeparacion);
    }

    return Math.max(0, Number(contrato.precioVenta || 0) - Number(contrato.montoPagadoTotal || 0));
  }

  montoInteresCredito(contrato: CronogramaContrato | null): number {
    if (!contrato) {
      return 0;
    }
    const base = this.saldoBaseCredito(contrato);
    const pct = Math.max(0, Number(contrato.interesPorcentaje || 0));
    return Math.round(base * (pct / 100));
  }

  totalCreditoConInteres(contrato: CronogramaContrato | null): number {
    return this.saldoBaseCredito(contrato) + this.montoInteresCredito(contrato);
  }

  cuotasCredito(contrato: CronogramaContrato | null): Array<{ numero: number; fecha: string; monto: number; estado: string }> {
    if (!contrato) {
      return [];
    }

    if (Array.isArray(contrato.cuotas) && contrato.cuotas.length > 0) {
      const cuotasRaw = contrato.cuotas
        .slice(0, this.maxPreviewCuotas)
        .map((cuota) => ({
          numero: Number(cuota.numeroCuota || 0),
          fecha: cuota.fechaVencimiento,
          monto: Math.round(Number(cuota.montoCuota || 0)),
          estado: (cuota.estadoPago || 'PENDIENTE').toUpperCase(),
          pagado: Math.round(Number(cuota.montoPagado || 0))
        }));

      const todasPendientesSinPago = cuotasRaw.length > 1
        && cuotasRaw.every((c) => c.pagado <= 0 && c.estado === 'PENDIENTE');

      if (!todasPendientesSinPago) {
        return cuotasRaw.map(({ numero, fecha, monto, estado }) => ({ numero, fecha, monto, estado }));
      }

      const total = this.totalCreditoConInteres(contrato);
      if (total <= 0) {
        return cuotasRaw.map(({ numero, fecha, monto, estado }) => ({ numero, fecha, monto, estado }));
      }

      const plazo = cuotasRaw.length;
      let cuotaBase = Math.round((total / plazo) / 10) * 10;
      if (cuotaBase <= 0) {
        cuotaBase = Math.round(total / plazo);
      }

      const maxPermitida = Math.floor(total / (plazo - 1));
      if (cuotaBase > maxPermitida) {
        cuotaBase = Math.max(1, maxPermitida);
      }

      let acumulado = 0;
      return cuotasRaw.map((c, idx) => {
        const monto = idx === plazo - 1 ? Math.max(0, Math.round(total - acumulado)) : cuotaBase;
        acumulado += monto;
        return {
          numero: c.numero,
          fecha: c.fecha,
          monto,
          estado: c.estado
        };
      });
    }

    const plazo = Math.max(1, Math.min(this.maxPreviewCuotas, Number(contrato.plazoMeses || 0) || 1));
    const total = this.totalCreditoConInteres(contrato);
    if (total <= 0) {
      return [];
    }

    let cuotaBase = Math.round((total / plazo) / 10) * 10;
    if (cuotaBase <= 0) {
      cuotaBase = Math.round(total / plazo);
    }

    if (plazo > 1) {
      const maxPermitida = Math.floor(total / (plazo - 1));
      if (cuotaBase > maxPermitida) {
        cuotaBase = Math.max(1, maxPermitida);
      }
    }

    const fechaBase = contrato.fechaInicioCronograma || contrato.fechaOperacion || this.todayISO();
    const cuotas: Array<{ numero: number; fecha: string; monto: number; estado: string }> = [];
    let acumulado = 0;

    for (let i = 1; i <= plazo; i++) {
      const monto = i === plazo ? Math.max(0, Math.round(total - acumulado)) : cuotaBase;
      acumulado += monto;
      cuotas.push({
        numero: i,
        fecha: this.addMonthsISO(fechaBase, i),
        monto,
        estado: 'PROYECTADA'
      });
    }

    return cuotas;
  }

  estadoCuotaResumenClass(estado: string): string {
    const normalized = (estado || '').toUpperCase();
    if (normalized === 'PAGADA') {
      return 'ok';
    }
    if (normalized === 'PARCIAL') {
      return 'parcial';
    }
    if (normalized === 'PROYECTADA') {
      return 'mensual';
    }
    return 'pendiente';
  }

  trackByContrato(index: number, item: CronogramaContrato): number {
    return item.id;
  }

  trackByCuota(index: number, item: CronogramaCuota): number {
    return item.id;
  }

  trackByDetallePago(index: number, item: { cuotaId: number; pagoId?: number | null }): string {
    return `${item.cuotaId}-${item.pagoId ?? 'base'}-${index}`;
  }

  switchDetalleTab(tab: 'separacion' | 'cronograma' | 'pagos'): void {
    this.activeDetailTab = tab;
  }

  mostrarSoloSeparacion(contrato: CronogramaContrato | null): boolean {
    if (!contrato) {
      return false;
    }
    return this.separacionPendiente(contrato) > 0;
  }

  mostrarTabSeparacion(contrato: CronogramaContrato | null): boolean {
    if (!contrato) {
      return false;
    }
    return Number(contrato.montoSeparacionAcumulado || 0) > 0 || this.detallePagosSeparacion(contrato).length > 0;
  }

  mostrarTabCronograma(contrato: CronogramaContrato | null): boolean {
    if (!contrato) {
      return false;
    }
    if (this.mostrarSoloSeparacion(contrato)) {
      return false;
    }
    return (contrato.cuotas?.length || 0) > 0;
  }

  mostrarTabPagosCuotas(contrato: CronogramaContrato | null): boolean {
    if (!contrato) {
      return false;
    }
    if (this.mostrarSoloSeparacion(contrato)) {
      return false;
    }
    return (contrato.cuotas?.length || 0) > 0;
  }

  tieneDetallePagosEfectuados(contrato: CronogramaContrato | null): boolean {
    return this.detallePagosEfectuados(contrato).length > 0;
  }

  detallePagosSeparacion(contrato: CronogramaContrato | null): Array<{
    cuotaId: number;
    pagoId?: number | null;
    numeroCuota: number;
    fechaPago: string | null;
    tipoPago: string;
    monto: number;
    estadoPago: string;
    notas: string;
    diasRetraso: number;
    tienePago: boolean;
  }> {
    if (!contrato) {
      return [];
    }

    if (!contrato.pagosSeparacion?.length) {
      const acumulado = Number(contrato.montoSeparacionAcumulado || 0);
      if (acumulado <= 0) {
        return [];
      }

      return [{
        cuotaId: 0,
        pagoId: null,
        numeroCuota: 0,
        fechaPago: contrato.fechaOperacion || null,
        tipoPago: 'EFECTIVO',
        monto: acumulado,
        estadoPago: this.separacionPendiente(contrato) > 0 ? 'PARCIAL' : 'PAGADA',
        notas: 'Pago de separación histórico',
        diasRetraso: 0,
        tienePago: true
      }];
    }

    const rows = contrato.pagosSeparacion
      .filter((pago) => Number(pago.monto || 0) > 0)
      .sort((a, b) => Number(a.id || 0) - Number(b.id || 0))
      .map((pago) => ({
        cuotaId: 0,
        pagoId: pago.id,
        numeroCuota: 0,
        fechaPago: pago.fechaPago || null,
        tipoPago: ((pago.tipoPago || '').trim().toUpperCase() || 'EFECTIVO'),
        monto: Number(pago.monto || 0),
        estadoPago: (pago.estadoPago || 'PARCIAL').toUpperCase(),
        notas: (pago.notas || '').trim() || 'Sin nota',
        diasRetraso: 0,
        tienePago: true
      }));

    // If initial payment at acquisition isn't covered by the registered records,
    // prepend a synthetic entry so the total matches montoSeparacionAcumulado.
    const sumaRegistros = rows.reduce((s, r) => s + r.monto, 0);
    const acumulado = Number(contrato.montoSeparacionAcumulado || 0);
    const pagoInicial = Math.round(acumulado - sumaRegistros);
    if (pagoInicial > 0) {
      rows.unshift({
        cuotaId: 0,
        pagoId: 0,
        numeroCuota: 0,
        fechaPago: contrato.fechaOperacion || null,
        tipoPago: 'EFECTIVO',
        monto: pagoInicial,
        estadoPago: 'PARCIAL',
        notas: 'Pago al adquirir el lote',
        diasRetraso: 0,
        tienePago: true
      });
    }

    return rows;
  }

  detallePagosEfectuados(contrato: CronogramaContrato | null): Array<{
    cuotaId: number;
    pagoId?: number | null;
    numeroCuota: number;
    fechaPago: string | null;
    tipoPago: string;
    monto: number;
    estadoPago: string;
    notas: string;
    diasRetraso: number;
    tienePago: boolean;
  }> {
    if (!contrato?.cuotas?.length) {
      return [];
    }

    const rows: Array<{
      cuotaId: number;
      pagoId?: number | null;
      numeroCuota: number;
      fechaPago: string | null;
      tipoPago: string;
      monto: number;
      estadoPago: string;
      notas: string;
      diasRetraso: number;
      tienePago: boolean;
    }> = [];

    contrato.cuotas.forEach((cuota) => {
      const pagos = (cuota.pagos || []).slice().sort((a, b) => Number(a.id || 0) - Number(b.id || 0));

      if (pagos.length > 0) {
        pagos.forEach((pago) => {
          const row = this.detallePagoRowDesdePago(cuota, pago);
          if (row.tienePago) {
            rows.push(row);
          }
        });
        return;
      }

      if (Number(cuota.montoPagado || 0) > 0) {
        rows.push({
          cuotaId: cuota.id,
          pagoId: null,
          numeroCuota: cuota.numeroCuota,
          fechaPago: cuota.fechaPago || null,
          tipoPago: 'EFECTIVO',
          monto: Number(cuota.montoPagado || 0),
          estadoPago: cuota.estadoPago,
          notas: cuota.diasRetraso > 0 ? `Retraso ${cuota.diasRetraso} día(s)` : 'Sin nota',
          diasRetraso: cuota.diasRetraso,
          tienePago: true
        });
      }
    });

    return rows;
  }

  private detallePagoRowDesdePago(cuota: CronogramaCuota, pago: CronogramaPagoCuota): {
    cuotaId: number;
    pagoId?: number | null;
    numeroCuota: number;
    fechaPago: string | null;
    tipoPago: string;
    monto: number;
    estadoPago: string;
    notas: string;
    diasRetraso: number;
    tienePago: boolean;
  } {
    const tipo = (pago.tipoPago || '').trim().toUpperCase() || 'EFECTIVO';
    const notas = (pago.notas || '').trim() || 'Sin nota';

    return {
      cuotaId: cuota.id,
      pagoId: pago.id,
      numeroCuota: cuota.numeroCuota,
      fechaPago: pago.fechaPago || null,
      tipoPago: tipo,
      monto: Number(pago.monto || 0),
      estadoPago: (pago.estadoPago || cuota.estadoPago || 'PENDIENTE').toUpperCase(),
      notas,
      diasRetraso: cuota.diasRetraso,
      tienePago: Number(pago.monto || 0) > 0
    };
  }

  private actualizarContrato(updated: CronogramaContrato): void {
    this.contratos = this.contratos.map(c => c.id === updated.id ? updated : c);
    this.selectedContrato = updated;
    if (this.activeDetailTab === 'separacion' && !this.mostrarTabSeparacion(updated)) {
      this.activeDetailTab = 'cronograma';
    }

    if (this.activeDetailTab === 'cronograma' && !this.mostrarTabCronograma(updated)) {
      this.activeDetailTab = this.defaultDetailTab(updated);
    }

    if (this.activeDetailTab === 'pagos' && !this.mostrarTabPagosCuotas(updated)) {
      this.activeDetailTab = this.defaultDetailTab(updated);
    }
    this.buscar();
  }

  private defaultDetailTab(contrato: CronogramaContrato | null): 'separacion' | 'cronograma' | 'pagos' {
    if (this.mostrarSoloSeparacion(contrato)) {
      return 'separacion';
    }

    if (this.mostrarTabSeparacion(contrato)) {
      return 'separacion';
    }

    return 'cronograma';
  }

  private showNotification(type: 'success' | 'error', message: string): void {
    this.notification = { type, message };
    setTimeout(() => {
      this.notification = null;
      this.cdr.detectChanges();
    }, 2000);
  }

  private todayISO(): string {
    const d = new Date();
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  abrirAmortizar(): void {
    if (!this.selectedContrato) return;
    const contrato = this.selectedContrato;
    let tipo: 'cuota' | 'separacion';
    let cuotaTarget: CronogramaCuota | null = null;

    if (this.separacionPendiente(contrato) > 0) {
      tipo = 'separacion';
    } else {
      tipo = 'cuota';
      cuotaTarget = this.primeraCuotaPendiente(contrato);
    }

    this.amortizarModal = {
      open: true,
      modo: 'amortizar',
      tipo,
      cuotaTarget,
      fechaPago: this.todayISO(),
      montoAmortizar: 0,
      montoFijo: false,
      descripcion: '',
      medios: [],
      _nextId: 0
    };
  }

  abrirPagoCuota(): void {
    if (!this.selectedContrato) return;
    const contrato = this.selectedContrato;
    const cuotaTarget = this.primeraCuotaPendiente(contrato);
    if (!cuotaTarget) {
      this.showNotification('error', 'No hay cuotas pendientes por pagar.');
      return;
    }

    this.amortizarModal = {
      open: true,
      modo: 'pago-cuota',
      tipo: 'cuota',
      cuotaTarget,
      fechaPago: this.todayISO(),
      montoAmortizar: this.saldoPendienteCuotaVisual(cuotaTarget, contrato),
      montoFijo: true,
      descripcion: '',
      medios: [],
      _nextId: 0
    };
  }

  cerrarAmortizar(): void {
    this.amortizarModal.open = false;
  }

  addMedio(): void {
    this.amortizarModal.medios.push({
      id: this.amortizarModal._nextId++,
      medio: 'EFECTIVO',
      monto: 0,
      efectivoEntregado: 0
    });
  }

  removeMedio(id: number): void {
    this.amortizarModal.medios = this.amortizarModal.medios.filter(m => m.id !== id);
  }

  totalAmortizar(): number {
    return this.amortizarModal.medios.reduce((sum, m) => sum + Number(m.monto || 0), 0);
  }

  abrirConfirmacionPago(): void {
    if (this.pagandoEnCurso) return;

    const preparado = this.prepararConfirmacionPago();
    if (!preparado) {
      return;
    }

    this.confirmacionPago = {
      open: true,
      requerido: Math.max(0, Number(this.amortizarModal.montoAmortizar || 0)),
      recibido: preparado.totalIngresado,
      aplicable: preparado.montoAplicable,
      fechaPago: this.amortizarModal.fechaPago || this.todayISO(),
      mediosResumen: this.resumenMediosActual()
    };
  }

  cancelarConfirmacionPago(): void {
    this.confirmacionPago.open = false;
  }

  confirmarPagoDesdeModal(): void {
    if (this.pagandoEnCurso) {
      return;
    }
    this.confirmacionPago.open = false;
    this.confirmarAmortizar();
  }

  private resumenMediosActual(): string {
    const mediosValidos = this.amortizarModal.medios
      .filter(m => Number(m.monto || 0) > 0)
      .map((m) => {
        const monto = this.formatMoney(Number(m.monto || 0));
        if (m.medio === 'EFECTIVO' && Number(m.efectivoEntregado || 0) > 0) {
          return `EFECTIVO ${monto} (con ${this.formatMoney(Number(m.efectivoEntregado || 0))})`;
        }
        return `${m.medio} ${monto}`;
      });

    return mediosValidos.length > 0 ? mediosValidos.join(' | ') : '-';
  }

  private prepararConfirmacionPago(): { contrato: CronogramaContrato; totalIngresado: number; montoAplicable: number } | null {
    if (!this.selectedContrato) return null;
    const contrato = this.selectedContrato;
    const totalIngresado = this.totalAmortizar();
    if (totalIngresado <= 0) {
      this.showNotification('error', 'Agrega al menos un medio de pago con monto valido.');
      return null;
    }

    const montoObjetivo = Math.max(0, Number(this.amortizarModal.montoAmortizar || 0));
    const montoAplicable = montoObjetivo > 0 ? Math.min(totalIngresado, montoObjetivo) : totalIngresado;

    if (montoAplicable <= 0) {
      this.showNotification('error', 'Ingresa un monto valido a pagar.');
      return null;
    }

    return { contrato, totalIngresado, montoAplicable };
  }

  vueltoEfectivo(medio: { monto: number; efectivoEntregado: number }): number {
    return Math.max(0, Number(medio.efectivoEntregado || 0) - Number(medio.monto || 0));
  }

  amortizarContextLabel(): string {
    const contrato = this.selectedContrato;
    if (!contrato) return '';
    if (this.amortizarModal.tipo === 'separacion') {
      return this.amortizarModal.modo === 'pago-cuota'
        ? 'Pago de separación'
        : `Amortización de separación — Pendiente: ${this.formatMoney(this.separacionPendiente(contrato))}`;
    }
    const cuota = this.amortizarModal.cuotaTarget;
    if (cuota) {
      if (this.amortizarModal.modo === 'pago-cuota') {
        return `Pago de cuota N° ${cuota.numeroCuota} — ${this.formatDate(cuota.fechaVencimiento)}`;
      }
      return `Amortización desde cuota N° ${cuota.numeroCuota} — ${this.formatDate(cuota.fechaVencimiento)}`;
    }
    return 'Amortización';
  }

  hayAlgoQueAmortizar(contrato: CronogramaContrato | null): boolean {
    if (!contrato) return false;
    if (this.separacionPendiente(contrato) > 0) return true;
    if (this.mostrarInfoCredito(contrato) && (contrato.cuotas?.length ?? 0) > 0) {
      return (contrato.cuotas || []).some(c => c.estadoPago !== 'PAGADA');
    }
    return false;
  }

  confirmarAmortizar(): void {
    if (this.pagandoEnCurso) return;
    const preparado = this.prepararConfirmacionPago();
    if (!preparado) {
      return;
    }

    const contrato = preparado.contrato;
    const montoAplicable = preparado.montoAplicable;
    this.confirmacionPago.open = false;

    this.pagandoEnCurso = true;
    this.cerrarAmortizar();

    if (this.amortizarModal.tipo === 'separacion') {
      this.cronogramaService.registrarPagoSeparacion(contrato.id, {
        monto: montoAplicable,
        fechaPago: this.amortizarModal.fechaPago || undefined,
        observacion: this.amortizarModal.descripcion?.trim() || undefined,
        metadata: {
          tipo: 'pago_separacion',
          clienteNombre: contrato.clienteNombre,
          medios: this.amortizarModal.medios
        }
      }).subscribe({
        next: (updated) => {
          this.pagandoEnCurso = false;
          this.actualizarContrato(updated);
          this.showNotification('success', 'Pago de separación registrado.');
        },
        error: (error) => {
          this.pagandoEnCurso = false;
          this.showNotification('error', error?.error?.message || 'No se pudo registrar el pago.');
        }
      });
      return;
    }

    const pendientes = this.cuotasPendientesOrdenadas(contrato);
    if (pendientes.length === 0) {
      this.pagandoEnCurso = false;
      this.showNotification('error', 'No hay cuotas pendientes por pagar.');
      return;
    }

    if (this.amortizarModal.modo === 'pago-cuota') {
      const cuota = pendientes[0];
      const saldo = this.saldoPendienteCuotaVisual(cuota, contrato);
      const montoPagoCuota = Math.min(montoAplicable, saldo);
      const numeroCuota = cuota.numeroCuota;
      const clienteNombre = contrato.clienteNombre;
      this.cronogramaService.registrarPagoCuota(cuota.id, {
        monto: montoPagoCuota,
        fechaPago: this.amortizarModal.fechaPago || undefined,
        observacion: this.amortizarModal.descripcion?.trim() || undefined,
        metadata: {
          tipo: 'pago_cuota',
          numeroCuota: numeroCuota,
          clienteNombre: clienteNombre,
          medios: this.amortizarModal.medios
        }
      }).subscribe({
        next: (updated) => {
          this.pagandoEnCurso = false;
          this.actualizarContrato(updated);
          const esParcial = montoPagoCuota < saldo;
          this.showNotification(
            'success',
            esParcial
              ? `Pago parcial registrado en cuota ${cuota.numeroCuota}.`
              : `Cuota ${cuota.numeroCuota} pagada correctamente.`
          );
        },
        error: (error) => {
          this.pagandoEnCurso = false;
          this.showNotification('error', error?.error?.message || 'No se pudo registrar el pago de cuota.');
        }
      });
      return;
    }

    const pagos = pendientes
      .map((cuota) => ({ cuota, saldo: this.saldoPendienteCuotaVisual(cuota, contrato) }))
      .filter((item) => item.saldo > 0);

    let restante = montoAplicable;
    const aplicados: Array<{ cuotaId: number; monto: number; numeroCuota: number; fechaVencimiento?: string; saldoOriginal: number }> = [];
    for (const item of pagos) {
      if (restante <= 0) {
        break;
      }
      const monto = Math.min(restante, item.saldo);
      if (monto > 0) {
        aplicados.push({
          cuotaId: item.cuota.id,
          numeroCuota: item.cuota.numeroCuota,
          fechaVencimiento: item.cuota.fechaVencimiento,
          saldoOriginal: item.saldo,
          monto
        });
        restante -= monto;
      }
    }

    if (aplicados.length === 0) {
      this.pagandoEnCurso = false;
      this.showNotification('error', 'No hay saldo pendiente para aplicar amortización.');
      return;
    }

    const descripcionManual = this.amortizarModal.descripcion?.trim() || '';

    let ultimoResultado: CronogramaContrato | null = null;
    of(...aplicados)
      .pipe(
        concatMap((item) => {
          const cubreCuotaCompleta = item.monto >= item.saldoOriginal;
          const tipoRegistro = cubreCuotaCompleta ? 'pago_cuota' : 'amortizacion';
          const descripcionAuto = this.buildResumenAplicacionItem(item, cubreCuotaCompleta);
          const descripcionFinal = descripcionManual || descripcionAuto;

          return this.cronogramaService.registrarPagoCuota(item.cuotaId, {
            monto: item.monto,
            fechaPago: this.amortizarModal.fechaPago || undefined,
            observacion: descripcionFinal || undefined,
            metadata: {
              tipo: tipoRegistro,
              numeroCuota: item.numeroCuota,
              clienteNombre: this.selectedContrato?.clienteNombre,
              medios: this.amortizarModal.medios
            }
          });
        })
      )
      .subscribe({
        next: (updated) => {
          ultimoResultado = updated;
        },
        error: (error) => {
          this.pagandoEnCurso = false;
          this.showNotification('error', error?.error?.message || 'No se pudo registrar la amortización.');
        },
        complete: () => {
          this.pagandoEnCurso = false;
          if (ultimoResultado) {
            this.actualizarContrato(ultimoResultado);
          }
          this.showNotification('success', 'Amortización aplicada correctamente.');
        }
      });
  }

  private buildResumenAplicacionItem(
    item: { cuotaId: number; monto: number; numeroCuota: number; fechaVencimiento?: string; saldoOriginal: number },
    cubreCuotaCompleta: boolean
  ): string {
    const mes = this.monthLabel(item.fechaVencimiento);
    const texto = cubreCuotaCompleta
      ? `Pago de cuota aplicado a ${mes}`
      : `Amortización parcial de ${mes} (faltante ${this.formatMoney(item.saldoOriginal - item.monto)})`;

    return texto.length > 150 ? texto.substring(0, 150) : texto;
  }

  vueltoGeneral(): number {
    const montoObjetivo = Math.max(0, Number(this.amortizarModal.montoAmortizar || 0));
    if (montoObjetivo <= 0) {
      return 0;
    }
    return Math.max(0, this.totalAmortizar() - montoObjetivo);
  }

  montoIngresadoReal(): number {
    const objetivo = Math.max(0, Number(this.amortizarModal.montoAmortizar || 0));
    const total = this.totalAmortizar();
    if (objetivo <= 0) {
      return total;
    }
    return Math.min(total, objetivo);
  }

  private primeraCuotaPendiente(contrato: CronogramaContrato): CronogramaCuota | null {
    return this.cuotasPendientesOrdenadas(contrato)[0] ?? null;
  }

  private cuotasPendientesOrdenadas(contrato: CronogramaContrato): CronogramaCuota[] {
    return [...(contrato.cuotas || [])]
      .filter((c) => c.estadoPago !== 'PAGADA')
      .sort((a, b) => Number(a.numeroCuota || 0) - Number(b.numeroCuota || 0));
  }

  private saldoPendienteCuotaVisual(cuota: CronogramaCuota, contrato: CronogramaContrato): number {
    const cuotaPlan = this.montoCuotaBase(cuota, contrato);
    return Math.max(0, cuotaPlan - Number(cuota.montoPagado || 0));
  }

  private addMonthsISO(baseDate: string, months: number): string {
    const parsed = new Date(baseDate);
    if (Number.isNaN(parsed.getTime())) {
      return baseDate;
    }

    const d = new Date(parsed.getFullYear(), parsed.getMonth(), parsed.getDate());
    d.setMonth(d.getMonth() + months);

    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  private parseLocalDate(value?: string | null): Date | null {
    if (!value) {
      return null;
    }

    const trimmed = String(value).trim();
    const isoMatch = trimmed.match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (isoMatch) {
      const [, year, month, day] = isoMatch;
      return new Date(Number(year), Number(month) - 1, Number(day));
    }

    const parsed = new Date(trimmed);
    if (Number.isNaN(parsed.getTime())) {
      return null;
    }

    return new Date(parsed.getFullYear(), parsed.getMonth(), parsed.getDate());
  }

  private addDaysLocal(date: Date, days: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
  }

  private formatLocalDateISO(date: Date): string {
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  private diffCalendarDays(from: Date, to: Date): number {
    const utcFrom = Date.UTC(from.getFullYear(), from.getMonth(), from.getDate());
    const utcTo = Date.UTC(to.getFullYear(), to.getMonth(), to.getDate());
    return Math.floor((utcTo - utcFrom) / (1000 * 60 * 60 * 24));
  }

  cronogramaDiasRetrasoMax(contrato: CronogramaContrato | null): number {
    if (!contrato?.cuotas?.length) return 0;
    return Math.max(0, ...contrato.cuotas.map(c => Number(c.diasRetraso || 0)));
  }

  editarAsesor(contrato: CronogramaContrato): void {
    this.asesorEditValue = contrato.asesor || '';
    this.asesorEditing = true;
  }

  cancelarEditarAsesor(): void {
    this.asesorEditing = false;
    this.asesorEditValue = '';
  }

  guardarAsesor(contrato: CronogramaContrato): void {
    if (!contrato?.id) return;
    this.cronogramaService.updateAsesor(contrato.id, this.asesorEditValue).subscribe({
      next: (updated) => {
        const asesorActualizado = (updated?.asesor || this.asesorEditValue || '').trim();
        contrato.asesor = asesorActualizado;

        if (this.selectedContrato?.id === contrato.id) {
          this.selectedContrato = { ...this.selectedContrato, asesor: asesorActualizado };
        }

        this.contratos = this.contratos.map((item) =>
          item.id === contrato.id ? { ...item, asesor: asesorActualizado } : item
        );

        this.asesorEditing = false;
        this.asesorEditValue = '';
        this.cdr.detectChanges();
        this.showNotification('success', 'Asesor actualizado correctamente.');
      },
      error: () => {
        alert('No se pudo guardar el asesor. Intente de nuevo.');
      }
    });
  }

  // Modal de descuento
  descuentoModal: { open: boolean; monto: number; observacion: string; confirm: boolean } = { open: false, monto: 0, observacion: '', confirm: false };

  abrirDescuento(): void {
    if (!this.selectedContrato) return;
    this.descuentoModal = { open: true, monto: 0, observacion: '', confirm: false };
  }

  cerrarDescuentoModal(): void {
    this.descuentoModal = { open: false, monto: 0, observacion: '', confirm: false };
  }

  confirmarDescuento(): void {
    if (!this.descuentoModal.monto || this.descuentoModal.monto <= 0) return;
    this.descuentoModal.confirm = true;
    this.descuentoModal.open = false;
  }

  aplicarDescuento(): void {
    if (!this.selectedContrato || !this.descuentoModal.monto || this.descuentoModal.monto <= 0) return;
    this.descuentoModal.confirm = false;
    this.loading = true;
    // Limpiar cache local para forzar refresco inmediato
    this.cronogramaService.clearCache();
    this.cronogramaService.aplicarDescuento({
      clienteId: this.selectedContrato.clienteId,
      montoDescuento: this.descuentoModal.monto,
      observacion: this.descuentoModal.observacion
    }).pipe(finalize(() => { this.loading = false; this.cerrarDescuentoModal(); }))
      .subscribe({
        next: (updatedContrato) => {
          if (updatedContrato) {
            // Actualizar el contrato seleccionado y la lista
            this.actualizarContrato(updatedContrato);
          }
          this.showNotification('success', 'Descuento aplicado correctamente.');
        },
        error: (err) => {
          this.showNotification('error', err?.error?.message || 'No se pudo aplicar el descuento.');
        }
      });
  }
}
