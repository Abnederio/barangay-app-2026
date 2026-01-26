import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MyHttpClient } from '../my-http-client';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css'
})
export class ForgotPassword {
  email: string = '';
  securityQuestion: string = '';
  securityAnswer: string = '';
  newPassword: string = '';
  confirmPassword: string = '';
  
  step: 'email' | 'question' | 'reset' = 'email';
  errors: { [key: string]: string } = {};
  isLoading: boolean = false;
  successMessage: string = '';

  constructor(
    private http: MyHttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  validateEmail(): boolean {
    this.errors = {};
    if (!this.email) {
      this.errors['email'] = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email)) {
      this.errors['email'] = 'Please enter a valid email address';
    }
    return Object.keys(this.errors).length === 0;
  }

  validateSecurityAnswer(): boolean {
    this.errors = {};
    if (!this.securityAnswer) {
      this.errors['securityAnswer'] = 'Security answer is required';
    }
    return Object.keys(this.errors).length === 0;
  }

  validatePassword(): boolean {
    this.errors = {};
    if (!this.newPassword) {
      this.errors['newPassword'] = 'New password is required';
    } else if (this.newPassword.length < 6) {
      this.errors['newPassword'] = 'Password must be at least 6 characters';
    }
    if (this.newPassword !== this.confirmPassword) {
      this.errors['confirmPassword'] = 'Passwords do not match';
    }
    return Object.keys(this.errors).length === 0;
  }

  onFieldChange(field: string): void {
    if (this.errors[field]) {
      delete this.errors[field];
    }
  }

  getSecurityQuestion(): void {
    if (!this.validateEmail()) {
      return;
    }

    this.isLoading = true;
    this.errors = {};
    this.http.getSecurityQuestion(this.email).pipe(
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (response: any) => {
        if (response.securityQuestion) {
          this.securityQuestion = response.securityQuestion;
          this.step = 'question';
        } else {
          // Don't reveal if email exists - show generic message
          this.errors['email'] = 'If this email exists, you will receive the security question.';
        }
      },
      error: (error) => {
        // Don't reveal if email exists
        this.errors['email'] = 'If this email exists, you will receive the security question.';
      }
    });
  }

  verifyAnswer(): void {
    if (!this.validateSecurityAnswer()) {
      return;
    }

    this.isLoading = true;
    this.errors = {};
    this.http.verifySecurityAnswer(this.email, this.securityAnswer).pipe(
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: () => {
        this.step = 'reset';
        this.errors = {};
      },
      error: (error) => {
        this.errors['securityAnswer'] = error?.message || 'Invalid security answer';
      }
    });
  }

  resetPassword(): void {
    if (!this.validatePassword()) {
      return;
    }

    this.isLoading = true;
    this.errors = {};
    this.http.resetPassword(this.email, this.securityAnswer, this.newPassword).pipe(
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: () => {
        this.successMessage = 'Password reset successfully! Redirecting to login...';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (error) => {
        this.errors['general'] = error?.message || 'Failed to reset password';
      }
    });
  }

  backToEmail(): void {
    this.step = 'email';
    this.securityQuestion = '';
    this.securityAnswer = '';
    this.errors = {};
  }

  backToQuestion(): void {
    this.step = 'question';
    this.newPassword = '';
    this.confirmPassword = '';
    this.errors = {};
  }
}
