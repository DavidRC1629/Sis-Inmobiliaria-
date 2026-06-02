import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import { of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { StartupPreloadService } from '../services/startup-preload.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const startupPreloadService = inject(StartupPreloadService);

  if (authService.isAuthenticated()) {
    return startupPreloadService.preloadCoreData().pipe(
      map(() => true),
      catchError(() => of(true))
    );
  }

  router.navigate(['/login']);
  return false;
};

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const startupPreloadService = inject(StartupPreloadService);

  if (authService.isAuthenticated() && authService.isAdmin()) {
    return startupPreloadService.preloadCoreData().pipe(
      map(() => true),
      catchError(() => of(true))
    );
  }

  router.navigate(['/']);
  return false;
};
