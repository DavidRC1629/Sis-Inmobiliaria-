import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // NO enviar token en rutas públicas (login, register, reniec)
  const isPublicRoute =
    req.url.includes('/auth/login') ||
    req.url.includes('/auth/register') ||
    req.url.includes('/auth/forgot-password') ||
    req.url.includes('/api/reniec');

  if (token && !isPublicRoute) {
    console.log('🔐 Interceptor: Agregando token a la petición:', req.url);
    console.log('🔑 Token presente:', token.substring(0, 20) + '...');
    
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  } else if (!isPublicRoute) {
    console.warn('⚠️ Interceptor: NO hay token para:', req.url);
  }

  return next(req);
};
