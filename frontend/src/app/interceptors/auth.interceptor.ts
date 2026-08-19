import { Injectable } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
  }

  intercept(
    request: HttpRequest<unknown>,
    next: HttpHandler
  ): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken();

    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: 'Bearer ' + token
        }
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // A 401 means the authentication token is no longer accepted.
        // A 403 means the user is still authenticated but is not allowed to
        // perform one particular action, so it must not destroy the session.
        const sessionRejected = error.status === 401;
        const authenticationRequest = request.url.includes('/auth/');

        if (sessionRejected && !authenticationRequest) {
          this.authService.logout();
          this.router.navigate(['/login'], {
            queryParams: { reason: 'session-expired' }
          });
        }

        return throwError(() => error);
      })
    );
  }
}
