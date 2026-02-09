import { Component, OnDestroy, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MyHttpClient } from '../my-http-client';
import { Subscription } from 'rxjs';

interface Program {
  id: number;
  name: string;
  description: string;
  startDate: string;
  endDate: string;
  isActive: boolean;
  imageUrl?: string;
  participants?: {
    id: number;
    fullName: string;
    email: string;
  }[];
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
  selector: 'app-programs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './programs.html',
  styleUrl: './programs.css'
})
export class Programs implements OnInit, OnDestroy {
  programs: Program[] = [];
  filteredPrograms: Program[] = [];
  searchQuery: string = '';
  isLoading: boolean = true;
  error: string = '';
  isAdmin: boolean = false;
  private authSub?: Subscription;

  // Admin form
  showCreateForm: boolean = false;
  editingProgram: Program | null = null;
  newProgram = {
    name: '',
    description: '',
    startDate: '',
    endDate: '',
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
      this.programs.forEach(p => this.loadLikes('PROGRAM', p.id, p));
      this.cdr.detectChanges();
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
    this.loadPrograms();
  }

  ngOnDestroy(): void {
    this.authSub?.unsubscribe();
  }

  onSearchChange(): void {
    if (!this.searchQuery.trim()) {
      this.filteredPrograms = this.programs;
      return;
    }
    const query = this.searchQuery.toLowerCase();
    this.filteredPrograms = this.programs.filter(p =>
      p.name.toLowerCase().includes(query) ||
      p.description?.toLowerCase().includes(query)
    );
  }

  loadPrograms(): void {
    this.isLoading = true;
    this.error = '';
    this.http.get('/api/public/programs').subscribe({
      next: (data: unknown) => {
        try {
          const list = Array.isArray(data) ? (data as Program[]) : [];

          // Sort: Programs starting soon come first
          this.programs = list.sort((a, b) => {
            return new Date(a.startDate).getTime() - new Date(b.startDate).getTime();
          });

          this.filteredPrograms = [...this.programs];

          this.programs.forEach((program) => {
            program.likeCount = 0;
            program.isLiked = false;
            this.loadLikes('PROGRAM', program.id, program);
            this.loadComments('PROGRAM', program.id, program);
          });
        } finally {
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        this.error = 'Failed to load programs';
        this.isLoading = false;
        this.programs = [];
        this.filteredPrograms = [];
        this.cdr.detectChanges();
      }
    });
  }

  // --- Helper for Badges ---
  isHappeningNow(program: Program): boolean {
    const now = new Date().getTime();
    const start = new Date(program.startDate).getTime();
    const end = new Date(program.endDate).getTime();
    return now >= start && now <= end;
  }

  isParticipant(program: Program): boolean {
    if (!this.http.isLoggedIn() || !program.participants) return false;
    const user = this.http.getCurrentUser();
    return user !== null && program.participants.some(p => p.id === user.userId);
  }

  joinProgram(programId: number): void {
    if (!this.http.isLoggedIn()) {
      if (confirm('You need to login to join a program. Would you like to go to the login page?')) {
        window.location.href = '/login';
      }
      return;
    }

    this.http.post(`/api/programs/${programId}/join`, {}).subscribe({
      next: () => {
        alert('Successfully joined the program!');
        this.loadPrograms();
      },
      error: (err: any) => {
        if (err.status === 401 || err.status === 403) {
          if (confirm('You need to login to join a program. Would you like to go to the login page?')) {
            window.location.href = '/login';
          }
        } else {
          alert(err.error?.error || err.message || 'Failed to join program');
        }
      }
    });
  }

  leaveProgram(programId: number): void {
    if (!this.http.isLoggedIn()) {
      return;
    }

    if (!confirm('Are you sure you want to leave this program?')) {
      return;
    }

    this.http.post(`/api/programs/${programId}/leave`, {}).subscribe({
      next: () => {
        alert('Successfully left the program!');
        this.loadPrograms();
      },
      error: (err: any) => {
        alert(err.error?.error || err.message || 'Failed to leave program');
      }
    });
  }

  removeParticipant(programId: number, userId: number): void {
    if (!confirm('Are you sure you want to remove this participant?')) {
      return;
    }

    this.http.delete(`/api/admin/programs/${programId}/participants/${userId}`).subscribe({
      next: () => {
        alert('Participant removed successfully!');
        this.loadPrograms();
      },
      error: (err: any) => {
        alert(err.error?.error || err.message || 'Failed to remove participant');
      }
    });
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
        this.newProgram.imageUrl = response.imageUrl;
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
    this.newProgram.imageUrl = '';
  }

  startEdit(program: Program): void {
    this.editingProgram = program;
    this.newProgram = {
      name: program.name,
      description: program.description || '',
      startDate: this.formatDateForInput(program.startDate),
      endDate: this.formatDateForInput(program.endDate),
      imageUrl: program.imageUrl || ''
    };
    this.selectedImageFile = null;
    this.imagePreview = program.imageUrl || null;
    this.showCreateForm = true;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit(): void {
    this.editingProgram = null;
    this.showCreateForm = false;
    this.newProgram = { name: '', description: '', startDate: '', endDate: '', imageUrl: '' };
    this.selectedImageFile = null;
    this.imagePreview = null;
    this.formErrors = {};
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

  saveProgram(): void {
    this.formErrors = {};

    if (!this.newProgram.name) {
      this.formErrors['name'] = 'Program name is required';
    }
    if (!this.newProgram.startDate) {
      this.formErrors['startDate'] = 'Start date is required';
    }
    if (!this.newProgram.endDate) {
      this.formErrors['endDate'] = 'End date is required';
    }

    if (Object.keys(this.formErrors).length > 0) {
      return;
    }

    if (this.selectedImageFile && !this.newProgram.imageUrl) {
      this.uploadImage();
      const checkUpload = setInterval(() => {
        if (!this.isUploadingImage && this.newProgram.imageUrl) {
          clearInterval(checkUpload);
          this.submitProgram();
        } else if (!this.isUploadingImage && !this.newProgram.imageUrl) {
          clearInterval(checkUpload);
          alert('Failed to upload image. Please try again.');
        }
      }, 100);
      return;
    }

    this.submitProgram();
  }

  private submitProgram(): void {
    if (this.editingProgram) {
      this.http.put(`/api/admin/programs/${this.editingProgram.id}`, {
        ...this.newProgram,
        isActive: this.editingProgram.isActive
      }).subscribe({
        next: () => {
          this.cancelEdit();
          this.loadPrograms();
          this.cdr.detectChanges();
          setTimeout(() => {
            alert('Program updated successfully!');
          }, 100);
        },
        error: (err) => {
          alert(err.message || 'Failed to update program');
        }
      });
    } else {
      this.http.post('/api/admin/programs', this.newProgram).subscribe({
        next: () => {
          this.cancelEdit();
          this.loadPrograms();
          this.cdr.detectChanges();
          setTimeout(() => {
            alert('Program created successfully!');
          }, 100);
        },
        error: (err) => {
          alert(err.message || 'Failed to create program');
        }
      });
    }
  }

  deleteProgram(programId: number): void {
    if (confirm('Are you sure you want to delete this program?')) {
      this.http.delete(`/api/admin/programs/${programId}`).subscribe({
        next: () => {
          this.loadPrograms();
          this.cdr.detectChanges();
          setTimeout(() => {
            alert('Program deleted successfully!');
          }, 100);
        },
        error: (err) => {
          alert(err.message || 'Failed to delete program');
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

  toggleComments(programId: number): void {
    if (this.expandedComments.has(programId)) {
      this.expandedComments.delete(programId);
    } else {
      this.expandedComments.add(programId);
    }
  }

  addComment(entityType: string, entityId: number, item: any): void {
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
      content: commentText
    }).subscribe({
      next: () => {
        this.commentTexts[entityId] = '';
        this.loadComments(entityType, entityId, item);
      },
      error: (err: any) => {
        // --- STRICT PROFANITY BLOCK ---
        if (err.status === 409 && err.error?.error === 'PROFANITY_WARNING') {
          alert(err.error.message); // Show message only. No retry logic.
        }
        // -----------------------------
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
