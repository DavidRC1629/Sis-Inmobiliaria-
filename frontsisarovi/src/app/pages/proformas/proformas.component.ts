
import { Component, ElementRef, HostListener, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProformaService } from '../../services/proforma.service';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user.model';
import { ChangeDetectorRef } from '@angular/core';
import { environment } from '../../../environments/environment';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import { finalize, timeout } from 'rxjs';

@Component({
  selector: 'app-proformas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="dashboard-container">
      <nav class="navbar">
        <div class="navbar-content">
          <div class="logo">
            <h1>AROVI</h1>
            <span class="subtitle">INMOBILIARIA</span>
          </div>
          <div class="header-actions">
            <button *ngIf="!(selectedProjectCard || showGeneratedProforma)" class="btn-back" title="Volver" (click)="headerBack()">←</button>
            <button *ngIf="!(selectedProjectCard || showGeneratedProforma)" class="btn-header-logos" type="button" (click)="toggleLogoPanel()">Logos</button>
            <span class="welcome-name">{{ currentUser?.nombres || 'Usuario' }}</span>
            <div class="dropdown" [class.open]="showDropdown" (mouseleave)="showDropdown = false">
              <button type="button" class="btn-menu" (click)="toggleDropdown()">👤 Mi Perfil</button>
              <div class="dropdown-menu" *ngIf="showDropdown">
                <button (click)="goToPerfil()">👤 Ver perfil</button>
                <button class="btn-logout" (click)="logout()">🚪 Cerrar Sesión</button>
              </div>
            </div>
          </div>
        </div>
      </nav>
      <main class="main-content">
        <ng-container *ngIf="!selectedProjectCard; else proformaForm">
          <h2 class="proformas-title">Seleccione el proyecto para realizar proforma</h2>
          <div class="logo-admin-panel no-print" *ngIf="showLogoPanel">
            <div class="logo-action-feedback" *ngIf="logoActionMessage" [class.success]="logoActionType === 'success'" [class.error]="logoActionType === 'error'">
              {{ logoActionMessage }}
            </div>
            <div class="logo-admin-row">
              <span class="logo-admin-label">Logo general Arovi</span>
              <div class="logo-upload-actions">
                <button class="btn-green btn-logo" type="button" (click)="triggerAroviLogoInput()">{{ canPreviewAroviLogo ? 'Cambiar logo Arovi' : 'Subir logo Arovi' }}</button>
                <button class="btn-eye" type="button" *ngIf="canPreviewAroviLogo" (click)="verLogoArovi()" title="Ver logo Arovi">👁</button>
              </div>
              <input #aroviLogoInput type="file" accept="image/*" class="hidden-file-input" (change)="onAroviLogoFileSelected($event)" />
            </div>
            <div class="logo-admin-row">
              <span class="logo-admin-label">Logo por proyecto</span>
              <select class="logo-project-select" [(ngModel)]="selectedLogoProjectId" name="selectedLogoProjectId">
                <option [ngValue]="null">Seleccione proyecto</option>
                <option *ngFor="let p of proyectos" [ngValue]="p.id">{{ p.nombre }}</option>
              </select>
              <div class="logo-upload-actions">
                <button class="btn-green btn-logo" type="button" [disabled]="!selectedLogoProjectId" (click)="triggerProjectLogoInput()">{{ canPreviewSelectedProjectLogo ? 'Cambiar logo proyecto' : 'Subir logo proyecto' }}</button>
                <button class="btn-eye" type="button" *ngIf="selectedLogoProjectId" (click)="verLogoProyectoSeleccionado()" title="Ver logo del proyecto">👁</button>
              </div>
              <input #projectLogoInput type="file" accept="image/*" class="hidden-file-input" (change)="onProjectLogoFileSelected($event)" />
            </div>

            <div class="confirm-modal-backdrop no-print" *ngIf="showUploadLogoConfirm">
              <div class="confirm-modal" (click)="$event.stopPropagation()">
                <div class="confirm-modal-header">
                  <h3>🖼️ Confirmar nuevo logo</h3>
                </div>
                <div class="confirm-modal-body">
                  <div class="confirm-highlight">
                    <span class="confirm-icon">✅</span>
                    <div>
                      <div class="confirm-title">{{ uploadLogoConfirmTitle }}</div>
                      <div class="confirm-subtitle">La previsualización ya se aplicó en pantalla. Confirma para guardarla de forma permanente.</div>
                    </div>
                  </div>
                  <div class="upload-preview-title" *ngIf="pendingUploadLogoDataUrl">
                    {{ pendingUploadType === 'project' ? 'Imagen del Proyecto' : 'Logo General Arovi' }}
                  </div>
                  <div class="upload-logo-preview-wrap" *ngIf="pendingUploadLogoDataUrl">
                    <img [src]="pendingUploadLogoDataUrl" alt="Previsualización logo" />
                  </div>
                </div>
                <div class="confirm-modal-actions">
                  <button type="button" class="btn-cancel-confirm" [disabled]="uploadingLogo" (click)="cancelUploadLogo()">Cancelar</button>
                  <button type="button" class="btn-ok-confirm" [disabled]="uploadingLogo" (click)="confirmUploadLogo()">{{ uploadingLogo ? 'Guardando...' : 'Confirmar logo' }}</button>
                </div>
              </div>
            </div>

            <div class="logo-preview-backdrop" *ngIf="showLogoPreviewModal" (click)="cerrarVistaLogo()">
              <div class="logo-preview-modal" (click)="$event.stopPropagation()">
                <div class="logo-preview-header">
                  <h4>{{ logoPreviewTitle }}</h4>
                  <button type="button" class="btn-close-logo-preview" (click)="cerrarVistaLogo()">✕</button>
                </div>
                <div class="logo-preview-body">
                  <img *ngIf="logoPreviewUrl" [src]="logoPreviewUrl" alt="Vista previa del logo" />
                  <div *ngIf="!logoPreviewUrl" class="logo-empty-state">No hay logo seleccionado.</div>
                </div>
                <div class="logo-preview-actions">
                  <button type="button" class="btn-delete-logo" *ngIf="canDeleteCurrentLogo" [disabled]="deletingLogo" (click)="eliminarLogoActual()">{{ deletingLogo ? 'Eliminando...' : 'Eliminar logo' }}</button>
                  <button type="button" class="btn-cancel-confirm" (click)="cerrarVistaLogo()">Cerrar</button>
                </div>
              </div>
            </div>

            <div class="confirm-modal-backdrop no-print" *ngIf="showDeleteLogoConfirm" (click)="cancelDeleteLogo()">
              <div class="confirm-modal" (click)="$event.stopPropagation()">
                <div class="confirm-modal-header">
                  <h3>🗑️ Confirmar eliminación de logo</h3>
                </div>
                <div class="confirm-modal-body">
                  <div class="confirm-highlight">
                    <span class="confirm-icon">⚠️</span>
                    <div>
                      <div class="confirm-title">¿Deseas eliminar este logo?</div>
                      <div class="confirm-subtitle">El logo se quitará y volverá a mostrarse el botón para subir uno nuevo.</div>
                    </div>
                  </div>
                </div>
                <div class="confirm-modal-actions">
                  <button type="button" class="btn-cancel-confirm" [disabled]="deletingLogo" (click)="cancelDeleteLogo()">Cancelar</button>
                  <button type="button" class="btn-delete-confirm" [disabled]="deletingLogo" (click)="confirmDeleteLogo()">{{ deletingLogo ? 'Eliminando...' : 'Sí, eliminar' }}</button>
                </div>
              </div>
            </div>
          </div>
          <div class="proformas-projects-grid">
            <div class="project-card">
              <div class="project-img libre">📝</div>
              <div class="project-info">
                <h3>Proforma libre</h3>
                <p>Generar una proforma sin asociar a un proyecto específico.</p>
                <button class="btn-green" (click)="selectProjectCard('libre')">Seleccionar</button>
              </div>
            </div>
            <div class="project-card">
              <div class="project-img libre">📚</div>
              <div class="project-info">
                <h3>Historial de Proformas</h3>
                <p>Ver y buscar proformas por código o por nombres y apellidos.</p>
                <button class="btn-green" (click)="goToHistorialProformas()">Ver historial</button>
              </div>
            </div>
            <div class="project-card" *ngFor="let p of proyectos">
              <img class="project-img" [src]="p.imagenUrl || 'https://via.placeholder.com/400x180?text=Proyecto'" alt="Imagen proyecto" />
              <div class="project-info">
                <h3>{{ p.nombre }}</h3>
                <button class="btn-green" (click)="selectProjectCard(p)">Seleccionar</button>
              </div>
            </div>
          </div>
        </ng-container>
        <ng-template #proformaForm>
          <ng-container *ngIf="!showGeneratedProforma; else generatedView">
            <div class="proforma-toolbar no-print" *ngIf="selectedProjectCard">
              <button class="btn-back-proforma" type="button" (click)="volverASeleccionProforma()">← Volver a selección</button>
            </div>
            <div class="proformas-card">
              <h2>Generar Proforma</h2>
              <form *ngIf="selectedProjectCard !== 'libre'; else libreForm">
                <div class="form-row project-display-row">
                  <label>Proyecto</label>
                  <input type="text" class="project-display-input" [value]="selectedProjectCard.nombre" readonly />
                </div>
                <div class="form-row">
                  <label>Etapa</label>
                  <select [(ngModel)]="selectedEtapa" name="etapa" (change)="onEtapaChange()">
                    <option value="">Seleccione una etapa</option>
                    <option *ngFor="let e of etapas" [value]="e.id">Etapa {{ e.numeroEtapa }}</option>
                  </select>
                </div>
                <div class="form-row">
                  <label>Parcela</label>
                  <select [(ngModel)]="selectedParcela" name="parcela" (change)="onParcelaChange()" [disabled]="!parcelas.length">
                    <option value="">Seleccione una parcela</option>
                    <option *ngFor="let pa of parcelas" [value]="pa.id">{{ pa.nombre }}</option>
                  </select>
                </div>
                <div class="form-row">
                  <label>Manzana</label>
                  <select [(ngModel)]="selectedManzana" name="manzana" (change)="onManzanaChange()" [disabled]="!manzanasFiltradas.length">
                    <option value="">Seleccione una manzana</option>
                    <option *ngFor="let m of manzanasFiltradas" [value]="m.id">{{ m.nombre }}</option>
                  </select>
                </div>
                <div class="form-row">
                  <label>Lote</label>
                  <select [(ngModel)]="selectedLote" name="lote" [disabled]="!lotes.length">
                    <option value="">Seleccione un lote</option>
                    <option *ngFor="let l of lotes" [value]="l.id">{{ l.numero ?? l.nombre }}</option>
                  </select>
                </div>
                <div class="form-error" *ngIf="proformaError">{{ proformaError }}</div>
                <button class="btn-green mt-2" type="button" (click)="generarProforma()">Generar Proforma</button>
              </form>

              <ng-template #libreForm>
                <div class="form-row">
                  <label>Proforma libre</label>
                  <input type="text" value="Proforma libre" disabled />
                </div>
              </ng-template>
            </div>
          </ng-container>

          <ng-template #generatedView>
            <div class="proforma-page">
              <div class="proforma-toolbar no-print">
                <button class="btn-back-proforma" type="button" (click)="volverASeleccionProforma()">← Volver a selección</button>
              </div>

              <div class="proforma-horizontal" #proformaContent>
                <div class="cot-header">
                  <div class="logo-box arovi-box">
                    <img class="logo-arovi-clean" [src]="logoAroviUrl" alt="Logo Arovi" />
                  </div>
                  <div class="company-box">
                    <div class="line-1">AROVI CONSULTORES E INMOBILIARIA S.R.L</div>
                    <div class="line-2">RUC 20601844134</div>
                    <div>PRO. ALFONSO UGARTE MZ A LT 16</div>
                    <div>50 METROS DEL GRIFO LA DOLOROSA - YURIMAGUAS, LORETO.</div>
                    <div>Central telefónica: 921 063 983 – 928 284 031</div>
                  </div>
                  <div class="logo-box right" *ngIf="selectedProjectCard !== 'libre'">
                    <img *ngIf="logoProyectoUrl" [src]="logoProyectoUrl" alt="Logo proyecto" (error)="onProjectLogoError()" />
                  </div>
                  <div class="logo-box right empty" *ngIf="selectedProjectCard === 'libre'"></div>
                </div>

                <div class="cot-title">COTIZACIÓN N.º {{ cotizacionCodigo }}</div>

                <div class="cot-block line-single">
                  <span class="label">PROYECTO:</span>
                  <input class="inline-input upper" maxlength="25" [(ngModel)]="proformaData.proyecto" name="pfProyecto2" (ngModelChange)="onProyectoChange($event)" />
                </div>

                <div class="cot-grid-2">
                  <div class="cot-block"><span class="label">Cliente:</span><input class="inline-input" maxlength="40" [(ngModel)]="proformaData.clienteNombre" name="pfCliente2" (ngModelChange)="onClienteNombreChange($event)" /></div>
                  <div class="cot-block"><span class="label">Asesor de ventas:</span><input class="inline-input" [(ngModel)]="proformaData.asesor" name="pfAsesor2" /></div>
                  <div class="cot-block"><span class="label">DNI:</span><input class="inline-input" maxlength="8" inputmode="numeric" [(ngModel)]="proformaData.clienteDni" name="pfDni2" (ngModelChange)="onDniChange($event)" /></div>
                  <div class="cot-block"><span class="label">Fecha de emisión:</span><input class="inline-input" type="date" [(ngModel)]="proformaData.fechaEmision" name="pfEmision2" /></div>
                  <div class="cot-block"><span class="label">Celular:</span><input class="inline-input" maxlength="11" inputmode="numeric" [(ngModel)]="proformaData.clienteCelular" name="pfCel2" (ngModelChange)="onCelularChange($event)" /></div>
                  <div class="cot-block"><span class="label">Fecha de vencimiento:</span><input class="inline-input" type="date" [(ngModel)]="proformaData.fechaVencimiento" name="pfVenc2" /></div>
                </div>

                <div class="cot-note">Atención: El precio por metro cuadrado es referencial y se calcula en base al redondeo.</div>

                <table class="cot-main-table">
                  <thead>
                    <tr>
                      <th>Mz.</th>
                      <th>Lt.</th>
                      <th>Medida</th>
                      <th>Área</th>
                      <th>Precio m2</th>
                      <th class="head-highlight">Precio Al Contado</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td><input class="table-input center upper" maxlength="1" [(ngModel)]="proformaData.manzana" name="pfMz2" (ngModelChange)="onMzChange($event)" /></td>
                      <td><input class="table-input center upper" maxlength="2" [(ngModel)]="proformaData.lote" name="pfLt2" (ngModelChange)="onLoteChange($event)" /></td>
                      <td><input class="table-input center upper" maxlength="10" [(ngModel)]="proformaData.medidaTexto" name="pfMedida2" (ngModelChange)="onMedidaChange($event)" /></td>
                      <td><input class="table-input center upper" maxlength="8" [ngModel]="formatZeroAsBlank(proformaData.areaM2)" name="pfArea2" (ngModelChange)="onAreaChange($event)" /></td>
                      <td><input class="table-input center" type="text" inputmode="decimal" [ngModel]="formatZeroAsBlank(proformaData.precioM2)" name="pfPrecioM2_2" (ngModelChange)="onPrecioM2InputChange($event)" /></td>
                      <td class="money-highlight">
                        <div class="money-input-wrap highlight">
                          <span>S/</span>
                          <input class="table-input center" type="text" inputmode="decimal" [ngModel]="precioContadoInputRaw !== '' ? precioContadoInputRaw : formatEditableAmountFinal(proformaData.precioContado)" name="pfPrecioContado2" (focus)="onPrecioContadoFocus()" (blur)="onPrecioContadoBlur()" (ngModelChange)="onPrecioContadoInputChange($event)" />
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>

                <table class="cot-summary-table">
                  <colgroup>
                    <col class="col-credito" />
                    <col class="col-cuota" />
                    <col class="col-restante" />
                  </colgroup>
                  <thead>
                    <tr>
                      <th class="credit-header">Precio A Crédito</th>
                      <th>Cuota Inicial</th>
                      <th>Restante</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td class="credit-value">
                        <div class="money-input-wrap">
                          <span>S/</span>
                          <input class="table-input center" type="text" inputmode="decimal" [ngModel]="credito1PrecioInputRaw !== '' ? credito1PrecioInputRaw : formatEditableAmountFinal(proformaData.credito1Precio)" name="pfCredito1Precio2" (focus)="onCredito1PrecioFocus()" (blur)="onCredito1PrecioBlur()" (ngModelChange)="onCredito1PrecioInputChange($event)" />
                        </div>
                      </td>
                      <td>
                        <div class="money-input-wrap">
                          <span>S/</span>
                          <input class="table-input center" type="text" inputmode="decimal" [ngModel]="cuotaInicialEditando ? cuotaInicialInputRaw : formatEditableAmountFinal(proformaData.cuotaInicial)" name="pfCuotaInicial2" (focus)="onCuotaInicialFocus()" (blur)="onCuotaInicialBlur()" (ngModelChange)="onCuotaInicialInputChange($event)" />
                        </div>
                      </td>
                      <td>{{ formatSoles(proformaData.saldoRestante) }}</td>
                    </tr>
                  </tbody>
                </table>

                <div class="plans-grid" [ngClass]="{ 'plans-grid-two': visiblePlanCount === 2, 'plans-grid-one': visiblePlanCount === 1 }">
                  <div class="plan-card" *ngIf="showPlan1">
                    <div class="plan-title plan-title-editable">
                      <span>{{ plan1Titulo }}</span>
                      <div class="plan-title-actions plan-editor-ui no-print" *ngIf="!isPreparingPdf">
                        <button type="button" class="plan-edit-btn" (click)="togglePlanEdit(1)">✎</button>
                        <button type="button" class="plan-delete-btn" (click)="abrirConfirmDeletePlan(1)">✕</button>
                      </div>
                    </div>
                    <div class="plan-edit-inline plan-editor-ui no-print" *ngIf="editPlan1 && !isPreparingPdf">
                      <label>Meses</label>
                      <input type="number" min="2" max="360" [ngModel]="plan1Months" name="plan1MonthsEdit" (ngModelChange)="onPlanMesesInputChange(1, $event)" (keydown.enter)="cerrarEdicionesPlanes()" />
                    </div>
                    <div class="plan-row"><span>Precio a crédito</span><b>{{ formatSoles(proformaData.credito1Precio) }}</b></div>
                    <div class="plan-row"><span>Saldo restante</span><b>{{ formatSoles(proformaData.credito1Saldo) }}</b></div>
                    <div class="plan-row"><span>Cuota referencial</span><b>{{ formatSoles(proformaData.credito1Cuota) }}</b></div>
                    <div class="plan-row emphasized"><span>{{ plan1CuotasPrevias }} cuotas</span><b>{{ formatSoles(cuota11_12) }}</b></div>
                    <div class="plan-row emphasized"><span>1 cuota</span><b>{{ formatSoles(cuotaFinal12) }}</b></div>
                  </div>

                  <div class="plan-card" *ngIf="showPlan2">
                    <div class="plan-title plan-title-editable">
                      <span>{{ plan2Titulo }}</span>
                      <div class="plan-title-actions plan-editor-ui no-print" *ngIf="!isPreparingPdf">
                        <button type="button" class="plan-edit-btn" (click)="togglePlanEdit(2)">✎</button>
                        <button type="button" class="plan-delete-btn" (click)="abrirConfirmDeletePlan(2)">✕</button>
                      </div>
                    </div>
                    <div class="plan-edit-inline plan-editor-ui no-print" *ngIf="editPlan2 && !isPreparingPdf">
                      <label>Meses</label>
                      <input type="number" min="2" max="360" [ngModel]="plan2Months" name="plan2MonthsEdit" (ngModelChange)="onPlanMesesInputChange(2, $event)" (keydown.enter)="cerrarEdicionesPlanes()" />
                      <label>Interés %</label>
                      <input type="number" min="0" max="100" step="0.01" [ngModel]="plan2InterestPercent" name="plan2InterestEdit" (ngModelChange)="onPlanInteresInputChange(2, $event)" (keydown.enter)="cerrarEdicionesPlanes()" />
                    </div>
                    <div class="plan-row"><span>Interés</span><b>{{ formatSoles(proformaData.credito2Interes) }}</b></div>
                    <div class="plan-row"><span>Precio a Crédito</span><b>{{ formatSoles(proformaData.credito2Precio) }}</b></div>
                    <div class="plan-row"><span>Saldo restante</span><b>{{ formatSoles(proformaData.credito2Saldo) }}</b></div>
                    <div class="plan-row"><span>Cuota referencial</span><b>{{ formatSoles(proformaData.credito2Cuota) }}</b></div>
                    <div class="plan-row emphasized"><span>{{ plan2CuotasPrevias }} cuotas</span><b>{{ formatSoles(cuota23_24) }}</b></div>
                    <div class="plan-row emphasized"><span>1 cuota</span><b>{{ formatSoles(cuotaFinal24) }}</b></div>
                  </div>

                  <div class="plan-card" *ngIf="showPlan3">
                    <div class="plan-title plan-title-editable">
                      <span>{{ plan3Titulo }}</span>
                      <div class="plan-title-actions plan-editor-ui no-print" *ngIf="!isPreparingPdf">
                        <button type="button" class="plan-edit-btn" (click)="togglePlanEdit(3)">✎</button>
                        <button type="button" class="plan-delete-btn" (click)="abrirConfirmDeletePlan(3)">✕</button>
                      </div>
                    </div>
                    <div class="plan-edit-inline plan-editor-ui no-print" *ngIf="editPlan3 && !isPreparingPdf">
                      <label>Meses</label>
                      <input type="number" min="2" max="360" [ngModel]="plan3Months" name="plan3MonthsEdit" (ngModelChange)="onPlanMesesInputChange(3, $event)" (keydown.enter)="cerrarEdicionesPlanes()" />
                      <label>Interés %</label>
                      <input type="number" min="0" max="100" step="0.01" [ngModel]="plan3InterestPercent" name="plan3InterestEdit" (ngModelChange)="onPlanInteresInputChange(3, $event)" (keydown.enter)="cerrarEdicionesPlanes()" />
                    </div>
                    <div class="plan-row"><span>Interés</span><b>{{ formatSoles(proformaData.credito3Interes) }}</b></div>
                    <div class="plan-row"><span>Precio A Crédito</span><b>{{ formatSoles(proformaData.credito3Precio) }}</b></div>
                    <div class="plan-row"><span>Saldo Restante</span><b>{{ formatSoles(proformaData.credito3Saldo) }}</b></div>
                    <div class="plan-row"><span>Cuota Referencial</span><b>{{ formatSoles(proformaData.credito3Cuota) }}</b></div>
                    <div class="plan-row emphasized"><span>{{ plan3CuotasPrevias }} cuotas</span><b>{{ formatSoles(cuota35_36) }}</b></div>
                    <div class="plan-row emphasized"><span>1 cuota</span><b>{{ formatSoles(cuotaFinal36) }}</b></div>
                  </div>
                </div>

                <div class="sep-grid">
                  <div>
                    <div><span class="label">MONTO DE SEPARACIÓN:</span></div>
                    <div class="sep-value sep-input-line">
                      <span>Efectivo - </span>
                      <div class="money-input-wrap sep-money-wrap">
                        <span>S/</span>
                        <input class="table-input" type="text" inputmode="decimal" [ngModel]="formatEditableAmount(montoSeparacionMinimoAplicado)" name="pfMontoSeparacion2" (ngModelChange)="onMontoSeparacionInputChange($event)" (blur)="normalizarMontoSeparacionMinimo()" />
                      </div>
                    </div>
                    <div class="sep-value">Restante de efectivo - {{ formatSoles(restanteEfectivo) }}</div>
                  </div>
                  <div>
                    <div class="cot-block"><span class="label">Fecha de separación:</span><input class="inline-input" type="date" [min]="fechaMinimaHoy" [(ngModel)]="proformaData.fechaSeparacion" name="pfFechaSep2" (ngModelChange)="onFechaSeparacionChange($event)" /></div>
                    <div class="cot-block plazo-separacion-row"><span class="label">Plazo de separación:</span><span class="plazo-fijo">{{ plazoSeparacionTexto }} -</span><input class="inline-input" type="date" [min]="fechaMinimaPlazo" [(ngModel)]="proformaData.fechaPlazoSeparacion" name="pfPlazoFecha2" (ngModelChange)="onFechaPlazoSeparacionChange($event)" /></div>
                  </div>
                </div>

                <div class="son-line"><span class="label">SON:</span> {{ montoSeparacionTexto }} soles.</div>

                <div class="titulares-grid" *ngIf="selectedProjectCard !== 'libre'">
                  <div>
                    <div><span class="label">Titular:</span> AROVI CONSULTORES E INMOBILIARIA</div>
                    <div><span class="label">RUC:</span> 20601844134</div>
                  </div>
                  <div>
                    <div><span class="label">Titular:</span> VIVIANA CHUQUIZUTA</div>
                  </div>
                </div>

                <table class="bank-table" *ngIf="selectedProjectCard !== 'libre'">
                  <thead>
                    <tr>
                      <th>BANCO</th>
                      <th>TIPO DE CUENTA</th>
                      <th>N.º DE CUENTA</th>
                      <th>CCI</th>
                      <th>YAPE</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td><input class="table-input center" [(ngModel)]="proformaData.banco" name="pfBanco2" /></td>
                      <td><input class="table-input center" [(ngModel)]="proformaData.tipoCuenta" name="pfTipo2" /></td>
                      <td><input class="table-input center" [(ngModel)]="proformaData.numeroCuenta" name="pfNum2" /></td>
                      <td><input class="table-input center" [(ngModel)]="proformaData.cci" name="pfCci2" /></td>
                      <td><input class="table-input center" [(ngModel)]="proformaData.yapePlin" name="pfYape2" /></td>
                    </tr>
                  </tbody>
                </table>

                <div class="firmas-grid">
                  <div>
                    <div class="firma-line"></div>
                    <div class="firma-label">ASESOR (A)</div>
                  </div>
                  <div>
                    <div class="firma-line"></div>
                    <div class="firma-label">CLIENTE</div>
                  </div>
                </div>
              </div>

              <div class="proforma-print-footer no-print">
                <button class="btn-save-pdf" type="button" [disabled]="guardandoPdf" (click)="openConfirmGuardarImprimir()">
                  {{ guardandoPdf ? 'Guardando e imprimiendo...' : '🖨️ Guardar e Imprimir' }}
                </button>
              </div>
              <div class="form-error no-print" *ngIf="saveValidationError">{{ saveValidationError }}</div>
              <div class="save-message no-print" *ngIf="ultimoCodigoGuardado">
                Proforma guardada con código: <b>{{ ultimoCodigoGuardado }}</b>
              </div>
            </div>

            <div class="confirm-modal-backdrop no-print" *ngIf="showConfirmGuardarImprimir" (click)="cancelConfirmGuardarImprimir()">
              <div class="confirm-modal" (click)="$event.stopPropagation()">
                <div class="confirm-modal-header">
                  <h3>🧾 Confirmar Guardado e Impresión</h3>
                </div>
                <div class="confirm-modal-body">
                  <div class="confirm-highlight">
                    <span class="confirm-icon">🖨️</span>
                    <div>
                      <div class="confirm-title">¿Deseas guardar e imprimir esta proforma?</div>
                      <div class="confirm-subtitle">Se registrará en el historial y se abrirá la vista de impresión.</div>
                    </div>
                  </div>
                </div>
                <div class="confirm-modal-actions">
                  <button type="button" class="btn-cancel-confirm" (click)="cancelConfirmGuardarImprimir()">Cancelar</button>
                  <button type="button" class="btn-ok-confirm" [disabled]="guardandoPdf" (click)="confirmarGuardarImprimir()">Sí, Guardar e Imprimir</button>
                </div>
              </div>
            </div>

            <div class="confirm-modal-backdrop no-print" *ngIf="showDeletePlanConfirm" (click)="cancelDeletePlan()">
              <div class="confirm-modal" (click)="$event.stopPropagation()">
                <div class="confirm-modal-header">
                  <h3>🗑️ Confirmar eliminación</h3>
                </div>
                <div class="confirm-modal-body">
                  <div class="confirm-highlight">
                    <span class="confirm-icon">⚠️</span>
                    <div>
                      <div class="confirm-title">¿Deseas eliminar este cuadro de plan?</div>
                      <div class="confirm-subtitle">El plan seleccionado dejará de mostrarse en la proforma actual.</div>
                    </div>
                  </div>
                </div>
                <div class="confirm-modal-actions">
                  <button type="button" class="btn-cancel-confirm" (click)="cancelDeletePlan()">Cancelar</button>
                  <button type="button" class="btn-delete-confirm" (click)="confirmDeletePlan()">Eliminar</button>
                </div>
              </div>
            </div>
          </ng-template>
        </ng-template>
      </main>
    </div>
  `,
  styleUrls: ['./proformas.component.css', '../dashboard/dashboard.component.css']
})
export class ProformasComponent implements OnInit {
  @ViewChild('proformaContent') proformaContentRef?: ElementRef<HTMLElement>;
  @ViewChild('aroviLogoInput') aroviLogoInputRef?: ElementRef<HTMLInputElement>;
  @ViewChild('projectLogoInput') projectLogoInputRef?: ElementRef<HTMLInputElement>;
  private readonly aroviLogoWidth = 220;
  private readonly aroviLogoHeight = 106;
  private readonly projectLogoWidth = 210;
  private readonly projectLogoHeight = 112;
      get etapaSeleccionadaNombre() {
        const etapa = this.etapas.find(e => e.id == this.selectedEtapa);
        return etapa ? etapa.nombre : '';
      }
      get parcelaSeleccionadaNombre() {
        const parcela = this.parcelas.find(pa => pa.id == this.selectedParcela);
        return parcela ? parcela.nombre : '';
      }
      get manzanaSeleccionadaNombre() {
        const manzana = this.manzanasFiltradas.find(m => m.id == this.selectedManzana);
        return manzana ? manzana.nombre : '';
      }
    get manzanasFiltradas() {
      return this.manzanas;
    }
  // ...existing code...
  toggleDropdown() {
    this.showDropdown = !this.showDropdown;
  }
  goToPerfil() {
    this.router.navigate(['/profile']);
    this.showDropdown = false;
  }
  goToDashboard() {
    this.router.navigate(['/dashboard']);
    this.showDropdown = false;
  }

  goToHistorialProformas(): void {
    this.router.navigate(['/proformas/historial']);
  }

  toggleLogoPanel(): void {
    this.showLogoPanel = !this.showLogoPanel;
  }

  /**
   * Header back behavior: if a project is selected, return to selection grid;
   * otherwise navigate to dashboard.
   */
  headerBack(): void {
    // If we are viewing a generated proforma or we have any selected project (not 'libre'),
    // return to the selection grid. Otherwise navigate to dashboard.
    if (this.showGeneratedProforma || (this.selectedProjectCard && this.selectedProjectCard !== 'libre')) {
      this.selectedProjectCard = null;
      this.selectedEtapa = '';
      this.selectedParcela = '';
      this.selectedManzana = '';
      this.selectedLote = '';
      this.proformaError = '';
      this.etapas = [];
      this.parcelas = [];
      this.manzanas = [];
      this.lotes = [];
      this.showGeneratedProforma = false;
    } else {
      this.goToDashboard();
    }
    this.showDropdown = false;
  }
  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
    this.showDropdown = false;
  }

  proyectos: any[] = [];
  etapas: any[] = [];
  parcelas: any[] = [];
  manzanas: any[] = [];
  lotes: any[] = [];

  selectedProjectCard: any = null;
  selectedEtapa: string = '';
  selectedParcela: string = '';
  selectedManzana: string = '';
  selectedLote: string = '';
  proformaError: string = '';
  showGeneratedProforma: boolean = false;
  proformaData: any = {
    proyecto: '',
    clienteNombre: '',
    clienteDni: '',
    clienteCelular: '',
    fechaEmision: '',
    fechaVencimiento: '',
    asesor: '',
    areaM2: '',
    perimetro: 0,
    precioM2: '',
    medidaTexto: '',
    manzana: '',
    lote: '',
    medidaFrente: 0,
    medidaIzquierda: 0,
    medidaDerecha: 0,
    medidaFondo: 0,
    calle: '',
    propietario: '',
    precioContado: 0,
    cuotaInicial: 0,
    saldoRestante: 0,
    credito1Precio: 0,
    credito1Saldo: 0,
    credito1Cuota: 0,
    credito2Interes: 0,
    credito2Precio: 0,
    credito2Saldo: 0,
    credito2Cuota: 0,
    credito3Interes: 0,
    credito3Precio: 0,
    credito3Saldo: 0,
    credito3Cuota: 0,
    montoSeparacion: 2000,
    fechaSeparacion: '',
    fechaPlazoSeparacion: '',
    plazoSeparacion: '40 DÍAS',
    banco: 'BCP',
    tipoCuenta: 'CUENTA CORRIENTE',
    numeroCuenta: '585-7066303-0-86',
    cci: '00258500706630308687',
    yapePlin: '952 840 431'
  };

  showDropdown = false;
  currentUser: User | null = null;
  private readonly defaultAroviLogoUrl = 'assets/logos/TU_LOGO_AROVI.png';
  logoAroviUrl = this.defaultAroviLogoUrl;
  private readonly defaultProjectLogoUrl = '';
  logoProyectoUrl = this.defaultProjectLogoUrl;
  selectedLogoProjectId: number | null = null;
  showLogoPanel = false;
  guardandoPdf = false;
  ultimoCodigoGuardado = '';
  showConfirmGuardarImprimir = false;
  saveValidationError = '';
  isPreparingPdf = false;
  plan1Months = 12;
  plan2Months = 24;
  plan3Months = 36;
  plan2InterestPercent = 10;
  plan3InterestPercent = 20;
  editPlan1 = false;
  editPlan2 = false;
  editPlan3 = false;
  showPlan1 = true;
  showPlan2 = true;
  showPlan3 = true;
  showDeletePlanConfirm = false;
  pendingDeletePlan: number | null = null;
  showLogoPreviewModal = false;
  logoPreviewUrl = '';
  logoPreviewTitle = 'Vista previa del logo';
  logoPreviewType: 'arovi' | 'project' | null = null;
  deletingLogo = false;
  showDeleteLogoConfirm = false;
  showUploadLogoConfirm = false;
  uploadingLogo = false;
  logoActionMessage = '';
  logoActionType: 'success' | 'error' = 'success';
  uploadLogoConfirmTitle = '¿Deseas guardar este logo?';
  pendingUploadType: 'arovi' | 'project' | null = null;
  pendingUploadLogoDataUrl = '';
  pendingUploadProjectId: number | null = null;
  pendingPreviousAroviLogoUrl = '';
  pendingPreviousProjectLogoUrl = '';
  cuotaInicialInputRaw = '';
  cuotaInicialEditando = false;
  cuotaInicialManual = false;
  precioContadoInputRaw = '';
  credito1PrecioInputRaw = '';
  private readonly montoSeparacionFijo = 2000;
  private quickClientNombre = '';
  private quickClientDni = '';
  private quickMode: 'libre' | 'proyecto' | '' = '';
  private quickProjectId: number | null = null;
  private quickPrecioContado: number | null = null;
  private quickCuotaInicial: number | null = null;
  private quickPlazoMeses: number | null = null;
  private quickInteresPorcentaje: number | null = null;
  private quickFechaOperacion = '';
  private quickProyectoNombre = '';
  private quickEtapaNumero = '';
  private quickParcelaNombre = '';
  private quickManzanaNombre = '';
  private quickLoteNumero = '';

  constructor(
    private proformaService: ProformaService,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private cdRef: ChangeDetectorRef
  ) {}

  get canPreviewAroviLogo(): boolean {
    return !!this.logoAroviUrl && this.logoAroviUrl !== this.defaultAroviLogoUrl;
  }

  get canPreviewSelectedProjectLogo(): boolean {
    return !!this.selectedProjectCustomLogoUrl;
  }

  get canDeleteCurrentLogo(): boolean {
    if (this.logoPreviewType === 'arovi') {
      return this.canPreviewAroviLogo;
    }
    if (this.logoPreviewType === 'project') {
      return this.hasSelectedProjectCustomLogo;
    }
    return false;
  }

  private get hasSelectedProjectCustomLogo(): boolean {
    if (!this.selectedLogoProjectId) {
      return false;
    }
    const selectedProject = this.proyectos.find((p) => Number(p.id) === Number(this.selectedLogoProjectId));
    const raw = String(selectedProject?.logoUrl || '').trim().toLowerCase();
    return !!raw && raw !== 'null' && raw !== 'undefined';
  }

  private get selectedProjectCustomLogoUrl(): string {
    if (!this.selectedLogoProjectId) {
      return '';
    }
    const selectedProject = this.proyectos.find((p) => Number(p.id) === Number(this.selectedLogoProjectId));
    return this.normalizeProjectLogoCandidate(selectedProject?.logoUrl) || '';
  }

  get cotizacionCodigo(): string {
    return this.proformaData?.codigo || '';
  }

  get medidaLoteTexto(): string {
    const frente = Number(this.proformaData?.medidaFrente) || 0;
    const fondo = Number(this.proformaData?.medidaFondo) || 0;
    if (frente > 0 && fondo > 0) {
      return `${frente} X ${fondo}`;
    }
    return this.proformaData?.medidaTexto || '';
  }

  get cuotaFinal12(): number {
    return this.calcularCuotaFinalExacta(Number(this.proformaData?.credito1Saldo), this.cuota11_12, this.plan1CuotasPrevias);
  }

  get cuotaFinal24(): number {
    return this.calcularCuotaFinalExacta(Number(this.proformaData?.credito2Saldo), this.cuota23_24, this.plan2CuotasPrevias);
  }

  get cuotaFinal36(): number {
    return this.calcularCuotaFinalExacta(Number(this.proformaData?.credito3Saldo), this.cuota35_36, this.plan3CuotasPrevias);
  }

  get plan1CuotasPrevias(): number {
    return Math.max(this.normalizarMesesPlan(this.plan1Months) - 1, 1);
  }

  get plan2CuotasPrevias(): number {
    return Math.max(this.normalizarMesesPlan(this.plan2Months) - 1, 1);
  }

  get plan3CuotasPrevias(): number {
    return Math.max(this.normalizarMesesPlan(this.plan3Months) - 1, 1);
  }

  get plan1Titulo(): string {
    return `${this.formatearTiempoPlan(this.plan1Months)} (${this.normalizarMesesPlan(this.plan1Months)})`;
  }

  get plan2Titulo(): string {
    return `${this.formatearTiempoPlan(this.plan2Months)} (${this.normalizarMesesPlan(this.plan2Months)})`;
  }

  get plan3Titulo(): string {
    return `${this.formatearTiempoPlan(this.plan3Months)} (${this.normalizarMesesPlan(this.plan3Months)})`;
  }

  get visiblePlanCount(): number {
    let total = 0;
    if (this.showPlan1) total++;
    if (this.showPlan2) total++;
    if (this.showPlan3) total++;
    return total;
  }

  get cuota11_12(): number {
    const saldo = Number(this.proformaData?.credito1Saldo) || 0;
    const cuotaReferencial = Number(this.proformaData?.credito1Cuota) || 0;
    return this.calcularCuotaRedondeada(saldo, this.plan1CuotasPrevias, cuotaReferencial);
  }

  get cuota23_24(): number {
    const saldo = Number(this.proformaData?.credito2Saldo) || 0;
    const cuotaRef = Number(this.proformaData?.credito2Cuota) || 0;
    return this.calcularCuotaRedondeada(saldo, this.plan2CuotasPrevias, cuotaRef);
  }

  get cuota35_36(): number {
    const saldo = Number(this.proformaData?.credito3Saldo) || 0;
    const cuotaRef = Number(this.proformaData?.credito3Cuota) || 0;
    return this.calcularCuotaRedondeada(saldo, this.plan3CuotasPrevias, cuotaRef);
  }

  togglePlanEdit(plan: number): void {
    if (plan === 1) {
      this.editPlan1 = !this.editPlan1;
      return;
    }
    if (plan === 2) {
      this.editPlan2 = !this.editPlan2;
      return;
    }
    this.editPlan3 = !this.editPlan3;
  }

  onPlanMesesInputChange(plan: number, value: any): void {
    const parsed = this.normalizarMesesPlan(value);
    if (plan === 1) {
      this.plan1Months = parsed;
      this.recalcularPlan1DesdePrecioCredito();
      return;
    }
    if (plan === 2) {
      this.plan2Months = parsed;
      this.recalcularPlan2y3();
      return;
    }
    this.plan3Months = parsed;
    this.recalcularPlan2y3();
  }

  onPlanInteresInputChange(plan: number, value: any): void {
    const percent = this.normalizarPorcentajeInteres(value);
    if (plan === 2) {
      this.plan2InterestPercent = percent;
      this.recalcularPlan2y3();
      return;
    }
    this.plan3InterestPercent = percent;
    this.recalcularPlan2y3();
  }

  abrirConfirmDeletePlan(plan: number): void {
    this.pendingDeletePlan = plan;
    this.showDeletePlanConfirm = true;
  }

  cancelDeletePlan(): void {
    this.showDeletePlanConfirm = false;
    this.pendingDeletePlan = null;
  }

  confirmDeletePlan(): void {
    if (this.pendingDeletePlan === 1) {
      this.showPlan1 = false;
      this.editPlan1 = false;
    } else if (this.pendingDeletePlan === 2) {
      this.showPlan2 = false;
      this.editPlan2 = false;
    } else if (this.pendingDeletePlan === 3) {
      this.showPlan3 = false;
      this.editPlan3 = false;
    }
    this.cancelDeletePlan();
  }

  cerrarEdicionesPlanes(): void {
    this.editPlan1 = false;
    this.editPlan2 = false;
    this.editPlan3 = false;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.showGeneratedProforma || (!this.editPlan1 && !this.editPlan2 && !this.editPlan3)) {
      return;
    }

    const target = event.target as HTMLElement | null;
    if (!target) {
      return;
    }

    if (target.closest('.plan-edit-inline') || target.closest('.plan-edit-btn')) {
      return;
    }

    this.cerrarEdicionesPlanes();
  }

  get montoSeparacionTexto(): string {
    const texto = this.numeroATexto(this.montoSeparacionMinimoAplicado);
    return texto ? texto.charAt(0).toUpperCase() + texto.slice(1) : texto;
  }

  get montoSeparacionMinimoAplicado(): number {
    const montoSeparacion = Number(this.proformaData?.montoSeparacion) || 0;
    return this.normalizarMontoSeparacion(montoSeparacion);
  }

  get restanteEfectivo(): number {
    const cuotaInicial = Number(this.proformaData?.cuotaInicial) || 0;
    const montoSeparacion = this.montoSeparacionMinimoAplicado;
    return this.redondear2(Math.max(cuotaInicial - montoSeparacion, 0));
  }

  get fechaMinimaHoy(): string {
    return this.toDateInputValue(new Date());
  }

  get fechaMinimaPlazo(): string {
    return this.proformaData?.fechaSeparacion || this.fechaMinimaHoy;
  }

  get plazoSeparacionTexto(): string {
    return this.calcularPlazoMesesDiasTexto(this.proformaData?.fechaSeparacion, this.proformaData?.fechaPlazoSeparacion);
  }

  formatSoles(value: any): string {
    const amount = Number(value) || 0;
    return `S/ ${amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  onMzChange(value: string): void {
    const cleaned = (value || '').replace(/[^a-zA-Z]/g, '').slice(0, 1).toUpperCase();
    this.proformaData.manzana = cleaned;
  }

  onProyectoChange(value: string): void {
    this.proformaData.proyecto = (value || '').slice(0, 25);
  }

  onClienteNombreChange(value: string): void {
    this.proformaData.clienteNombre = (value || '').slice(0, 40);
  }

  onDniChange(value: string): void {
    this.proformaData.clienteDni = (value || '').replace(/\D/g, '').slice(0, 8);
  }

  onCelularChange(value: string): void {
    const digits = (value || '').replace(/\D/g, '').slice(0, 9);
    const part1 = digits.slice(0, 3);
    const part2 = digits.slice(3, 6);
    const part3 = digits.slice(6, 9);
    this.proformaData.clienteCelular = [part1, part2, part3].filter(Boolean).join(' ');
  }

  onLoteChange(value: string): void {
    const cleaned = (value || '').replace(/[^a-zA-Z0-9]/g, '').slice(0, 2).toUpperCase();
    this.proformaData.lote = cleaned;
  }

  onMedidaChange(value: string): void {
    const cleaned = (value || '').replace(/[^0-9xX\s]/g, '').replace(/\s+/g, ' ').trim().slice(0, 10).toUpperCase();
    this.proformaData.medidaTexto = cleaned;
  }

  onAreaChange(value: string): void {
    const cleaned = (value || '').replace(/[^a-zA-Z0-9\s]/g, '').replace(/\s+/g, ' ').trim().slice(0, 8).toUpperCase();
    this.proformaData.areaM2 = cleaned === '0' ? '' : cleaned;
  }

  onPrecioM2InputChange(rawValue: string): void {
    const parsed = this.parseEditableAmount(rawValue);
    this.proformaData.precioM2 = parsed === 0 ? '' : parsed;
  }

  onFechaSeparacionChange(value: string): void {
    const minDate = this.fechaMinimaHoy;
    const normalized = !value || value < minDate ? minDate : value;
    this.proformaData.fechaSeparacion = normalized;

    if (!this.proformaData.fechaPlazoSeparacion || this.proformaData.fechaPlazoSeparacion < normalized) {
      this.proformaData.fechaPlazoSeparacion = normalized;
    }
  }

  onFechaPlazoSeparacionChange(value: string): void {
    const minDate = this.fechaMinimaPlazo;
    if (!value) {
      this.proformaData.fechaPlazoSeparacion = minDate;
      return;
    }

    this.proformaData.fechaPlazoSeparacion = value < minDate ? minDate : value;
  }

  onPrecioContadoChange(): void {
    this.proformaData.precioContado = this.redondear2(this.proformaData.precioContado);
  }

  onPrecioContadoInputChange(rawValue: string): void {
    this.precioContadoInputRaw = this.formatearMilesEnEntrada(rawValue);
    if ((this.precioContadoInputRaw || '').trim() === '') {
      this.proformaData.precioContado = 0;
      return;
    }
    this.proformaData.precioContado = this.parseEditableAmount(this.precioContadoInputRaw);
    this.onPrecioContadoChange();
  }

  onPrecioContadoFocus(): void {
    this.precioContadoInputRaw = this.formatEditableAmount(this.proformaData.precioContado);
  }

  onPrecioContadoBlur(): void {
    const raw = (this.precioContadoInputRaw || '').trim();
    this.proformaData.precioContado = raw ? this.parseEditableAmount(raw) : 0;
    this.onPrecioContadoChange();
    this.precioContadoInputRaw = '';
  }

  onCredito1PrecioChange(): void {
    const contado = this.redondear2(this.proformaData.precioContado);
    const creditoActual = this.redondear2(this.proformaData.credito1Precio);
    if (creditoActual < contado) {
      this.proformaData.credito1Precio = contado;
    }
    if (!this.cuotaInicialManual && this.proformaData.credito1Precio > 0) {
      this.proformaData.cuotaInicial = this.obtenerCuotaInicialPorDefecto();
    }
    this.recalcularPlan1DesdePrecioCredito();
  }

  onCredito1PrecioInputChange(rawValue: string): void {
    this.credito1PrecioInputRaw = this.formatearMilesEnEntrada(rawValue);
    if ((this.credito1PrecioInputRaw || '').trim() === '') {
      this.proformaData.credito1Precio = 0;
      this.onCredito1PrecioChange();
      return;
    }
    this.proformaData.credito1Precio = this.parseEditableAmount(this.credito1PrecioInputRaw);
    this.onCredito1PrecioChange();
  }

  onCredito1PrecioFocus(): void {
    this.credito1PrecioInputRaw = this.formatEditableAmount(this.proformaData.credito1Precio);
  }

  onCredito1PrecioBlur(): void {
    const raw = (this.credito1PrecioInputRaw || '').trim();
    this.proformaData.credito1Precio = raw ? this.parseEditableAmount(raw) : 0;
    this.onCredito1PrecioChange();
    this.credito1PrecioInputRaw = '';
  }

  onCuotaInicialInputChange(rawValue: string): void {
    this.cuotaInicialInputRaw = this.formatearMilesEnEntrada(rawValue);
    if ((this.cuotaInicialInputRaw || '').trim() === '') {
      return;
    }
    this.cuotaInicialManual = true;
    this.proformaData.cuotaInicial = this.parseEditableAmount(this.cuotaInicialInputRaw);
    this.normalizarCuotaInicial();
    this.recalcularPlan1DesdePrecioCredito();
  }

  onCuotaInicialFocus(): void {
    this.cuotaInicialEditando = true;
    this.cuotaInicialInputRaw = this.formatEditableAmount(this.proformaData.cuotaInicial);
  }

  onCuotaInicialBlur(): void {
    const raw = (this.cuotaInicialInputRaw || '').trim();
    if (!raw) {
      this.cuotaInicialManual = false;
      this.proformaData.cuotaInicial = this.obtenerCuotaInicialPorDefecto();
    } else {
      this.proformaData.cuotaInicial = this.parseEditableAmount(raw);
    }
    this.normalizarCuotaInicial();
    this.recalcularPlan1DesdePrecioCredito();
    this.cuotaInicialInputRaw = '';
    this.cuotaInicialEditando = false;
  }

  onMontoSeparacionInputChange(rawValue: string): void {
    const parsed = this.parseEditableAmount(rawValue);
    this.proformaData.montoSeparacion = this.normalizarMontoSeparacion(parsed);
  }

  normalizarMontoSeparacionMinimo(): void {
    this.proformaData.montoSeparacion = this.normalizarMontoSeparacion(this.proformaData.montoSeparacion);
  }

  normalizarCuotaInicial(): void {
    const cuota = this.redondear2(Number(this.proformaData.cuotaInicial) || 0);
    const credito = this.redondear2(Number(this.proformaData.credito1Precio) || 0);
    if (credito > 0) {
      this.proformaData.cuotaInicial = Math.min(Math.max(cuota, 0), credito);
      return;
    }
    this.proformaData.cuotaInicial = Math.max(cuota, 0);
  }

  formatEditableAmount(value: any): string {
    const amount = Number(value);
    if (!Number.isFinite(amount)) {
      return '0';
    }
    return amount.toLocaleString('en-US', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2
    });
  }

  formatEditableAmountFinal(value: any): string {
    const amount = Number(value);
    if (!Number.isFinite(amount) || amount === 0) {
      return '0';
    }
    return amount.toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }

  private formatearMilesEnEntrada(rawValue: string): string {
    const digitsOnly = (rawValue || '').replace(/\D/g, '');
    if (!digitsOnly) {
      return '';
    }

    const normalized = digitsOnly.replace(/^0+(?=\d)/, '');
    if (!normalized) {
      return '0';
    }

    return Number(normalized).toLocaleString('en-US', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    });
  }

  formatZeroAsBlank(value: any): string {
    if (value === null || value === undefined) {
      return '';
    }

    const raw = String(value).trim();
    if (!raw) {
      return '';
    }

    const normalized = raw.replace(/,/g, '');
    const numeric = Number(normalized);
    if (Number.isFinite(numeric) && numeric === 0) {
      return '';
    }

    return raw;
  }

  private parseEditableAmount(rawValue: string): number {
    const input = (rawValue || '').trim();
    const cleaned = input.replace(/[^0-9.,]/g, '');
    if (!cleaned) {
      return 0;
    }

    const hasComma = cleaned.includes(',');
    const hasDot = cleaned.includes('.');
    let normalized = cleaned;

    if (hasComma && hasDot) {
      const lastComma = cleaned.lastIndexOf(',');
      const lastDot = cleaned.lastIndexOf('.');
      if (lastComma > lastDot) {
        normalized = cleaned.replace(/\./g, '').replace(',', '.');
      } else {
        normalized = cleaned.replace(/,/g, '');
      }
    } else if (hasComma) {
      const commaCount = (cleaned.match(/,/g) || []).length;
      const lastComma = cleaned.lastIndexOf(',');
      const decimals = cleaned.length - lastComma - 1;
      if (commaCount === 1 && decimals > 0 && decimals <= 2) {
        normalized = cleaned.replace(',', '.');
      } else {
        normalized = cleaned.replace(/,/g, '');
      }
    } else if (hasDot) {
      const dotCount = (cleaned.match(/\./g) || []).length;
      const lastDot = cleaned.lastIndexOf('.');
      const decimals = cleaned.length - lastDot - 1;
      if (!(dotCount === 1 && decimals > 0 && decimals <= 2)) {
        normalized = cleaned.replace(/\./g, '');
      }
    }

    const parsed = Number(normalized);
    if (!Number.isFinite(parsed)) {
      return 0;
    }
    return this.redondear2(parsed);
  }


  ngOnInit() {
    this.readQuickActionFromRoute();

    this.proformaService.getProyectos().subscribe(data => {
      this.proyectos = (data || []).map((p: any) => ({
        ...p,
        logoUrl: this.resolveProjectLogoUrl(p?.logoUrl)
      }));
      this.applyQuickActionSelection();
      this.cdRef.markForCheck();
    });
    this.proformaService.getLogoArovi().subscribe({
      next: async (res) => {
        if (res?.logoAroviUrl) {
          this.logoAroviUrl = await this.trimImageInnerBorder(res.logoAroviUrl);
        }
      },
      error: () => {
      }
    });
    this.authService.currentUser$.subscribe(user => this.currentUser = user);
  }

  private readQuickActionFromRoute(): void {
    const quickAction = this.route.snapshot.queryParamMap.get('quickAction');
    if (quickAction !== '1') {
      return;
    }

    const nombres = (this.route.snapshot.queryParamMap.get('nombres') || '').trim();
    const apellidos = (this.route.snapshot.queryParamMap.get('apellidos') || '').trim();
    this.quickClientNombre = `${nombres} ${apellidos}`.trim().slice(0, 40);
    this.quickClientDni = (this.route.snapshot.queryParamMap.get('dni') || '').replace(/\D/g, '').slice(0, 8);

    const mode = (this.route.snapshot.queryParamMap.get('mode') || '').toLowerCase();
    this.quickMode = mode === 'libre' || mode === 'proyecto' ? (mode as 'libre' | 'proyecto') : '';

    const projectIdRaw = this.route.snapshot.queryParamMap.get('projectId');
    this.quickProjectId = projectIdRaw ? Number(projectIdRaw) : null;

    this.quickPrecioContado = this.toPositiveNumber(this.route.snapshot.queryParamMap.get('precioContado'));
    this.quickCuotaInicial = this.toPositiveNumber(this.route.snapshot.queryParamMap.get('cuotaInicial'));
    this.quickPlazoMeses = this.toPositiveNumber(this.route.snapshot.queryParamMap.get('plazoMeses'));
    this.quickInteresPorcentaje = this.toPositiveNumber(this.route.snapshot.queryParamMap.get('interesPorcentaje'));
    this.quickFechaOperacion = (this.route.snapshot.queryParamMap.get('fechaOperacion') || '').trim();
    this.quickProyectoNombre = (this.route.snapshot.queryParamMap.get('proyectoNombre') || '').trim();
    this.quickEtapaNumero = (this.route.snapshot.queryParamMap.get('etapaNumero') || '').trim();
    this.quickParcelaNombre = (this.route.snapshot.queryParamMap.get('parcelaNombre') || '').trim();
    this.quickManzanaNombre = (this.route.snapshot.queryParamMap.get('manzanaNombre') || '').trim();
    this.quickLoteNumero = (this.route.snapshot.queryParamMap.get('loteNumero') || '').trim();
  }

  private applyQuickActionSelection(): void {
    if (!this.quickMode) {
      return;
    }

    if (this.quickMode === 'libre') {
      this.selectProjectCard('libre');
      this.applyQuickClientToCurrentProforma();
      return;
    }

    if (this.quickMode === 'proyecto' && this.quickProjectId) {
      const selectedProject = this.proyectos.find((p) => Number(p.id) === Number(this.quickProjectId));
      if (selectedProject) {
        this.selectProjectCard(selectedProject);
      }
    }
  }

  private applyQuickClientToCurrentProforma(): void {
    if (this.quickClientNombre) {
      this.onClienteNombreChange(this.quickClientNombre);
    }
    if (this.quickClientDni) {
      this.onDniChange(this.quickClientDni);
    }

    this.applyQuickAdquisicionToCurrentProforma();
  }

  private applyQuickAdquisicionToCurrentProforma(): void {
    if (!this.proformaData) {
      return;
    }

    if (this.quickPrecioContado !== null) {
      this.proformaData.precioContado = this.redondear2(this.quickPrecioContado);
      this.proformaData.credito1Precio = this.redondear2(this.quickPrecioContado);
    }

    if (this.quickCuotaInicial !== null) {
      this.cuotaInicialManual = true;
      this.proformaData.cuotaInicial = this.redondear2(this.quickCuotaInicial);
    }

    if (this.quickPlazoMeses !== null) {
      this.plan1Months = this.normalizarMesesPlan(this.quickPlazoMeses);
    }

    if (this.quickInteresPorcentaje !== null) {
      this.plan2InterestPercent = this.normalizarPorcentajeInteres(this.quickInteresPorcentaje);
    }

    this.proformaData.montoSeparacion = this.montoSeparacionFijo;

    if (this.quickFechaOperacion) {
      this.proformaData.fechaEmision = this.quickFechaOperacion;
      this.proformaData.fechaSeparacion = this.quickFechaOperacion;
    }

    if (this.quickProyectoNombre) {
      this.proformaData.proyecto = this.quickProyectoNombre;
      this.onProyectoChange(this.quickProyectoNombre);
    }

    if (this.quickEtapaNumero) {
      this.proformaData.etapa = `Etapa ${this.quickEtapaNumero}`;
    }

    if (this.quickParcelaNombre) {
      this.proformaData.parcela = this.quickParcelaNombre;
    }

    if (this.quickManzanaNombre) {
      this.proformaData.manzana = this.quickManzanaNombre;
    }

    if (this.quickLoteNumero) {
      this.proformaData.lote = this.quickLoteNumero;
    }

    this.normalizarCuotaInicial();
    this.recalcularPlan1DesdePrecioCredito();
    this.recalcularPlan2y3();
  }

  private toPositiveNumber(raw: string | null): number | null {
    if (!raw) {
      return null;
    }
    const parsed = Number(raw);
    if (!Number.isFinite(parsed) || parsed < 0) {
      return null;
    }
    return parsed;
  }

  triggerAroviLogoInput(): void {
    this.aroviLogoInputRef?.nativeElement.click();
  }

  triggerProjectLogoInput(): void {
    if (!this.selectedLogoProjectId) {
      return;
    }
    this.projectLogoInputRef?.nativeElement.click();
  }

  onAroviLogoFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    if (!file) {
      return;
    }

    this.fileToDataUrl(file).then(async (dataUrl) => {
      const cleanedDataUrl = await this.trimImageInnerBorder(dataUrl);
      const normalizedLogo = await this.normalizeLogoDimensions(cleanedDataUrl, this.aroviLogoWidth, this.aroviLogoHeight);
      this.pendingUploadType = 'arovi';
      this.pendingUploadProjectId = null;
      this.pendingUploadLogoDataUrl = normalizedLogo;
      this.pendingPreviousAroviLogoUrl = this.logoAroviUrl;
      this.uploadLogoConfirmTitle = '¿Deseas confirmar el nuevo logo general Arovi?';
      this.logoAroviUrl = normalizedLogo;
      this.showUploadLogoConfirm = true;
      this.cdRef.detectChanges();
    }).catch(() => {
      console.error('No se pudo leer la imagen del logo Arovi');
    }).finally(() => {
      input.value = '';
    });
  }

  onProjectLogoFileSelected(event: Event): void {
    if (!this.selectedLogoProjectId) {
      return;
    }

    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    if (!file) {
      return;
    }

    this.fileToDataUrl(file).then(async (dataUrl) => {
      const projectId = Number(this.selectedLogoProjectId);
      const normalizedLogo = await this.normalizeLogoDimensions(dataUrl, this.projectLogoWidth, this.projectLogoHeight);
      const selectedProject = this.proyectos.find((p) => Number(p.id) === projectId);
      this.pendingUploadType = 'project';
      this.pendingUploadProjectId = projectId;
      this.pendingUploadLogoDataUrl = normalizedLogo;
      this.pendingPreviousProjectLogoUrl = String(selectedProject?.logoUrl || '');
      this.uploadLogoConfirmTitle = '¿Deseas confirmar el nuevo logo del proyecto?';

      this.proyectos = this.proyectos.map((p) => Number(p.id) === projectId ? { ...p, logoUrl: normalizedLogo } : p);
      if (this.selectedProjectCard && this.selectedProjectCard !== 'libre' && Number(this.selectedProjectCard.id) === projectId) {
        this.selectedProjectCard = { ...this.selectedProjectCard, logoUrl: normalizedLogo };
        this.logoProyectoUrl = normalizedLogo;
      }
      this.showUploadLogoConfirm = true;
      this.cdRef.detectChanges();
    }).catch(() => {
      console.error('No se pudo leer la imagen del logo del proyecto');
    }).finally(() => {
      input.value = '';
    });
  }

  cancelUploadLogo(): void {
    if (this.uploadingLogo) {
      return;
    }

    if (this.pendingUploadType === 'arovi') {
      this.logoAroviUrl = this.pendingPreviousAroviLogoUrl || this.defaultAroviLogoUrl;
    }

    if (this.pendingUploadType === 'project' && this.pendingUploadProjectId) {
      const projectId = Number(this.pendingUploadProjectId);
      const previous = this.pendingPreviousProjectLogoUrl || '';
      const restoredLogo = this.resolveProjectLogoUrl(previous);
      this.proyectos = this.proyectos.map((p) => Number(p.id) === projectId ? { ...p, logoUrl: previous } : p);
      if (this.selectedProjectCard && this.selectedProjectCard !== 'libre' && Number(this.selectedProjectCard.id) === projectId) {
        this.selectedProjectCard = { ...this.selectedProjectCard, logoUrl: previous };
        this.logoProyectoUrl = restoredLogo;
      }
    }

    this.clearPendingUploadState();
  }

  confirmUploadLogo(): void {
    if (this.uploadingLogo || !this.pendingUploadType || !this.pendingUploadLogoDataUrl) {
      return;
    }

    this.logoActionMessage = '';
    this.uploadingLogo = true;

    if (this.pendingUploadType === 'arovi') {
      const pendingLogo = this.pendingUploadLogoDataUrl;
      this.proformaService.updateLogoArovi(pendingLogo).subscribe({
        next: (res) => {
          this.logoAroviUrl = res?.logoAroviUrl || pendingLogo;
          this.clearPendingUploadState();
          this.showLogoActionMessage('Logo general Arovi guardado correctamente.', 'success');
        },
        error: () => {
          this.uploadingLogo = false;
          this.showLogoActionMessage('No se pudo guardar el logo general Arovi. Intenta nuevamente.', 'error');
          console.error('No se pudo guardar el logo general Arovi');
        }
      });
      return;
    }

    if (this.pendingUploadType === 'project' && this.pendingUploadProjectId) {
      const projectId = Number(this.pendingUploadProjectId);
      const pendingLogo = this.pendingUploadLogoDataUrl;
      this.proformaService.updateProjectLogo(projectId, pendingLogo).subscribe({
        next: (updated) => {
          const resolvedLogo = this.resolveProjectLogoUrl(updated?.logoUrl);
          this.proyectos = this.proyectos.map((p) => Number(p.id) === Number(updated.id) ? { ...p, logoUrl: resolvedLogo } : p);
          if (this.selectedProjectCard && this.selectedProjectCard !== 'libre' && Number(this.selectedProjectCard.id) === Number(updated.id)) {
            this.selectedProjectCard = { ...this.selectedProjectCard, logoUrl: resolvedLogo };
            this.logoProyectoUrl = resolvedLogo;
          }
          this.clearPendingUploadState();
          this.showLogoActionMessage('Logo del proyecto guardado correctamente.', 'success');
        },
        error: () => {
          this.uploadingLogo = false;
          this.showLogoActionMessage('No se pudo guardar el logo del proyecto. Intenta nuevamente.', 'error');
          console.error('No se pudo guardar el logo del proyecto');
        }
      });
      return;
    }

    this.uploadingLogo = false;
  }

  private clearPendingUploadState(): void {
    this.showUploadLogoConfirm = false;
    this.uploadingLogo = false;
    this.pendingUploadType = null;
    this.pendingUploadLogoDataUrl = '';
    this.pendingUploadProjectId = null;
    this.pendingPreviousAroviLogoUrl = '';
    this.pendingPreviousProjectLogoUrl = '';
    this.uploadLogoConfirmTitle = '¿Deseas guardar este logo?';
  }

  selectProjectCard(card: any) {
    this.showLogoPanel = false;
    this.selectedProjectCard = card;
    this.selectedEtapa = '';
    this.selectedParcela = '';
    this.selectedLote = '';
    this.selectedManzana = '';
    this.proformaError = '';
    this.showGeneratedProforma = false;
    this.etapas = [];
    this.parcelas = [];
    this.manzanas = [];
    this.lotes = [];

    if (card === 'libre') {
      this.prepareProformaLibre();
      return;
    }

    if (card && card !== 'libre') {
      this.logoProyectoUrl = this.resolveProjectLogoUrl(card?.logoUrl);
      this.proformaService.getEtapas(card.id).subscribe(data => {
        this.etapas = data;
        this.cdRef.markForCheck();
      });
    }
  }

  onProjectLogoError(): void {
    this.logoProyectoUrl = '';
  }

  verLogoArovi(): void {
    this.abrirVistaLogo(this.logoAroviUrl, 'Logo general Arovi', 'arovi');
  }

  verLogoProyectoSeleccionado(): void {
    this.abrirVistaLogo(this.selectedProjectCustomLogoUrl, 'Logo del proyecto', 'project');
  }

  cerrarVistaLogo(): void {
    this.showLogoPreviewModal = false;
    this.logoPreviewUrl = '';
    this.logoPreviewType = null;
    this.deletingLogo = false;
    this.showDeleteLogoConfirm = false;
  }

  eliminarLogoActual(): void {
    if (!this.canDeleteCurrentLogo || this.deletingLogo) {
      return;
    }
    this.showDeleteLogoConfirm = true;
  }

  cancelDeleteLogo(): void {
    if (this.deletingLogo) {
      return;
    }
    this.showDeleteLogoConfirm = false;
  }

  confirmDeleteLogo(): void {
    if (this.deletingLogo || !this.canDeleteCurrentLogo) {
      return;
    }
    this.logoActionMessage = '';
    this.showDeleteLogoConfirm = false;
    this.ejecutarEliminacionLogoActual();
  }

  private ejecutarEliminacionLogoActual(): void {
    if (this.deletingLogo) {
      return;
    }

    if (this.logoPreviewType === 'arovi') {
      this.deletingLogo = true;
      this.proformaService.updateLogoArovi('').pipe(
        timeout(12000),
        finalize(() => {
          this.deletingLogo = false;
        })
      ).subscribe({
        next: () => {
          this.logoAroviUrl = this.defaultAroviLogoUrl;
          this.cerrarVistaLogo();
          this.showLogoActionMessage('Logo general Arovi eliminado correctamente.', 'success');
        },
        error: () => {
          this.showLogoActionMessage('No se pudo eliminar el logo Arovi. Intenta nuevamente.', 'error');
          console.error('No se pudo eliminar el logo Arovi');
        }
      });
      return;
    }

    if (this.logoPreviewType === 'project' && this.selectedLogoProjectId) {
      this.deletingLogo = true;
      const projectId = Number(this.selectedLogoProjectId);
      this.proformaService.updateProjectLogo(projectId, '').pipe(
        timeout(12000),
        finalize(() => {
          this.deletingLogo = false;
        })
      ).subscribe({
        next: (updated) => {
          const resolvedLogo = this.resolveProjectLogoUrl(updated?.logoUrl);
          this.proyectos = this.proyectos.map((p) => p.id === projectId ? { ...p, logoUrl: '' } : p);

          if (this.selectedProjectCard && this.selectedProjectCard !== 'libre' && Number(this.selectedProjectCard.id) === projectId) {
            this.selectedProjectCard = { ...this.selectedProjectCard, logoUrl: '' };
            this.logoProyectoUrl = resolvedLogo;
          }

          this.cerrarVistaLogo();
          this.showLogoActionMessage('Logo del proyecto eliminado correctamente.', 'success');
        },
        error: () => {
          this.showLogoActionMessage('No se pudo eliminar el logo del proyecto. Intenta nuevamente.', 'error');
          console.error('No se pudo eliminar el logo del proyecto');
        }
      });
    }
  }

  private showLogoActionMessage(message: string, type: 'success' | 'error'): void {
    this.logoActionMessage = message;
    this.logoActionType = type;
    this.cdRef.detectChanges();
    setTimeout(() => {
      if (this.logoActionMessage === message) {
        this.logoActionMessage = '';
      }
    }, 4000);
  }

  private abrirVistaLogo(url: string, title: string, type: 'arovi' | 'project'): void {
    this.logoPreviewTitle = title;
    this.logoPreviewUrl = url || '';
    this.logoPreviewType = type;
    this.showLogoPreviewModal = true;
  }

  private resolveProjectLogoUrl(logoUrl?: string | null): string {
    const logoCandidate = this.normalizeProjectLogoCandidate(logoUrl);
    if (logoCandidate) {
      return logoCandidate;
    }

    return this.defaultProjectLogoUrl;
  }

  private normalizeProjectLogoCandidate(value?: string | null): string | null {
    const raw = (value || '').trim();
    if (!raw || raw.toLowerCase() === 'null' || raw.toLowerCase() === 'undefined') {
      return null;
    }

    if (raw.startsWith('data:image/')) {
      return raw;
    }

    if (raw.startsWith('http://') || raw.startsWith('https://')) {
      return this.appendCacheBuster(raw);
    }

    if (raw.startsWith('assets/')) {
      return raw;
    }

    if (raw.startsWith('/')) {
      const base = environment.apiUrl.replace(/\/api\/?$/, '');
      return this.appendCacheBuster(`${base}${raw}`);
    }

    return null;
  }

  private appendCacheBuster(url: string): string {
    const separator = url.includes('?') ? '&' : '?';
    return `${url}${separator}v=${Date.now()}`;
  }

  onEtapaChange() {
    this.selectedParcela = '';
    this.selectedManzana = '';
    this.selectedLote = '';
    this.proformaError = '';
    this.showGeneratedProforma = false;
    this.parcelas = [];
    this.manzanas = [];
    this.lotes = [];
    if (this.selectedEtapa) {
      this.proformaService.getParcelas(+this.selectedEtapa).subscribe(data => this.parcelas = data);
    }
  }

  onParcelaChange() {
    this.selectedManzana = '';
    this.selectedLote = '';
    this.proformaError = '';
    this.showGeneratedProforma = false;
    this.manzanas = [];
    this.lotes = [];
    if (this.selectedParcela) {
      this.proformaService.getManzanas(+this.selectedParcela).subscribe(data => this.manzanas = data);
    }
  }

  onManzanaChange() {
    this.selectedLote = '';
    this.proformaError = '';
    this.showGeneratedProforma = false;
    this.lotes = [];
    if (this.selectedManzana) {
      this.proformaService.getLotesByManzana(+this.selectedManzana).subscribe(data => {
        this.lotes = (data || []).filter(lote => !lote.adquirido);
      });
    }
  }

  generarProforma() {
    this.proformaError = '';
    this.resetPlanesConfigurables();

    if (!this.selectedProjectCard || !this.selectedEtapa || !this.selectedParcela || !this.selectedManzana || !this.selectedLote) {
      this.showGeneratedProforma = false;
      this.proformaError = 'Debes seleccionar Proyecto, Etapa, Parcela, Manzana y Lote para generar la proforma.';
      return;
    }

    const selectedEtapaObj = this.etapas.find(e => String(e.id) === this.selectedEtapa);
    const selectedParcelaObj = this.parcelas.find(p => String(p.id) === this.selectedParcela);
    const selectedManzanaObj = this.manzanas.find(m => String(m.id) === this.selectedManzana);
    const selectedLoteObj = this.lotes.find(l => String(l.id) === this.selectedLote);

    if (!selectedLoteObj) {
      this.showGeneratedProforma = false;
      this.proformaError = 'No se encontró la información del lote seleccionado.';
      return;
    }

    if (selectedLoteObj.adquirido) {
      this.showGeneratedProforma = false;
      this.proformaError = 'Este lote ya está adquirido y no puede ser cotizado.';
      this.selectedLote = '';
      return;
    }

    const today = new Date();
    const dueDate = new Date(today);
    dueDate.setDate(dueDate.getDate() + 40);

    this.proformaData = {
      codigo: this.generarCodigoCotizacionLocal(),
      proyecto: this.selectedProjectCard?.nombre || '',
      etapa: selectedEtapaObj?.numeroEtapa ? `Etapa ${selectedEtapaObj.numeroEtapa}` : '',
      parcela: selectedParcelaObj?.nombre || '',
      manzana: selectedManzanaObj?.nombre || '',
      lote: selectedLoteObj?.numero ?? selectedLoteObj?.nombre ?? '',
      clienteNombre: this.quickClientNombre || '',
      clienteDni: this.quickClientDni || '',
      clienteCelular: '',
      fechaEmision: this.toDateInputValue(today),
      fechaVencimiento: this.toDateInputValue(dueDate),
      asesor: this.currentUser ? `${this.currentUser.nombres} ${this.currentUser.primerApellido}` : '',
      areaM2: (Number(selectedLoteObj?.areaM2) || 0) === 0 ? '' : selectedLoteObj?.areaM2,
      perimetro: selectedLoteObj?.perimetro ?? 0,
      precioM2: (Number(selectedLoteObj?.precioM2) || 0) === 0 ? '' : selectedLoteObj?.precioM2,
      medidaTexto: `${selectedLoteObj?.medidaFrente ?? 0} X ${selectedLoteObj?.medidaFondo ?? 0}`,
      medidaFrente: selectedLoteObj?.medidaFrente ?? 0,
      medidaIzquierda: selectedLoteObj?.medidaIzquierda ?? 0,
      medidaDerecha: selectedLoteObj?.medidaDerecha ?? 0,
      medidaFondo: selectedLoteObj?.medidaFondo ?? 0,
      calle: selectedLoteObj?.calle ?? '',
      propietario: selectedLoteObj?.propietario ?? '',
      precioContado: this.redondear2(Number(selectedLoteObj?.precioLote || 0)),
      cuotaInicial: 0,
      saldoRestante: 0,
      credito1Precio: 0,
      credito1Saldo: 0,
      credito1Cuota: 0,
      credito2Interes: 0,
      credito2Precio: 0,
      credito2Saldo: 0,
      credito2Cuota: 0,
      credito3Interes: 0,
      credito3Precio: 0,
      credito3Saldo: 0,
      credito3Cuota: 0,
      montoSeparacion: 2000,
      fechaSeparacion: this.toDateInputValue(today),
      fechaPlazoSeparacion: this.toDateInputValue(dueDate),
      plazoSeparacion: '40 DÍAS',
      banco: 'BCP',
      tipoCuenta: 'CUENTA CORRIENTE',
      numeroCuenta: '585-7066303-0-86',
      cci: '00258500706630308687',
      yapePlin: '952 840 431'
    };

    this.initializeProformaFinanzas();

    this.showGeneratedProforma = true;
    this.ultimoCodigoGuardado = '';
  }

  volverASeleccionProforma(): void {
    this.showGeneratedProforma = false;
    this.editPlan1 = false;
    this.editPlan2 = false;
    this.editPlan3 = false;
    // Reset selection so user returns to the projects grid (no 'Proforma libre' small form)
    this.selectedProjectCard = null;
    this.selectedEtapa = '';
    this.selectedParcela = '';
    this.selectedManzana = '';
    this.selectedLote = '';
    this.proformaError = '';
    this.etapas = [];
    this.parcelas = [];
    this.manzanas = [];
    this.lotes = [];
  }

  imprimirProforma(): void {
    window.print();
  }

  async guardarEImprimir(): Promise<void> {
    await this.guardarEnPdf(true);
  }

  openConfirmGuardarImprimir(): void {
    const validationMessage = this.validarCamposMinimosGuardar();
    if (validationMessage) {
      this.saveValidationError = validationMessage;
      return;
    }
    this.saveValidationError = '';
    this.showConfirmGuardarImprimir = true;
  }

  cancelConfirmGuardarImprimir(): void {
    if (this.guardandoPdf) {
      return;
    }
    this.showConfirmGuardarImprimir = false;
  }

  async confirmarGuardarImprimir(): Promise<void> {
    this.showConfirmGuardarImprimir = false;
    await this.guardarEImprimir();
  }

  private imprimirPdfYVolverASeleccion(pdfBlob: Blob): void {
    const printUrl = URL.createObjectURL(pdfBlob);
    const iframe = document.createElement('iframe');
    iframe.style.position = 'fixed';
    iframe.style.width = '0';
    iframe.style.height = '0';
    iframe.style.border = '0';
    iframe.style.opacity = '0';
    iframe.src = printUrl;

    let finalizado = false;
    const hardTimeoutId = window.setTimeout(() => {
      finalizar();
    }, 8000);

    const finalizar = () => {
      if (finalizado) {
        return;
      }
      finalizado = true;
      window.clearTimeout(hardTimeoutId);
      try {
        iframe.remove();
      } catch {}
      URL.revokeObjectURL(printUrl);
      this.guardandoPdf = false;
    };

    iframe.onerror = () => {
      finalizar();
    };

    iframe.onload = () => {
      const frameWindow = iframe.contentWindow;
      if (!frameWindow) {
        finalizar();
        return;
      }

      frameWindow.addEventListener('afterprint', finalizar);
      frameWindow.focus();
      frameWindow.print();
      setTimeout(finalizar, 2000);
    };

    document.body.appendChild(iframe);
  }

  async guardarEnPdf(imprimirDespues: boolean = false): Promise<void> {
    const validationMessage = this.validarCamposMinimosGuardar();
    if (validationMessage) {
      this.saveValidationError = validationMessage;
      return;
    }

    const element = this.proformaContentRef?.nativeElement;
    if (!element) {
      return;
    }

    this.saveValidationError = '';
    this.guardandoPdf = true;
    this.ultimoCodigoGuardado = '';
    this.cerrarEdicionesPlanes();

    try {
      if (!this.proformaData.codigo) {
        this.proformaData.codigo = this.generarCodigoCotizacionLocal();
      }

      this.isPreparingPdf = true;
      this.cdRef.detectChanges();
      await new Promise((resolve) => setTimeout(resolve, 0));
      await new Promise((resolve) => requestAnimationFrame(() => resolve(null)));
      const pdfBlob = await this.generarPdfDesdeElemento(element);
      this.isPreparingPdf = false;
      const fileName = `proforma-${new Date().getTime()}.pdf`;

      const payload: any = {
        codigo: this.proformaData.codigo,
        proyecto: this.proformaData.proyecto,
        clienteNombre: this.proformaData.clienteNombre,
        clienteDni: this.proformaData.clienteDni,
        clienteCelular: this.proformaData.clienteCelular,
        asesor: this.proformaData.asesor,
        fechaEmision: this.proformaData.fechaEmision,
        fechaVencimiento: this.proformaData.fechaVencimiento,
        precioContado: this.proformaData.precioContado,
        detalle: this.proformaData
      };

      this.proformaService.createProformaWithPdf(payload, pdfBlob, fileName).subscribe({
        next: (saved) => {
          this.guardandoPdf = false;
          this.ultimoCodigoGuardado = saved?.codigo || '';
          this.proformaData.codigo = saved?.codigo || '';

          const url = URL.createObjectURL(pdfBlob);
          const a = document.createElement('a');
          a.href = url;
          a.download = fileName;
          a.click();
          URL.revokeObjectURL(url);

          if (imprimirDespues) {
            setTimeout(() => {
              this.imprimirPdfYVolverASeleccion(pdfBlob);
            }, 150);
            this.volverASeleccionProforma();
          }
        },
        error: (err) => {
          this.guardandoPdf = false;
          console.error('No se pudo guardar PDF de proforma', err);
        }
      });
    } catch (err) {
      this.isPreparingPdf = false;
      this.guardandoPdf = false;
      console.error('No se pudo generar el PDF', err);
    }
  }

  private async generarPdfDesdeElemento(element: HTMLElement): Promise<Blob> {
    document.body.classList.add('pdf-exporting');
    let canvas: HTMLCanvasElement;
    try {
      canvas = await html2canvas(element, {
        scale: 2,
        useCORS: true,
        backgroundColor: '#ffffff',
        ignoreElements: (node) => {
          const el = node as HTMLElement;
          const classList = el?.classList;
          if (!classList) {
            return false;
          }
          return classList.contains('plan-edit-btn') ||
            classList.contains('plan-edit-inline') ||
            classList.contains('plan-editor-ui') ||
            classList.contains('no-print');
        }
      });
    } finally {
      document.body.classList.remove('pdf-exporting');
    }

    const imgData = canvas.toDataURL('image/jpeg', 0.92);
    const pdf = new jsPDF('p', 'mm', 'a4');
    const pageWidth = pdf.internal.pageSize.getWidth();
    const pageHeight = pdf.internal.pageSize.getHeight();
    const margin = 6;
    const imgWidth = pageWidth - margin * 2;
    const imgHeight = (canvas.height * imgWidth) / canvas.width;

    let heightLeft = imgHeight;
    let position = margin;

    pdf.addImage(imgData, 'JPEG', margin, position, imgWidth, imgHeight);
    heightLeft -= (pageHeight - margin * 2);

    while (heightLeft > 0) {
      position = heightLeft - imgHeight + margin;
      pdf.addPage();
      pdf.addImage(imgData, 'JPEG', margin, position, imgWidth, imgHeight);
      heightLeft -= (pageHeight - margin * 2);
    }

    return pdf.output('blob');
  }

  private initializeProformaFinanzas(): void {
    const area = Number(this.proformaData.areaM2) || 0;
    const precioM2 = Number(this.proformaData.precioM2) || 0;
    const precioContadoActual = Number(this.proformaData.precioContado) || 0;
    const precioContadoInicial = precioContadoActual > 0
      ? this.redondear2(precioContadoActual)
      : Number((area * precioM2).toFixed(2));

    this.proformaData.precioContado = precioContadoInicial;
    this.proformaData.cuotaInicial = 0;
    this.proformaData.credito1Precio = precioContadoInicial;
    this.cuotaInicialInputRaw = '';
    this.cuotaInicialManual = false;
    this.onProyectoChange(this.proformaData.proyecto || '');
    this.onClienteNombreChange(this.proformaData.clienteNombre || '');
    this.onDniChange(this.proformaData.clienteDni || '');
    this.onCelularChange(this.proformaData.clienteCelular || '');
    this.recalcularPlan1DesdePrecioCredito();
    this.recalcularPlan2y3();
  }

  private recalcularFinanzasDesdePrecioContado(): void {
    this.proformaData.precioContado = this.redondear2(this.proformaData.precioContado);
  }

  private recalcularPlan1DesdePrecioCredito(): void {
    this.plan1Months = this.normalizarMesesPlan(this.plan1Months);
    const precioCredito1 = this.redondear2(this.proformaData.credito1Precio);
    this.normalizarCuotaInicial();
    const cuotaInicial = this.redondear2(Number(this.proformaData.cuotaInicial) || 0);
    const saldoPlan1 = this.redondear2(Math.max(precioCredito1 - cuotaInicial, 0));

    this.proformaData.credito1Precio = precioCredito1;
    this.proformaData.saldoRestante = saldoPlan1;
    this.proformaData.credito1Saldo = saldoPlan1;
    this.proformaData.credito1Cuota = this.redondear2(saldoPlan1 / this.plan1Months);

    this.recalcularPlan2y3();
  }

  private recalcularPlan2y3(): void {
    this.plan2Months = this.normalizarMesesPlan(this.plan2Months);
    this.plan3Months = this.normalizarMesesPlan(this.plan3Months);
    this.plan2InterestPercent = this.normalizarPorcentajeInteres(this.plan2InterestPercent);
    this.plan3InterestPercent = this.normalizarPorcentajeInteres(this.plan3InterestPercent);

    const cuotaInicial = this.redondear2(Number(this.proformaData.cuotaInicial) || 0);
    const precioCuotasBase = this.redondear2(Number(this.proformaData.credito1Precio) || 0);
    const restanteBase = this.redondear2(Math.max(precioCuotasBase - cuotaInicial, 0));

    const interes2 = this.redondear2(restanteBase * (this.plan2InterestPercent / 100));
    const precio2 = this.redondear2(precioCuotasBase + interes2);
    const saldo2 = this.redondear2(Math.max(precio2 - cuotaInicial, 0));
    const cuota2 = this.redondear2(saldo2 / this.plan2Months);

    const interes3 = this.redondear2(restanteBase * (this.plan3InterestPercent / 100));
    const precio3 = this.redondear2(precioCuotasBase + interes3);
    const saldo3 = this.redondear2(Math.max(precio3 - cuotaInicial, 0));
    const cuota3 = this.redondear2(saldo3 / this.plan3Months);

    this.proformaData.credito2Interes = interes2;
    this.proformaData.credito2Precio = precio2;
    this.proformaData.credito2Saldo = saldo2;
    this.proformaData.credito2Cuota = cuota2;

    this.proformaData.credito3Interes = interes3;
    this.proformaData.credito3Precio = precio3;
    this.proformaData.credito3Saldo = saldo3;
    this.proformaData.credito3Cuota = cuota3;
  }

  private formatearTiempoPlan(value: any): string {
    const meses = this.normalizarMesesPlan(value);
    if (meses % 12 === 0) {
      const years = meses / 12;
      return `${years} ${years === 1 ? 'año' : 'años'}`;
    }
    return `${meses} meses`;
  }

  private normalizarMesesPlan(value: any): number {
    const parsed = Math.floor(Number(value) || 0);
    if (!Number.isFinite(parsed)) {
      return 12;
    }
    return Math.min(360, Math.max(2, parsed));
  }

  private normalizarPorcentajeInteres(value: any): number {
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) {
      return 0;
    }
    return this.redondear2(Math.min(100, Math.max(0, parsed)));
  }

  private prepareProformaLibre(): void {
    this.resetPlanesConfigurables();
    const today = new Date();
    const dueDate = new Date(today);
    dueDate.setDate(dueDate.getDate() + 40);

    this.proformaData = {
      codigo: this.generarCodigoCotizacionLocal(),
      proyecto: '',
      etapa: '',
      parcela: '',
      manzana: '',
      lote: '',
      clienteNombre: '',
      clienteDni: '',
      clienteCelular: '',
      fechaEmision: this.toDateInputValue(today),
      fechaVencimiento: this.toDateInputValue(dueDate),
      asesor: this.currentUser ? `${this.currentUser.nombres} ${this.currentUser.primerApellido}` : '',
      areaM2: '',
      perimetro: 0,
      precioM2: '',
      medidaTexto: '',
      medidaFrente: 0,
      medidaIzquierda: 0,
      medidaDerecha: 0,
      medidaFondo: 0,
      calle: '',
      propietario: '',
      precioContado: 0,
      cuotaInicial: 0,
      saldoRestante: 0,
      credito1Precio: 0,
      credito1Saldo: 0,
      credito1Cuota: 0,
      credito2Interes: 0,
      credito2Precio: 0,
      credito2Saldo: 0,
      credito2Cuota: 0,
      credito3Interes: 0,
      credito3Precio: 0,
      credito3Saldo: 0,
      credito3Cuota: 0,
      montoSeparacion: 2000,
      fechaSeparacion: this.toDateInputValue(today),
      fechaPlazoSeparacion: this.toDateInputValue(dueDate),
      plazoSeparacion: '40 DÍAS',
      banco: 'BCP',
      tipoCuenta: 'CUENTA CORRIENTE',
      numeroCuenta: '585-7066303-0-86',
      cci: '00258500706630308687',
      yapePlin: '952 840 431'
    };

    this.showGeneratedProforma = true;
    this.ultimoCodigoGuardado = '';
    this.cuotaInicialInputRaw = '';
    this.cuotaInicialManual = false;
  }

  private obtenerCuotaInicialPorDefecto(): number {
    const credito = this.redondear2(Number(this.proformaData?.credito1Precio) || 0);
    if (credito > 0) {
      return Math.min(2000, credito);
    }
    return 0;
  }

  private resetPlanesConfigurables(): void {
    this.plan1Months = 12;
    this.plan2Months = 24;
    this.plan3Months = 36;
    this.plan2InterestPercent = 10;
    this.plan3InterestPercent = 20;
    this.showPlan1 = true;
    this.showPlan2 = true;
    this.showPlan3 = true;
    this.cancelDeletePlan();
    this.cerrarEdicionesPlanes();
  }

  private toDateInputValue(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private generarCodigoCotizacionLocal(): string {
    const letras = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const numero = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
    let texto = '';

    for (let i = 0; i < 3; i++) {
      texto += letras.charAt(Math.floor(Math.random() * letras.length));
    }

    return `${numero}-${texto}`;
  }

  private calcularPlazoMesesDiasTexto(fechaInicio: string, fechaFin: string): string {
    const inicio = this.parseDateOnly(fechaInicio || this.fechaMinimaHoy);
    const fin = this.parseDateOnly(fechaFin || fechaInicio || this.fechaMinimaHoy);

    if (!inicio || !fin || fin.getTime() <= inicio.getTime()) {
      return '0 días';
    }

    let meses = 0;
    let cursor = new Date(inicio.getFullYear(), inicio.getMonth(), inicio.getDate());

    while (true) {
      const siguienteMes = this.addMonthsClamped(cursor, 1);
      if (siguienteMes.getTime() <= fin.getTime()) {
        meses++;
        cursor = siguienteMes;
      } else {
        break;
      }
    }

    const diffMs = fin.getTime() - cursor.getTime();
    const dias = Math.round(diffMs / (1000 * 60 * 60 * 24));

    const partes: string[] = [];
    if (meses > 0) {
      partes.push(`${meses} ${meses === 1 ? 'mes' : 'meses'}`);
    }
    if (dias > 0 || partes.length === 0) {
      partes.push(`${dias} ${dias === 1 ? 'día' : 'días'}`);
    }

    return partes.join(' y ');
  }

  private parseDateOnly(value: string): Date | null {
    const [year, month, day] = (value || '').split('-').map(Number);
    if (!year || !month || !day) {
      return null;
    }
    return new Date(year, month - 1, day);
  }

  private addMonthsClamped(baseDate: Date, months: number): Date {
    const year = baseDate.getFullYear();
    const monthIndex = baseDate.getMonth();
    const day = baseDate.getDate();

    const targetMonthDate = new Date(year, monthIndex + months, 1);
    const targetYear = targetMonthDate.getFullYear();
    const targetMonth = targetMonthDate.getMonth();
    const lastDayOfTargetMonth = new Date(targetYear, targetMonth + 1, 0).getDate();
    const clampedDay = Math.min(day, lastDayOfTargetMonth);

    return new Date(targetYear, targetMonth, clampedDay);
  }

  private fileToDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result || ''));
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  }

  private normalizeLogoDimensions(dataUrl: string, width: number, height: number): Promise<string> {
    return new Promise((resolve) => {
      if (!dataUrl) {
        resolve(dataUrl);
        return;
      }

      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');

        if (!ctx) {
          resolve(dataUrl);
          return;
        }

        ctx.clearRect(0, 0, width, height);
        const scale = Math.min(width / img.naturalWidth, height / img.naturalHeight);
        const drawWidth = img.naturalWidth * scale;
        const drawHeight = img.naturalHeight * scale;
        const x = (width - drawWidth) / 2;
        const y = (height - drawHeight) / 2;

        ctx.drawImage(img, x, y, drawWidth, drawHeight);
        resolve(canvas.toDataURL('image/png'));
      };

      img.onerror = () => resolve(dataUrl);
      img.src = dataUrl;
    });
  }

  private trimImageInnerBorder(dataUrl: string): Promise<string> {
    return new Promise((resolve) => {
      if (!dataUrl) {
        resolve(dataUrl);
        return;
      }

      const img = new Image();
      img.onload = () => {
        try {
          const sourceCanvas = document.createElement('canvas');
          sourceCanvas.width = img.naturalWidth;
          sourceCanvas.height = img.naturalHeight;
          const sourceCtx = sourceCanvas.getContext('2d');
          if (!sourceCtx) {
            resolve(dataUrl);
            return;
          }

          sourceCtx.drawImage(img, 0, 0);
          const { data, width, height } = sourceCtx.getImageData(0, 0, sourceCanvas.width, sourceCanvas.height);

          const samplePixel = (x: number, y: number): [number, number, number, number] => {
            const idx = (y * width + x) * 4;
            return [data[idx], data[idx + 1], data[idx + 2], data[idx + 3]];
          };

          const cornerPoints: Array<[number, number]> = [
            [0, 0],
            [Math.max(0, width - 1), 0],
            [0, Math.max(0, height - 1)],
            [Math.max(0, width - 1), Math.max(0, height - 1)]
          ];

          let bgR = 255;
          let bgG = 255;
          let bgB = 255;
          const validCorners: Array<[number, number, number]> = [];

          for (const [cx, cy] of cornerPoints) {
            const [cr, cg, cb, ca] = samplePixel(cx, cy);
            if (ca > 10) {
              validCorners.push([cr, cg, cb]);
            }
          }

          if (validCorners.length > 0) {
            bgR = Math.round(validCorners.reduce((acc, item) => acc + item[0], 0) / validCorners.length);
            bgG = Math.round(validCorners.reduce((acc, item) => acc + item[1], 0) / validCorners.length);
            bgB = Math.round(validCorners.reduce((acc, item) => acc + item[2], 0) / validCorners.length);
          }

          let minX = width;
          let minY = height;
          let maxX = -1;
          let maxY = -1;

          for (let y = 0; y < height; y++) {
            for (let x = 0; x < width; x++) {
              const idx = (y * width + x) * 4;
              const r = data[idx];
              const g = data[idx + 1];
              const b = data[idx + 2];
              const a = data[idx + 3];

              const isVisible = a > 10;
              const isNearWhite = r > 245 && g > 245 && b > 245;
              const colorDistanceToBg = Math.sqrt(
                Math.pow(r - bgR, 2) +
                Math.pow(g - bgG, 2) +
                Math.pow(b - bgB, 2)
              );
              const isBackgroundLike = colorDistanceToBg < 35;

              if (isVisible && !isNearWhite && !isBackgroundLike) {
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
              }
            }
          }

          if (maxX < minX || maxY < minY) {
            resolve(dataUrl);
            return;
          }

          const padding = 2;
          const cropX = Math.max(0, minX - padding);
          const cropY = Math.max(0, minY - padding);
          const cropW = Math.min(width - cropX, (maxX - minX + 1) + padding * 2);
          const cropH = Math.min(height - cropY, (maxY - minY + 1) + padding * 2);

          const outCanvas = document.createElement('canvas');
          outCanvas.width = cropW;
          outCanvas.height = cropH;
          const outCtx = outCanvas.getContext('2d');
          if (!outCtx) {
            resolve(dataUrl);
            return;
          }

          outCtx.drawImage(sourceCanvas, cropX, cropY, cropW, cropH, 0, 0, cropW, cropH);
          resolve(outCanvas.toDataURL('image/png'));
        } catch {
          resolve(dataUrl);
        }
      };

      img.onerror = () => resolve(dataUrl);
      img.src = dataUrl;
    });
  }

  private calcularCuotaFinalExacta(saldo: number, cuotaRedondeada: number, cuotasPrevias: number): number {
    const calculatedSaldo = Number(saldo) || 0;
    const calculatedCuota = Number(cuotaRedondeada) || 0;
    const finalCuota = calculatedSaldo - (calculatedCuota * cuotasPrevias);
    return this.redondear2(finalCuota);
  }

  private redondear2(value: any): number {
    return Number((Number(value) || 0).toFixed(2));
  }

  private redondearADecenaCercana(value: any): number {
    const numeric = Number(value) || 0;
    return Math.round(numeric / 10) * 10;
  }

  private calcularCuotaRedondeada(saldo: number, cuotasPrevias: number, cuotaReferencial: number): number {
    const referencial = Number(cuotaReferencial) || 0;

    if ((Number(saldo) || 0) <= 0 || cuotasPrevias <= 0) {
      return 0;
    }

    return this.redondear2(this.redondearADecenaCercana(referencial));
  }

  private validarCamposMinimosGuardar(): string {
    const cliente = (this.proformaData?.clienteNombre || '').trim();
    const dni = (this.proformaData?.clienteDni || '').trim();

    if (!cliente) {
      return 'Debes completar el campo Cliente antes de Guardar e Imprimir.';
    }

    if (!/^\d{8}$/.test(dni)) {
      return 'Debes completar el DNI con 8 números antes de Guardar e Imprimir.';
    }

    return '';
  }

  private normalizarMontoSeparacion(value: any): number {
    return this.montoSeparacionFijo;
  }

  private numeroATexto(value: number): string {
    const numberValue = Math.max(0, Number(value) || 0);
    const rounded = Number(numberValue.toFixed(2));
    const integerPart = Math.floor(rounded);
    const decimalPart = Math.round((rounded - integerPart) * 100);
    const integerText = this.numeroEnteroATexto(integerPart);
    return `${integerText} con ${decimalPart.toString().padStart(2, '0')}/100`;
  }

  private numeroEnteroATexto(value: number): string {
    const units = ['', 'uno', 'dos', 'tres', 'cuatro', 'cinco', 'seis', 'siete', 'ocho', 'nueve'];
    const teens = ['diez', 'once', 'doce', 'trece', 'catorce', 'quince', 'dieciséis', 'diecisiete', 'dieciocho', 'diecinueve'];
    const tens = ['', '', 'veinte', 'treinta', 'cuarenta', 'cincuenta', 'sesenta', 'setenta', 'ochenta', 'noventa'];
    const hundreds = ['', 'ciento', 'doscientos', 'trescientos', 'cuatrocientos', 'quinientos', 'seiscientos', 'setecientos', 'ochocientos', 'novecientos'];

    if (value === 0) {
      return 'cero';
    }
    if (value === 100) {
      return 'cien';
    }
    if (value < 10) {
      return units[value];
    }
    if (value < 20) {
      return teens[value - 10];
    }
    if (value < 30) {
      return value === 20 ? 'veinte' : `veinti${units[value - 20]}`;
    }
    if (value < 100) {
      const tenValue = Math.floor(value / 10);
      const unitValue = value % 10;
      return unitValue === 0 ? tens[tenValue] : `${tens[tenValue]} y ${units[unitValue]}`;
    }
    if (value < 1000) {
      const hundredValue = Math.floor(value / 100);
      const remainder = value % 100;
      return remainder === 0 ? hundreds[hundredValue] : `${hundreds[hundredValue]} ${this.numeroEnteroATexto(remainder)}`;
    }
    if (value < 1000000) {
      const thousandValue = Math.floor(value / 1000);
      const remainder = value % 1000;
      const thousandText = thousandValue === 1 ? 'mil' : `${this.numeroEnteroATexto(thousandValue)} mil`;
      return remainder === 0 ? thousandText : `${thousandText} ${this.numeroEnteroATexto(remainder)}`;
    }

    const millionValue = Math.floor(value / 1000000);
    const remainder = value % 1000000;
    const millionText = millionValue === 1 ? 'un millón' : `${this.numeroEnteroATexto(millionValue)} millones`;
    return remainder === 0 ? millionText : `${millionText} ${this.numeroEnteroATexto(remainder)}`;
  }
}
