import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, NavigationEnd } from '@angular/router';
import { MyHttpClient } from '../my-http-client';
import { Subscription, filter } from 'rxjs';

interface FeedbackItem {
  id: number;
  message: string;
  submittedAt: string;
  adminReply?: string;
  repliedAt?: string;
  user?: {
    fullName: string;
    email: string;
  };
}

@Component({
  selector: 'app-feedback',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './feedback.html',
  styleUrl: './feedback.css'
})
export class Feedback implements OnInit, OnDestroy {
  feedback: FeedbackItem[] = [];
  filteredFeedback: FeedbackItem[] = [];
  searchQuery: string = '';
  isLoading: boolean = false;
  error: string = '';
  isAdmin: boolean = false;

  // Form Variables
  message: string = '';
  formErrors: { [key: string]: string } = {};
  successMessage: string = '';
  showSuccessAlert: boolean = false;
  isSubmitting: boolean = false;

  // Admin Reply Variables
  replyingTo: number | null = null;
  replyText: { [key: number]: string } = {};
  private routerSub?: Subscription;

  constructor(
    private http: MyHttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadData();

    // Reload data when navigating to this route
    this.routerSub = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      if (this.router.url.includes('/feedback') || this.router.url === '/feedback') {
        this.loadData();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.routerSub) {
      this.routerSub.unsubscribe();
    }
  }

  onSearchChange(): void {
    if (!this.searchQuery.trim()) {
      this.filteredFeedback = this.feedback;
      return;
    }
    const query = this.searchQuery.toLowerCase();
    this.filteredFeedback = this.feedback.filter(f =>
      f.message.toLowerCase().includes(query) ||
      f.user?.fullName?.toLowerCase().includes(query) ||
      f.user?.email?.toLowerCase().includes(query) ||
      f.adminReply?.toLowerCase().includes(query)
    );
  }

  private loadData(): void {
    this.isAdmin = this.http.isAdmin();

    if (this.http.isLoggedIn()) {
      this.loadFeedback();
    } else {
      this.feedback = [];
      this.filteredFeedback = [];
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    this.http.refreshUserProfile().subscribe({
      next: (profile: any) => {
        this.http.updateUserProfile(profile);
        const wasAdmin = this.isAdmin;
        this.isAdmin = profile.isAdmin;
        if (this.isAdmin && (!wasAdmin || this.feedback.length === 0)) {
          this.loadFeedback();
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.isAdmin = this.http.isAdmin();
        if (this.isAdmin && this.http.isLoggedIn()) {
          this.loadFeedback();
        }
        this.cdr.detectChanges();
      }
    });
  }

  loadFeedback(): void {
    if (!this.http.isLoggedIn()) {
      this.feedback = [];
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    // Only show loading if user is likely an admin to avoid flashing for users
    if (this.isAdmin) {
       this.isLoading = true;
    }

    this.error = '';
    this.http.get('/api/admin/feedback').subscribe({
      next: (data: any) => {
        this.feedback = Array.isArray(data) ? data : [];
        this.filteredFeedback = this.feedback;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        // Ignore 401/403 (Regular users)
        if (err.status !== 401 && err.status !== 403) {
          this.error = 'Failed to load feedback';
        }
        this.feedback = [];
        this.filteredFeedback = [];
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  submitFeedback(): void {
    if (!this.http.isLoggedIn()) {
      if (confirm('You need to login to submit feedback. Would you like to go to the login page?')) {
        window.location.href = '/login';
      }
      return;
    }

    this.formErrors = {};
    this.successMessage = '';

    if (!this.message || this.message.trim().length < 10) {
      this.formErrors['message'] = 'Feedback must be at least 10 characters';
      return;
    }

    this.isSubmitting = true;
    this.http.post('/api/feedback', { message: this.message }).subscribe({
      next: () => {
        this.successMessage = 'Thank you for your feedback! It has been submitted successfully.';
        this.showSuccessAlert = true;
        this.message = '';
        this.formErrors = {};
        this.isSubmitting = false;

        setTimeout(() => {
          this.showSuccessAlert = false;
          this.successMessage = '';
        }, 5000);

        if (this.isAdmin) {
          this.loadFeedback();
        }
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.isSubmitting = false;

        // --- STRICT PROFANITY BLOCK ---
        if (err.status === 409 && err.error?.error === 'PROFANITY_WARNING') {
           alert(err.error.message); // Show alert, keep text in box
        } else if (err.status === 401 || err.status === 403) {
          if (confirm('Session expired. You need to login to submit feedback. Go to login?')) {
            window.location.href = '/login';
          }
        } else {
          this.formErrors['general'] = err.error?.error || err.message || 'Failed to submit feedback';
        }
        this.cdr.detectChanges();
      }
    });
  }

  startReply(feedbackId: number): void {
    this.replyingTo = feedbackId;
    if (!this.replyText[feedbackId]) {
      this.replyText[feedbackId] = '';
    }
  }

  cancelReply(): void {
    this.replyingTo = null;
  }

  submitReply(feedbackId: number): void {
    const reply = this.replyText[feedbackId];
    if (!reply || reply.trim().length === 0) {
      alert('Reply cannot be empty');
      return;
    }

    this.http.post(`/api/admin/feedback/${feedbackId}/reply`, { reply: reply.trim() }).subscribe({
      next: () => {
        alert('Reply submitted successfully!');
        this.replyingTo = null;
        this.replyText[feedbackId] = '';
        this.loadFeedback();
      },
      error: (err: any) => {
        alert(err.error?.error || err.message || 'Failed to submit reply');
      }
    });
  }

  deleteFeedback(feedbackId: number): void {
    if (!confirm('Are you sure you want to delete this feedback?')) {
      return;
    }

    this.http.delete(`/api/admin/feedback/${feedbackId}`).subscribe({
      next: () => {
        this.loadFeedback();
        setTimeout(() => alert('Feedback deleted successfully!'), 100);
      },
      error: (err: any) => {
        alert(err.error?.error || err.message || 'Failed to delete feedback');
      }
    });
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
