import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MyHttpClient } from '../my-http-client';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-signup-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './signup-form.html',
  styleUrl: './signup-form.css'
})
export class SignupForm {
  fullName: string = '';
  email: string = '';
  password: string = '';
  confirmPassword: string = '';
  address: string = '';
  phoneNumber: string = '';
  adminCode: string = '';
  securityQuestion: string = '';
  securityAnswer: string = '';
  
  securityQuestions: string[] = [
    'What city were you born in?',
    'What was the name of your first pet?',
    'What was your mother\'s maiden name?',
    'What was the name of your elementary school?',
    'What was your childhood nickname?',
    'What is your favorite movie?',
    'What is the name of your best friend?',
    'What was the make of your first car?',
    'What is your favorite food?',
    'What is the name of the street you grew up on?'
  ];
  
  errors: { [key: string]: string } = {};
  isLoading: boolean = false;
  successMessage: string = '';

  constructor(
    private http: MyHttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  validateForm(): boolean {
    this.errors = {};

    if (!this.fullName || this.fullName.trim().length < 2) {
      this.errors['fullName'] = 'Full name must be at least 2 characters';
    }

    if (!this.email) {
      this.errors['email'] = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email)) {
      this.errors['email'] = 'Please enter a valid email address';
    }

    if (!this.password) {
      this.errors['password'] = 'Password is required';
    } else if (this.password.length < 6) {
      this.errors['password'] = 'Password must be at least 6 characters';
    }

    if (this.password !== this.confirmPassword) {
      this.errors['confirmPassword'] = 'Passwords do not match';
    }

    if (!this.securityQuestion || this.securityQuestion.trim().length === 0) {
      this.errors['securityQuestion'] = 'Please select a security question';
    }

    if (!this.securityAnswer || this.securityAnswer.trim().length < 2) {
      this.errors['securityAnswer'] = 'Security answer must be at least 2 characters';
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
    this.successMessage = '';

    this.http.signup(
      this.email,
      this.password,
      this.fullName,
      this.address,
      this.phoneNumber,
      this.adminCode,
      this.securityQuestion,
      this.securityAnswer
    ).pipe(
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (response) => {
        this.http.saveAuth(response.token, response);
        this.successMessage = 'Account created successfully! Redirecting...';
        setTimeout(() => {
          this.router.navigate(['/']).then(() => {
            window.location.reload(); // Force page reload to refresh admin status
          });
        }, 1500);
      },
      error: (error) => {
        this.isLoading = false;
        try {
          this.errors = (error?.error && typeof error.error === 'object')
            ? { ...error.error, general: error.error.error || error.error.general || error.error.email || error?.message }
            : { general: error?.message || 'Signup failed. Please try again.' };
        } finally {
          this.cdr.detectChanges();
        }
      }
    });
  }
}
