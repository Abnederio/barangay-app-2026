import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MyHttpClient } from '../my-http-client';
import { Subscription } from 'rxjs';

interface Announcement {
  id: number;
  title: string;
  content: string;
  createdAt: string;
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
  selector: 'app-announcements',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './announcements.html',
  styleUrl: './announcements.css'
})
export class Announcements implements OnInit, OnDestroy {
  announcements: Announcement[] = [];
  filteredAnnouncements: Announcement[] = [];
  searchQuery: string = '';
  isLoading: boolean = true;
  error: string = '';
  isAdmin: boolean = false;
  private authSub?: Subscription;

  showCreateForm: boolean = false;
  editingAnnouncement: Announcement | null = null; // Track the announcement being edited

  newAnnouncement = {
    title: '',
    content: '',
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
      this.isAdmin = this.http.isAdmin();
      this.announcements.forEach(a => this.loadLikes('ANNOUNCEMENT', a.id, a));
    });
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
    this.loadAnnouncements();
  }

  ngOnDestroy(): void {
    this.authSub?.unsubscribe();
  }

  onSearchChange(): void {
    if (!this.searchQuery.trim()) {
      this.filteredAnnouncements = this.announcements;
      return;
    }
    const query = this.searchQuery.toLowerCase();
    this.filteredAnnouncements = this.announcements.filter(a =>
      a.title.toLowerCase().includes(query) ||
      a.content.toLowerCase().includes(query)
    );
  }

  loadAnnouncements(): void {
    this.isLoading = true;
    this.http.get('/api/public/announcements').subscribe({
      next: (data: unknown) => {
        try {
          const list = Array.isArray(data) ? (data as Announcement[]) : [];
          const uniqueMap = new Map<number, Announcement>();
          list.forEach(item => {
            if (!uniqueMap.has(item.id)) {
              uniqueMap.set(item.id, item);
            }
          });
          this.announcements = Array.from(uniqueMap.values()).sort((a, b) => {
            const dateA = new Date(a.createdAt).getTime();
            const dateB = new Date(b.createdAt).getTime();
            return dateB - dateA;
          });
          this.filteredAnnouncements = [...this.announcements];
          this.announcements.forEach((announcement) => {
            announcement.likeCount = 0;
            announcement.isLiked = false;
            this.loadLikes('ANNOUNCEMENT', announcement.id, announcement);
            this.loadComments('ANNOUNCEMENT', announcement.id, announcement);
          });
        } finally {
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        this.error = 'Failed to load announcements';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  // --- Edit and Delete Logic ---

  startEdit(announcement: Announcement): void {
    this.editingAnnouncement = announcement;
    this.newAnnouncement = {
      title: announcement.title,
      content: announcement.content,
      imageUrl: announcement.imageUrl || ''
    };
    this.imagePreview = announcement.imageUrl || null;
    this.showCreateForm = true;
    window.scrollTo({ top: 0, behavior: 'smooth' }); // Scroll to form
  }

  cancelEdit(): void {
    this.editingAnnouncement = null;
    this.showCreateForm = false;
    this.newAnnouncement = { title: '', content: '', imageUrl: '' };
    this.selectedImageFile = null;
    this.imagePreview = null;
    this.formErrors = {};
  }

  deleteAnnouncement(id: number): void {
    if (confirm('Are you sure you want to delete this announcement?')) {
      this.http.delete(`/api/admin/announcements/${id}`).subscribe({
        next: () => {
          this.loadAnnouncements();
          alert('Announcement deleted successfully!');
        },
        error: (err) => alert(err.message || 'Failed to delete announcement')
      });
    }
  }

  saveAnnouncement(): void {
    this.formErrors = {};
    if (!this.newAnnouncement.title || this.newAnnouncement.title.trim().length < 3) {
      this.formErrors['title'] = 'Title must be at least 3 characters';
    }

    if (Object.keys(this.formErrors).length > 0) {
      return;
    }

    // Handle image upload if a new file was selected but not yet uploaded
    if (this.selectedImageFile && !this.newAnnouncement.imageUrl) {
      this.uploadImage();
      const checkUpload = setInterval(() => {
        if (!this.isUploadingImage && this.newAnnouncement.imageUrl) {
          clearInterval(checkUpload);
          this.submitAnnouncement();
        } else if (!this.isUploadingImage && !this.newAnnouncement.imageUrl) {
          clearInterval(checkUpload);
          alert('Failed to upload image. Please try again.');
        }
      }, 100);
      return;
    }
    this.submitAnnouncement();
  }

  private submitAnnouncement(): void {
    if (this.isUploadingImage) return;

    if (this.editingAnnouncement) {
      // Update existing announcement
      this.http.put(`/api/admin/announcements/${this.editingAnnouncement.id}`, this.newAnnouncement).subscribe({
        next: () => {
          this.finishForm();
          alert('Announcement updated successfully!');
        },
        error: (err) => alert(err.message || 'Failed to update announcement')
      });
    } else {
      // Create new announcement
      this.http.post('/api/admin/announcements', this.newAnnouncement).subscribe({
        next: () => {
          this.finishForm();
          alert('Announcement created successfully!');
        },
        error: (err) => alert(err.message || 'Failed to create announcement')
      });
    }
  }

  private finishForm(): void {
    this.showCreateForm = false;
    this.editingAnnouncement = null;
    this.newAnnouncement = { title: '', content: '', imageUrl: '' };
    this.selectedImageFile = null;
    this.imagePreview = null;
    this.formErrors = {};
    this.loadAnnouncements();
    this.cdr.detectChanges();
  }

  // --- Existing Logic for Likes, Comments, and Images ---

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

  toggleComments(announcementId: number): void {
    if (this.expandedComments.has(announcementId)) {
      this.expandedComments.delete(announcementId);
    } else {
      this.expandedComments.add(announcementId);
    }
  }

  // ... (keep existing code)

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
       confirmed: confirmed
     }).subscribe({
       next: () => {
         this.commentTexts[entityId] = '';
         this.loadComments(entityType, entityId, item);
       },
       error: (err: any) => {
         // --- CHANGED LOGIC: HARD BLOCK ---
         if (err.status === 409 && err.error?.error === 'PROFANITY_WARNING') {
           // Just alert the user. Do NOT ask to confirm. Do NOT retry.
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
    if (!this.selectedImageFile) return;
    this.isUploadingImage = true;
    this.http.uploadImage(this.selectedImageFile).subscribe({
      next: (response: any) => {
        this.newAnnouncement.imageUrl = response.imageUrl;
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
    this.newAnnouncement.imageUrl = '';
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  isLoggedIn(): boolean {
    return this.http.isLoggedIn();
  }

  getCurrentUserId(): number | undefined {
    return this.http.getCurrentUser()?.userId;
  }
}
