import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError, race, timer } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  fullName: string;
  isAdmin: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class MyHttpClient {
  private baseUrl: string = "http://localhost:8080";
  private token: string | null = null;
  private currentUser: AuthResponse | null = null;
  private authChangedSubject = new BehaviorSubject<void>(undefined);
  authChanged$ = this.authChangedSubject.asObservable();

  constructor(private http: HttpClient) {
    const savedToken = localStorage.getItem('auth_token');
    const savedUser = localStorage.getItem('current_user');
    if (savedToken) {
      this.token = savedToken;
    }
    if (savedUser) {
      this.currentUser = JSON.parse(savedUser);
    }
  }

  private getHeaders(): HttpHeaders {
    let headers = new HttpHeaders();
    if (this.token) {
      headers = headers.set('Authorization', `Bearer ${this.token}`);
    }
    return headers;
  }

  get(url: string): Observable<any> {
    return this.http.get(this.baseUrl + url, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  post(url: string, body: any): Observable<any> {
    return this.http.post(this.baseUrl + url, body, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  put(url: string, body: any): Observable<any> {
    return this.http.put(this.baseUrl + url, body, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  delete(url: string): Observable<any> {
    return this.http.delete(this.baseUrl + url, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  signup(email: string, password: string, fullName: string, address?: string, phoneNumber?: string, adminCode?: string, securityQuestion?: string, securityAnswer?: string): Observable<AuthResponse> {
    const request$ = this.http.post<AuthResponse>(`${this.baseUrl}/api/auth/signup`, {
      email,
      password,
      fullName,
      address,
      phoneNumber,
      adminCode,
      securityQuestion,
      securityAnswer
    });
    const timeout$ = timer(15000).pipe(
      switchMap(() => throwError(() => Object.assign(new Error('Request timed out'), { name: 'TimeoutError' })))
    );
    return race(request$, timeout$).pipe(
      catchError(this.handleError),
      catchError((error) => {
        this.saveAuth(null, null);
        return throwError(() => error);
      })
    );
  }

  login(email: string, password: string): Observable<AuthResponse> {
    const request$ = this.http.post<AuthResponse>(`${this.baseUrl}/api/auth/login`, { email, password });
    const timeout$ = timer(15000).pipe(
      switchMap(() => throwError(() => Object.assign(new Error('Request timed out'), { name: 'TimeoutError' })))
    );
    return race(request$, timeout$).pipe(catchError(this.handleError));
  }

  saveAuth(token: string | null, user: AuthResponse | null): void {
    this.token = token;
    // Normalize: ensure isAdmin exists (backend may use "admin" in some setups)
    if (user) {
      const u = user as AuthResponse & { admin?: boolean };
      this.currentUser = { ...user, isAdmin: user.isAdmin ?? u.admin ?? false };
    } else {
      this.currentUser = null;
    }
    if (token) {
      localStorage.setItem('auth_token', token);
    } else {
      localStorage.removeItem('auth_token');
    }
    if (this.currentUser) {
      localStorage.setItem('current_user', JSON.stringify(this.currentUser));
    } else {
      localStorage.removeItem('current_user');
    }

    // Notify listeners (components) that auth state changed
    this.authChangedSubject.next();
  }

  logout(): void {
    this.saveAuth(null, null);
  }

  isLoggedIn(): boolean {
    return this.token !== null;
  }

  getCurrentUser(): AuthResponse | null {
    return this.currentUser;
  }

  isAdmin(): boolean {
    return this.currentUser?.isAdmin || false;
  }

  refreshUserProfile(): Observable<any> {
    return this.get('/api/user/profile').pipe(
      catchError(this.handleError)
    );
  }

  updateUserProfile(profile: any): void {
    if (this.currentUser && profile) {
      this.currentUser.isAdmin = profile.isAdmin;
      this.currentUser.userId = profile.id || profile.userId;
      this.currentUser.email = profile.email;
      this.currentUser.fullName = profile.fullName;
      localStorage.setItem('current_user', JSON.stringify(this.currentUser));
    }
  }

  uploadImage(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(this.baseUrl + '/api/admin/upload-image', formData, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  getSecurityQuestion(email: string): Observable<any> {
    return this.http.get(this.baseUrl + `/api/auth/forgot-password/question?email=${encodeURIComponent(email)}`)
      .pipe(catchError(this.handleError));
  }

  verifySecurityAnswer(email: string, securityAnswer: string): Observable<any> {
    return this.http.post(this.baseUrl + '/api/auth/forgot-password/verify', {
      email,
      securityAnswer
    }).pipe(catchError(this.handleError));
  }

  resetPassword(email: string, securityAnswer: string, newPassword: string): Observable<any> {
    return this.http.post(this.baseUrl + '/api/auth/forgot-password/reset', {
      email,
      securityAnswer,
      newPassword
    }).pipe(catchError(this.handleError));
  }

  private handleError = (error: any): Observable<never> => {
    let errorMessage = 'An unknown error occurred';
    if (error?.name === 'TimeoutError' || error?.message?.toLowerCase?.().includes('timeout')) {
      errorMessage = `Request timed out. Is the server at ${this.baseUrl} running?`;
    } else if (error?.error instanceof ErrorEvent) {
      errorMessage = `Error: ${(error.error as ErrorEvent).message}`;
    } else {
      const err = error as HttpErrorResponse;
      if (err?.error && typeof err.error === 'object') {
        errorMessage = err.error.error || err.error.message || `Error: ${err.status} ${err.message}`;
      } else {
        errorMessage = err?.message || `Error: ${err?.status} ${err?.message}`;
      }
    }
    return throwError(() => ({ message: errorMessage, status: (error as HttpErrorResponse)?.status, error: (error as HttpErrorResponse)?.error }));
  };
}
