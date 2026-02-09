import { Component, OnDestroy, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MyHttpClient } from '../my-http-client';
import { Subscription } from 'rxjs';

interface Event {
  id: number;
  title: string;
  description: string;
  eventDate: string;
  location: string;
  imageUrl?: string;
  likeCount?: number;
  isLiked?: boolean;
  comments?: Comment[];
}

interface Comment {
  id: number;
  content: string;
  createdAt: string;
  user?: {
    id: number;
    fullName: string;
    email: string;
  };
}

@Component({
  selector: 'app-events',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './events.html',
  styleUrl: './events.css'
})
export class Events implements OnInit, OnDestroy {
  events: Event[] = [];
  filteredEvents: Event[] = [];
  searchQuery: string = '';
  isLoading: boolean = true;
  error: string = '';
  isAdmin: boolean = false;
  private authSub?: Subscription;

  showCreateForm: boolean = false;
  editingEvent: Event | null = null;
  newEvent = {
    title: '',
    description: '',
    eventDate: '',
    location: '',
    imageUrl: ''
  };
  selectedImageFile: File | null = null;
  imagePreview: string | null = null;
  isUploadingImage: boolean = false;
  formErrors: { [key: string]: string } = {};

  expandedComments: Set<number> = new Set();
  commentTexts: { [key: number]: string } = {};

  constructor(
    private http: MyHttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.http.isAdmin();
    this.authSub = this.http.authChanged$.subscribe(() => {
      // When switching accounts, refresh admin flag and like/check state
      this.isAdmin = this.http.isAdmin();
      this.events.forEach(e => this.loadLikes('EVENT', e.id, e));
      this.cdr.detectChanges();
    });
    // Refresh admin status from backend to ensure accuracy
    if (this.http.isLoggedIn()) {
      this.http.refreshUserProfile().subscribe({
        next: (profile: any) => {
          this.http.updateUserProfile(profile);
          this.isAdmin = profile.isAdmin;
        },
        error: () => {
          this.isAdmin = this.http.isAdmin();
        }
      });
    }
    this.loadEvents();
  }

  ngOnDestroy(): void {
    this.authSub?.unsubscribe();
  }

  onSearchChange(): void {
    if (!this.searchQuery.trim()) {
      this.filteredEvents = this.events;
      return;
    }
    const query = this.searchQuery.toLowerCase();
    this.filteredEvents = this.events.filter(e =>
      e.title.toLowerCase().includes(query) ||
      e.description?.toLowerCase().includes(query) ||
      e.location?.toLowerCase().includes(query)
    );
  }

  loadEvents(): void {
    this.isLoading = true;
    this.error = '';
    this.http.get('/api/public/events').subscribe({
      next: (data: unknown) => {
        try {
          const list = Array.isArray(data) ? (data as Event[]) : [];
          // Sort by eventDate descending (newest first)
          this.events = list.sort((a, b) => {
            const dateA = new Date(a.eventDate).getTime();
            const dateB = new Date(b.eventDate).getTime();
            return dateB - dateA;
          });
          this.filteredEvents = [...this.events];
          // Load likes and comments for each event
          this.events.forEach((event) => {
            // Reset UI state so we don't show stale likes after account switch
            event.likeCount = 0;
            event.isLiked = false;
            this.loadLikes('EVENT', event.id, event);
            this.loadComments('EVENT', event.id, event);
          });
        } finally {
          this.isLoading = false;
          this.cdr.detectChanges(); // Force change detection after loading
        }
      },
      error: (err) => {
        this.error = 'Failed to load events';
        this.isLoading = false;
        this.events = [];
        this.filteredEvents = [];
        this.cdr.detectChanges();
      }
    });
  }

  startEdit(event: Event): void {
    this.editingEvent = event;
    this.newEvent = {
      title: event.title,
      description: event.description || '',
      eventDate: this.formatDateForInput(event.eventDate),
      location: event.location || '',
      imageUrl: event.imageUrl || ''
    };
    this.selectedImageFile = null;
    this.imagePreview = event.imageUrl || null;
    this.showCreateForm = true;
  }

  cancelEdit(): void {
    this.editingEvent = null;
    this.showCreateForm = false;
    this.newEvent = { title: '', description: '', eventDate: '', location: '', imageUrl: '' };
    this.selectedImageFile = null;
    this.imagePreview = null;
    this.formErrors = {};
  }

  onImageSelected(event: any): void {
    const file = event.target.files?.[0];
    if (file) {
      if (file.size > 10 * 1024 * 1024) {
        alert('Image size must be less than 10MB');
        return;
      }
      if (!file.type.startsWith('image/')) {
        alert('Please select an image file');
        return;
      }
      this.selectedImageFile = file;
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.imagePreview = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  uploadImage(): void {
    if (!this.selectedImageFile) {
      return;
    }

    this.isUploadingImage = true;
    this.http.uploadImage(this.selectedImageFile).subscribe({
      next: (response: any) => {
        this.newEvent.imageUrl = response.imageUrl;
        this.isUploadingImage = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        alert(err.message || 'Failed to upload image');
        this.isUploadingImage = false;
      }
    });
  }

  removeImage(): void {
    this.selectedImageFile = null;
    this.imagePreview = null;
    this.newEvent.imageUrl = '';
  }

  formatDateForInput(dateString: string): string {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  saveEvent(): void {
    this.formErrors = {};

    if (!this.newEvent.title) {
      this.formErrors['title'] = 'Event title is required';
    }
    if (!this.newEvent.eventDate) {
      this.formErrors['eventDate'] = 'Event date is required';
    }

    if (Object.keys(this.formErrors).length > 0) {
      return;
    }

    // If image is selected but not uploaded yet, upload it first
    if (this.selectedImageFile && !this.newEvent.imageUrl) {
      this.uploadImage();
      const checkUpload = setInterval(() => {
        if (!this.isUploadingImage && this.newEvent.imageUrl) {
          clearInterval(checkUpload);
          this.submitEvent();
        } else if (!this.isUploadingImage && !this.newEvent.imageUrl) {
          clearInterval(checkUpload);
          alert('Failed to upload image. Please try again.');
        }
      }, 100);
      return;
    }

    this.submitEvent();
  }

  private submitEvent(): void {
    if (this.editingEvent) {
      this.http.put(`/api/admin/events/${this.editingEvent.id}`, this.newEvent).subscribe({
        next: () => {
          this.cancelEdit();
          this.loadEvents();
          this.cdr.detectChanges();
          setTimeout(() => {
            alert('Event updated successfully!');
          }, 100);
        },
        error: (err) => {
          alert(err.message || 'Failed to update event');
        }
      });
    } else {
      this.http.post('/api/admin/events', this.newEvent).subscribe({
        next: (response: any) => {
          // Reset form first
          this.newEvent = {
            title: '',
            description: '',
            eventDate: '',
            location: '',
            imageUrl: ''
          };
          this.selectedImageFile = null;
          this.imagePreview = null;
          this.formErrors = {};
          this.showCreateForm = false;
          this.editingEvent = null;
          // Reload events to show the new one - use a small delay to ensure backend has saved
          setTimeout(() => {
            this.loadEvents();
            this.cdr.detectChanges();
          }, 200);
          setTimeout(() => {
            alert('Event created successfully!');
          }, 300);
        },
        error: (err) => {
          alert(err.message || 'Failed to create event');
        }
      });
    }
  }

  deleteEvent(eventId: number): void {
    if (confirm('Are you sure you want to delete this event?')) {
      this.http.delete(`/api/admin/events/${eventId}`).subscribe({
        next: () => {
          this.loadEvents(); // Reload first
          setTimeout(() => {
            alert('Event deleted successfully!');
          }, 100);
        },
        error: (err) => {
          alert(err.message || 'Failed to delete event');
        }
      });
    }
  }

  loadLikes(entityType: string, entityId: number, item: any): void {
    this.http.get(`/api/likes/${entityType}/${entityId}`).subscribe({
      next: (data: any) => {
        item.likeCount = data.count || 0;
        this.cdr.detectChanges();
      },
      error: () => {
        item.likeCount = 0;
        this.cdr.detectChanges();
      }
    });

    // Always call check: backend returns { liked: false } when not logged in
    this.http.get(`/api/likes/${entityType}/${entityId}/check`).subscribe({
      next: (data: any) => {
        item.isLiked = data.liked || false;
        this.cdr.detectChanges();
      },
      error: () => {
        item.isLiked = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadComments(entityType: string, entityId: number, item: any): void {
    this.http.get(`/api/comments/${entityType}/${entityId}`).subscribe({
      next: (data: Comment[]) => {
        item.comments = data || [];
      },
      error: () => {
        item.comments = [];
      }
    });
  }

  toggleLike(entityType: string, entityId: number, item: any): void {
    if (!this.http.isLoggedIn()) {
      if (confirm('You need to login to like. Would you like to go to the login page?')) {
        window.location.href = '/login';
      }
      return;
    }

    this.http.post('/api/likes', {
      entityType: entityType,
      entityId: entityId.toString()
    }).subscribe({
      next: () => {
        // After toggling like, refresh just this item's likes from the server
        this.loadLikes(entityType, entityId, item);
      },
      error: (err: any) => {
        if (err.status === 401 || err.status === 403) {
          if (confirm('You need to login to like. Would you like to go to the login page?')) {
            window.location.href = '/login';
          }
        }
      }
    });
  }

  toggleComments(eventId: number): void {
    if (this.expandedComments.has(eventId)) {
      this.expandedComments.delete(eventId);
    } else {
      this.expandedComments.add(eventId);
    }
  }

  // ... (keep existing code)

    addComment(entityType: string, entityId: number, item: any, confirmed: boolean = false): void {
      if (!this.http.isLoggedIn()) {
        if (confirm('You need to login to comment. Would you like to go to the login page?')) {
          window.location.href = '/login';
        }
        return;
      }

      const commentText = this.commentTexts[entityId]?.trim();
      if (!commentText || commentText.length < 1) {
        return;
      }

      this.http.post('/api/comments', {
        entityType: entityType,
        entityId: entityId.toString(),
        content: commentText,
      }).subscribe({
        next: () => {
          this.commentTexts[entityId] = '';
          this.loadComments(entityType, entityId, item);
        },
        error: (err: any) => {
          // --- CHANGED LOGIC: HARD BLOCK ---
          if (err.status === 409 && err.error?.error === 'PROFANITY_WARNING') {
            alert(err.error.message);
          }
          // --- END CHANGED LOGIC ---
          else if (err.status === 401 || err.status === 403) {
            if (confirm('You need to login to comment. Would you like to go to the login page?')) {
              window.location.href = '/login';
            }
          } else {
            alert(err.message || 'Failed to add comment');
          }
        }
      });
    }

  // ... (keep existing code)

  deleteComment(commentId: number, entityType: string, entityId: number, item: any): void {
    if (confirm('Are you sure you want to delete this comment?')) {
      this.http.delete(`/api/comments/${commentId}`).subscribe({
        next: () => {
          this.loadComments(entityType, entityId, item);
        },
        error: (err: any) => {
          alert(err.message || 'Failed to delete comment');
        }
      });
    }
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

  isLoggedIn(): boolean {
    return this.http.isLoggedIn();
  }

  getCurrentUserId(): number | undefined {
    return this.http.getCurrentUser()?.userId;
  }
}
