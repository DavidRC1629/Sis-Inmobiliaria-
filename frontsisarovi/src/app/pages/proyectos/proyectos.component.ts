import { Component, inject, OnInit, ChangeDetectorRef, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-proyectos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './proyectos.component.html',
  styleUrls: ['./proyectos.component.css']
})
export class ProyectosComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);
  private platformId = inject(PLATFORM_ID);
  private isBrowser: boolean;

  proyectos: any[] = [];
  userName: string = '';
  userId: number = 0;
  showModal: boolean = false;
  selectedFile: File | null = null;
  showSuccessMessage: boolean = false;
  errorManzanas: string = '';
  
  nuevoProyecto = {
    nombre: '',
    perteneceA: '',
    foto: '',
    cantidadLotes: 0,
    cantidadManzanas: 0
  };

  constructor() {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit(): void {
    console.log('ProyectosComponent inicializado');
    if (this.isBrowser) {
      const userStr = localStorage.getItem('currentUser');
      if (userStr) {
        const user = JSON.parse(userStr);
        this.userName = `${user.nombres} ${user.primerApellido}`;
        this.userId = user.id;
        console.log('Usuario:', this.userName);
      }
      this.cargarProyectos();
    }
  }

  cargarProyectos(): void {
    if (!this.isBrowser) return;
    
    const token = localStorage.getItem('jwt_token');
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    
    console.log('Iniciando carga de proyectos...');
    console.log('Token:', token ? 'Presente' : 'No encontrado');
    
    this.http.get<any[]>('http://localhost:8080/api/proyectos', { headers })
      .subscribe({
        next: (proyectos) => {
          console.log('Proyectos recibidos del backend:', proyectos);
          this.proyectos = [...proyectos];
          console.log('Array proyectos asignado:', this.proyectos);
          console.log('Longitud del array:', this.proyectos.length);
          setTimeout(() => this.cdr.detectChanges(), 100);
        },
        error: (err) => {
          console.error('Error al cargar proyectos:', err);
          console.error('Status:', err.status);
          console.error('Message:', err.message);
        }
      });
  }

  abrirModal(): void {
    this.showModal = true;
  }

  cerrarModal(): void {
    this.showModal = false;
    this.resetForm();
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = () => {
        this.nuevoProyecto.foto = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  validarCantidadManzanas(): void {
    if (this.nuevoProyecto.cantidadManzanas >= 28) {
      this.errorManzanas = 'Desde el número 28 no hay letra en el abecedario';
      this.nuevoProyecto.cantidadManzanas = 27;
    } else {
      this.errorManzanas = '';
    }
  }

  guardarProyecto(): void {
    if (!this.isBrowser) return;
    
    const token = localStorage.getItem('jwt_token');
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    
    this.http.post('http://localhost:8080/api/proyectos', this.nuevoProyecto, { headers })
      .subscribe({
        next: () => {
          this.cargarProyectos();
          this.cerrarModal();
          this.showSuccessMessage = true;
          setTimeout(() => {
            this.showSuccessMessage = false;
          }, 3000);
        },
        error: (err) => console.error('Error al crear proyecto:', err)
      });
  }

  resetForm(): void {
    this.nuevoProyecto = {
      nombre: '',
      perteneceA: '',
      foto: '',
      cantidadLotes: 0,
      cantidadManzanas: 0
    };
    this.selectedFile = null;
    this.errorManzanas = '';
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  goBack() {
    this.router.navigate(['../']);
  }

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }
}
