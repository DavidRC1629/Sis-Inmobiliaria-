import { Component, inject, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  private router = inject(Router);
  private authService = inject(AuthService);
  
  showProfileMenu = false;
  currentUser: any = null;

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const dropdown = target.closest('.profile-dropdown');
    if (!dropdown && this.showProfileMenu) {
      this.showProfileMenu = false;
    }
  }

  toggleProfileMenu(): void {
    this.showProfileMenu = !this.showProfileMenu;
  }

  goToProfile(): void {
    this.showProfileMenu = false;
    // Ya estamos en el perfil
  }

  goToDashboard(): void {
    this.showProfileMenu = false;
    setTimeout(() => {
      this.router.navigate(['/dashboard']);
    }, 100);
  }

  logout(): void {
    this.showProfileMenu = false;
    this.authService.logout();
  }
}
