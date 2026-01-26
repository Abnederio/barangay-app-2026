import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MyHttpClient } from '../my-http-client';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-login-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login-form.html',
  styleUrl: './login-form.css'
})
export class LoginForm {
  email: string = '';
  password: string = '';
  
  errors: { [key: string]: string } = {};
  isLoading: boolean = false;

  constructor(
    private http: MyHttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  validateForm(): boolean {
    this.errors = {};

    if (!this.email) {
      this.errors['email'] = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email)) {
      this.errors['email'] = 'Please enter a valid email address';
    }

    if (!this.password) {
      this.errors['password'] = 'Password is required';
    }

    return Object.keys(this.errors).length === 0;
  }

  onFieldChange(field: string): void {
    if (this.errors[field]) {
      delete this.errors[field];
    }
  }

  onSubmit(): void {
    if (!this.validateForm()) {
      return;
    }

    this.isLoading = true;
    this.errors = {};

    this.http.login(this.email, this.password).pipe(
      finalize(() => {
        // Always unlock the button + refresh UI immediately
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (response) => {
        this.http.saveAuth(response.token, response);
        this.router.navigate(['/']).then(() => {
          window.location.reload(); // Force page reload to refresh admin status
        });
      },
      error: (error) => {
        // Be defensive: never let error parsing keep the form "stuck"
        this.isLoading = false;
        try {
          this.errors = (error?.error && typeof error.error === 'object')
            ? { ...error.error, general: error.error.error || error.error.general || error?.message }
            : { general: error?.message || 'Invalid email or password' };
        } finally {
          this.cdr.detectChanges();
        }
      }
    });
  }
}
