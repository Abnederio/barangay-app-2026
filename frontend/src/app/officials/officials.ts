import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MyHttpClient } from '../my-http-client';

interface Official {
  id: number;
  name: string;
  position: string;
  email: string;
  phoneNumber: string;
  pictureUrl: string;
}

@Component({
  selector: 'app-officials',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './officials.html',
  styleUrl: './officials.css'
})
export class Officials implements OnInit {
  officials: Official[] = [];
  filteredOfficials: Official[] = [];
  searchQuery: string = '';
  isLoading: boolean = true;
  error: string = '';
  isAdmin: boolean = false;
  
  showCreateForm: boolean = false;
  editingOfficial: Official | null = null;
  formData = {
    name: '',
    position: '',
    email: '',
    phoneNumber: '',
    pictureUrl: ''
  };
  selectedImageFile: File | null = null;
  imagePreview: string | null = null;
  isUploadingImage: boolean = false;
  formErrors: { [key: string]: string } = {};

  constructor(
    private http: MyHttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.http.isAdmin();
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
    this.loadOfficials();
  }

  onSearchChange(): void {
    if (!this.searchQuery.trim()) {
      this.filteredOfficials = this.officials;
      return;
    }
    const query = this.searchQuery.toLowerCase();
    this.filteredOfficials = this.officials.filter(o =>
      o.name.toLowerCase().includes(query) ||
      o.position.toLowerCase().includes(query) ||
      o.email?.toLowerCase().includes(query)
    );
  }

  loadOfficials(): void {
    this.isLoading = true;
    this.error = '';
    this.http.get('/api/public/officials').subscribe({
      next: (data: Official[]) => {
        this.officials = data || [];
        this.filteredOfficials = this.officials;
        this.isLoading = false;
        this.cdr.detectChanges(); // Force change detection
      },
      error: (err) => {
        this.error = 'Failed to load officials';
        this.isLoading = false;
        this.officials = [];
        this.filteredOfficials = [];
        this.cdr.detectChanges();
      }
    });
  }

  startEdit(official: Official): void {
    this.editingOfficial = official;
    this.formData = {
      name: official.name,
      position: official.position,
      email: official.email || '',
      phoneNumber: official.phoneNumber || '',
      pictureUrl: official.pictureUrl || ''
    };
    this.selectedImageFile = null;
    this.imagePreview = official.pictureUrl || null;
    this.showCreateForm = true;
  }

  cancelEdit(): void {
    this.editingOfficial = null;
    this.showCreateForm = false;
    this.formData = { name: '', position: '', email: '', phoneNumber: '', pictureUrl: '' };
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
        this.formData.pictureUrl = response.imageUrl;
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
    this.formData.pictureUrl = '';
  }

  saveOfficial(): void {
    this.formErrors = {};
    
    if (!this.formData.name) {
      this.formErrors['name'] = 'Name is required';
    }
    if (!this.formData.position) {
      this.formErrors['position'] = 'Position is required';
    }
    
    if (Object.keys(this.formErrors).length > 0) {
      return;
    }

    // If image is selected but not uploaded yet, upload it first
    if (this.selectedImageFile && !this.formData.pictureUrl) {
      this.uploadImage();
      const checkUpload = setInterval(() => {
        if (!this.isUploadingImage && this.formData.pictureUrl) {
          clearInterval(checkUpload);
          this.submitOfficial();
        } else if (!this.isUploadingImage && !this.formData.pictureUrl) {
          clearInterval(checkUpload);
          alert('Failed to upload image. Please try again.');
        }
      }, 100);
      return;
    }

    this.submitOfficial();
  }

  private submitOfficial(): void {
    if (this.editingOfficial) {
      this.http.put(`/api/admin/officials/${this.editingOfficial.id}`, this.formData).subscribe({
        next: () => {
          this.cancelEdit();
          this.loadOfficials();
          this.cdr.detectChanges();
          setTimeout(() => {
            alert('Official updated successfully!');
          }, 100);
        },
        error: (err) => {
          alert(err.message || 'Failed to update official');
        }
      });
    } else {
      this.http.post('/api/admin/officials', this.formData).subscribe({
        next: () => {
          this.cancelEdit();
          this.loadOfficials();
          this.cdr.detectChanges();
          setTimeout(() => {
            alert('Official created successfully!');
          }, 100);
        },
        error: (err) => {
          alert(err.message || 'Failed to create official');
        }
      });
    }
  }
}
