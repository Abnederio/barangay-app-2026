import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, NavigationEnd } from '@angular/router';
import { MyHttpClient } from '../my-http-client';
import { Subscription, filter } from 'rxjs';

interface Service {
  id: number;
  name: string;
  description: string;
  imageUrl?: string;
  isActive: boolean;
  participants?: {
    id: number;
    fullName: string;
    email: string;
  }[];
}

interface ServiceApplication {
  id: number;
  serviceType: string;
  additionalInfo: string;
  status: string;
  submittedAt: string;
  user?: {
    fullName: string;
    email: string;
  };
}

@Component({
  selector: 'app-services',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './services.html',
  styleUrl: './services.css'
})
export class Services implements OnInit, OnDestroy {
  availableServices: Service[] = [];
  filteredServices: Service[] = [];
  searchQuery: string = '';
  myApplications: ServiceApplication[] = [];
  allApplications: ServiceApplication[] = [];
  isLoading: boolean = true;
  error: string = '';
  isAdmin: boolean = false;
  
  showApplicationForm: boolean = false;
  showServiceForm: boolean = false;
  editingService: Service | null = null;
  selectedService: string = '';
  additionalInfo: string = '';
  newService = {
    name: '',
    description: '',
    imageUrl: ''
  };
  selectedImageFile: File | null = null;
  imagePreview: string | null = null;
  isUploadingImage: boolean = false;
  formErrors: { [key: string]: string } = {};
  successMessage: string = '';
  private routerSub?: Subscription;

  constructor(
    private http: MyHttpClient,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadData();
    
    // Reload data when navigating to this route
    this.routerSub = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      // Only reload if we're on the services route
      if (this.router.url.includes('/services') || this.router.url === '/services') {
        this.loadData();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.routerSub) {
      this.routerSub.unsubscribe();
    }
  }

  private loadData(): void {
    this.isAdmin = this.http.isAdmin();
    this.loadServices();
    
    // Always try to load applications immediately if logged in
    if (this.http.isLoggedIn()) {
      if (this.isAdmin) {
        this.loadAllApplications();
      } else {
        this.loadMyApplications();
      }
    }
    
    // Refresh admin status from backend to ensure accuracy and reload data
    if (this.http.isLoggedIn()) {
      this.http.refreshUserProfile().subscribe({
        next: (profile: any) => {
          this.http.updateUserProfile(profile);
          this.isAdmin = profile.isAdmin;
          // Always reload applications when admin status is confirmed
          if (this.isAdmin) {
            this.loadAllApplications();
          } else {
            // If user is logged in but not admin, load their applications
            this.loadMyApplications();
          }
          this.cdr.detectChanges();
        },
        error: () => {
          this.isAdmin = this.http.isAdmin();
          // Still try to load applications even if profile refresh fails
          if (this.isAdmin) {
            this.loadAllApplications();
          } else if (this.http.isLoggedIn()) {
            this.loadMyApplications();
          }
          this.cdr.detectChanges();
        }
      });
    }
  }

  onSearchChange(): void {
    if (!this.searchQuery.trim()) {
      this.filteredServices = this.availableServices;
      return;
    }
    const query = this.searchQuery.toLowerCase();
    this.filteredServices = this.availableServices.filter(s =>
      s.name.toLowerCase().includes(query) ||
      s.description?.toLowerCase().includes(query)
    );
  }

  loadServices(): void {
    this.isLoading = true;
    this.error = '';
    this.http.get('/api/public/services').subscribe({
      next: (data: Service[]) => {
        this.availableServices = data || [];
        this.filteredServices = this.availableServices;
        this.isLoading = false;
        this.cdr.detectChanges(); // Force change detection
      },
      error: (err) => {
        this.error = 'Failed to load services';
        this.isLoading = false;
        this.availableServices = [];
        this.filteredServices = [];
        this.cdr.detectChanges();
      }
    });
  }

  loadMyApplications(): void {
    if (!this.http.isLoggedIn()) {
      this.myApplications = [];
      return;
    }
    this.http.get('/api/services/my-applications').subscribe({
      next: (data: ServiceApplication[]) => {
        this.myApplications = Array.isArray(data) ? data : [];
        this.cdr.detectChanges();
      },
      error: (err) => {
        // Only ignore 401/403 errors (not logged in or unauthorized)
        if (err.status !== 401 && err.status !== 403) {
          console.error('Failed to load my applications', err);
        }
        this.myApplications = [];
        this.cdr.detectChanges();
      }
    });
  }

  openApplicationForm(service: Service | string): void {
    // Check if user is logged in
    if (!this.http.isLoggedIn()) {
      if (confirm('You need to login to apply for services. Would you like to go to the login page?')) {
        window.location.href = '/login';
      }
      return;
    }

    if (typeof service === 'string') {
      this.selectedService = service;
    } else {
      this.selectedService = service.name;
    }
    this.showApplicationForm = true;
    this.additionalInfo = '';
    this.formErrors = {};
    this.successMessage = '';
  }

  startEditService(service: Service): void {
    this.editingService = service;
    this.newService = {
      name: service.name,
      description: service.description || '',
      imageUrl: service.imageUrl || ''
    };
    this.selectedImageFile = null;
    this.imagePreview = service.imageUrl || null;
    this.showServiceForm = true;
  }

  cancelServiceEdit(): void {
    this.editingService = null;
    this.showServiceForm = false;
    this.newService = { name: '', description: '', imageUrl: '' };
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
        this.newService.imageUrl = response.imageUrl;
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
    this.newService.imageUrl = '';
  }

  saveService(): void {
    this.formErrors = {};
    
    if (!this.newService.name || this.newService.name.trim().length < 3) {
      this.formErrors['name'] = 'Service name must be at least 3 characters';
      return;
    }

    // If image is selected but not uploaded yet, upload it first
    if (this.selectedImageFile && !this.newService.imageUrl) {
      this.uploadImage();
      const checkUpload = setInterval(() => {
        if (!this.isUploadingImage && this.newService.imageUrl) {
          clearInterval(checkUpload);
          this.submitService();
        } else if (!this.isUploadingImage && !this.newService.imageUrl) {
          clearInterval(checkUpload);
          alert('Failed to upload image. Please try again.');
        }
      }, 100);
      return;
    }

    this.submitService();
  }

  private submitService(): void {
    if (this.editingService) {
      this.http.put(`/api/admin/services/${this.editingService.id}`, {
        ...this.newService,
        isActive: this.editingService.isActive
      }).subscribe({
        next: () => {
          this.cancelServiceEdit();
          this.loadServices();
          this.cdr.detectChanges();
          setTimeout(() => {
            alert('Service updated successfully!');
          }, 100);
        },
        error: (err) => {
          alert(err.message || 'Failed to update service');
        }
      });
    } else {
      this.http.post('/api/admin/services', this.newService).subscribe({
        next: () => {
          this.cancelServiceEdit();
          this.loadServices();
          this.cdr.detectChanges();
          setTimeout(() => {
            alert('Service created successfully!');
          }, 100);
        },
        error: (err) => {
          alert(err.message || 'Failed to create service');
        }
      });
    }
  }

  deleteService(serviceId: number): void {
    if (confirm('Are you sure you want to delete this service?')) {
      this.http.delete(`/api/admin/services/${serviceId}`).subscribe({
        next: () => {
          this.loadServices();
          this.cdr.detectChanges(); // Force change detection
          setTimeout(() => {
            alert('Service deleted successfully!');
          }, 100);
        },
        error: (err) => {
          alert(err.message || 'Failed to delete service');
        }
      });
    }
  }

  isParticipant(service: Service): boolean {
    if (!this.http.isLoggedIn() || !service.participants) return false;
    const user = this.http.getCurrentUser();
    return user !== null && service.participants.some(p => p.id === user.userId);
  }

  joinService(serviceId: number): void {
    // Check if user is logged in
    if (!this.http.isLoggedIn()) {
      if (confirm('You need to login to join a service. Would you like to go to the login page?')) {
        window.location.href = '/login';
      }
      return;
    }

    this.http.post(`/api/services/${serviceId}/join`, {}).subscribe({
      next: () => {
        alert('Successfully joined the service!');
        this.loadServices();
      },
      error: (err: any) => {
        if (err.status === 401 || err.status === 403) {
          if (confirm('You need to login to join a service. Would you like to go to the login page?')) {
            window.location.href = '/login';
          }
        } else {
          alert(err.error?.error || err.message || 'Failed to join service');
        }
      }
    });
  }

  leaveService(serviceId: number): void {
    if (!this.http.isLoggedIn()) {
      return;
    }

    if (!confirm('Are you sure you want to leave this service?')) {
      return;
    }

    this.http.post(`/api/services/${serviceId}/leave`, {}).subscribe({
      next: () => {
        alert('Successfully left the service!');
        this.loadServices();
      },
      error: (err: any) => {
        alert(err.error?.error || err.message || 'Failed to leave service');
      }
    });
  }

  removeParticipant(serviceId: number, userId: number): void {
    if (!confirm('Are you sure you want to remove this participant?')) {
      return;
    }

    this.http.delete(`/api/admin/services/${serviceId}/participants/${userId}`).subscribe({
      next: () => {
        alert('Participant removed successfully!');
        this.loadServices();
      },
      error: (err: any) => {
        alert(err.error?.error || err.message || 'Failed to remove participant');
      }
    });
  }

  submitApplication(): void {
    // Check if user is logged in
    if (!this.http.isLoggedIn()) {
      if (confirm('You need to login to apply for services. Would you like to go to the login page?')) {
        window.location.href = '/login';
      }
      return;
    }

    this.formErrors = {};
    
    if (!this.selectedService) {
      this.formErrors['serviceType'] = 'Please select a service';
      return;
    }

    this.http.post('/api/services/apply', {
      serviceType: this.selectedService,
      additionalInfo: this.additionalInfo
    }).subscribe({
      next: (response: any) => {
        this.successMessage = response.message || 'Application submitted successfully! You will be notified via email.';
        this.showApplicationForm = false;
        this.loadMyApplications();
      },
      error: (err: any) => {
        if (err.status === 401 || err.status === 403) {
          if (confirm('You need to login to apply for services. Would you like to go to the login page?')) {
            window.location.href = '/login';
          }
        } else {
          this.formErrors['general'] = err.message || 'Failed to submit application';
        }
      }
    });
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  loadAllApplications(): void {
    if (!this.http.isLoggedIn()) {
      this.allApplications = [];
      return;
    }
    this.http.get('/api/admin/services/applications').subscribe({
      next: (data: ServiceApplication[]) => {
        this.allApplications = Array.isArray(data) ? data : [];
        this.cdr.detectChanges();
      },
      error: (err) => {
        // Only log non-auth errors
        if (err.status !== 401 && err.status !== 403) {
          console.error('Failed to load all applications', err);
        }
        this.allApplications = [];
        this.cdr.detectChanges();
      }
    });
  }

  updateApplicationStatus(applicationId: number, status: string): void {
    this.http.put(`/api/admin/services/applications/${applicationId}/status`, { status }).subscribe({
      next: () => {
        alert('Application status updated!');
        this.loadAllApplications();
      },
      error: (err) => {
        alert(err.message || 'Failed to update status');
      }
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'APPROVED': return 'status-approved';
      case 'REJECTED': return 'status-rejected';
      default: return 'status-pending';
    }
  }
}
