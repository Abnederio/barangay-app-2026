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
  serviceStatus: string; // UPCOMING, ONGOING, FINISHED, CANCELLED
  participants?: { id: number; fullName: string; email: string; }[];
}

interface ServiceApplication {
  id: number;
  serviceType: string;
  additionalInfo: string;
  status: string;
  submittedAt: string;
  user?: { fullName: string; email: string; };
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
  newService = { name: '', description: '', imageUrl: '', serviceStatus: 'ONGOING' };
  selectedImageFile: File | null = null;
  imagePreview: string | null = null;
  isUploadingImage: boolean = false;
  formErrors: { [key: string]: string } = {};
  successMessage: string = '';
  private routerSub?: Subscription;

  constructor(private http: MyHttpClient, private cdr: ChangeDetectorRef, private router: Router) {}

  ngOnInit(): void {
    this.loadData();
    this.routerSub = this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(() => {
      if (this.router.url.includes('/services') || this.router.url === '/services') {
        this.loadData();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.routerSub) this.routerSub.unsubscribe();
  }

  private loadData(): void {
    this.isAdmin = this.http.isAdmin();
    this.loadServices();
    if (this.http.isLoggedIn()) {
      if (this.isAdmin) this.loadAllApplications();
      else this.loadMyApplications();
    }
  }

  onSearchChange(): void {
    if (!this.searchQuery.trim()) {
      this.filteredServices = this.availableServices;
      return;
    }
    const query = this.searchQuery.toLowerCase();
    this.filteredServices = this.availableServices.filter(s =>
      s.name.toLowerCase().includes(query) || s.description?.toLowerCase().includes(query)
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
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = 'Failed to load services';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadMyApplications(): void {
    if (!this.http.isLoggedIn()) return;
    this.http.get('/api/services/my-applications').subscribe({
      next: (data: ServiceApplication[]) => {
        this.myApplications = Array.isArray(data) ? data : [];
        this.cdr.detectChanges();
      },
      error: () => { this.myApplications = []; this.cdr.detectChanges(); }
    });
  }

  loadAllApplications(): void {
    if (!this.http.isLoggedIn()) return;
    this.http.get('/api/admin/services/applications').subscribe({
      next: (data: ServiceApplication[]) => {
        this.allApplications = Array.isArray(data) ? data : [];
        this.cdr.detectChanges();
      },
      error: () => { this.allApplications = []; this.cdr.detectChanges(); }
    });
  }

  openApplicationForm(service: Service | string): void {
    if (!this.http.isLoggedIn()) {
      if (confirm('You need to login to apply for services. Go to login page?')) window.location.href = '/login';
      return;
    }
    this.selectedService = typeof service === 'string' ? service : service.name;
    this.showApplicationForm = true;
    this.additionalInfo = '';
    this.formErrors = {};
    this.successMessage = '';
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  submitApplication(): void {
    if (!this.http.isLoggedIn()) return;
    this.formErrors = {};
    if (!this.selectedService) {
      this.formErrors['serviceType'] = 'Please select a service';
      return;
    }

    this.http.post('/api/services/apply', { serviceType: this.selectedService, additionalInfo: this.additionalInfo }).subscribe({
      next: (response: any) => {
        this.successMessage = response.message;
        this.showApplicationForm = false;
        this.loadMyApplications();
        alert('Application submitted successfully! Track it in the My Applications tab.');
      },
      error: (err: any) => { this.formErrors['general'] = err.message || 'Failed to submit application'; }
    });
  }

  updateApplicationStatus(applicationId: number, status: string): void {
    this.http.put(`/api/admin/services/applications/${applicationId}/status`, { status }).subscribe({
      next: () => {
        alert(`Application marked as ${status}!`);
        this.loadAllApplications();
        this.loadServices(); // Reload services to show new participant
      },
      error: (err) => alert(err.message || 'Failed to update status')
    });
  }

  startEditService(service: Service): void {
    this.editingService = service;
    this.newService = {
      name: service.name,
      description: service.description || '',
      imageUrl: service.imageUrl || '',
      serviceStatus: service.serviceStatus || 'ONGOING'
    };
    this.selectedImageFile = null;
    this.imagePreview = service.imageUrl || null;
    this.showServiceForm = true;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelServiceEdit(): void {
    this.editingService = null;
    this.showServiceForm = false;
    this.newService = { name: '', description: '', imageUrl: '', serviceStatus: 'ONGOING' };
    this.selectedImageFile = null;
    this.imagePreview = null;
    this.formErrors = {};
  }

  onImageSelected(event: any): void {
    const file = event.target.files?.[0];
    if (file) {
      this.selectedImageFile = file;
      const reader = new FileReader();
      reader.onload = (e: any) => { this.imagePreview = e.target.result; };
      reader.readAsDataURL(file);
    }
  }

  uploadImage(): void {
    if (!this.selectedImageFile) return;
    this.isUploadingImage = true;
    this.http.uploadImage(this.selectedImageFile).subscribe({
      next: (response: any) => {
        this.newService.imageUrl = response.imageUrl;
        this.isUploadingImage = false;
      },
      error: (err) => { alert('Upload failed'); this.isUploadingImage = false; }
    });
  }

  removeImage(): void {
    this.selectedImageFile = null;
    this.imagePreview = null;
    this.newService.imageUrl = '';
  }

  saveService(): void {
    this.formErrors = {};
    if (!this.newService.name) { this.formErrors['name'] = 'Service name is required'; return; }
    if (this.selectedImageFile && !this.newService.imageUrl) {
      this.uploadImage();
      const checkUpload = setInterval(() => {
        if (!this.isUploadingImage && this.newService.imageUrl) {
          clearInterval(checkUpload);
          this.submitService();
        }
      }, 100);
      return;
    }
    this.submitService();
  }

  private submitService(): void {
    if (this.editingService) {
      this.http.put(`/api/admin/services/${this.editingService.id}`, this.newService).subscribe({
        next: () => { this.cancelServiceEdit(); this.loadServices(); alert('Service updated!'); },
        error: (err) => alert(err.message || 'Failed to update')
      });
    } else {
      this.http.post('/api/admin/services', this.newService).subscribe({
        next: () => { this.cancelServiceEdit(); this.loadServices(); alert('Service created!'); },
        error: (err) => alert(err.message || 'Failed to create')
      });
    }
  }

  deleteService(serviceId: number): void {
    if (confirm('Are you sure?')) {
      this.http.delete(`/api/admin/services/${serviceId}`).subscribe({
        next: () => { this.loadServices(); alert('Service deleted!'); },
        error: (err) => alert('Failed to delete')
      });
    }
  }

  hasApplied(service: Service): boolean {
    // Check if the user has a pending or approved application for this service
    return this.myApplications.some(app => app.serviceType === service.name && (app.status === 'PENDING' || app.status === 'APPROVED'));
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'APPROVED': return 'status-approved';
      case 'REJECTED': return 'status-rejected';
      case 'PENDING': return 'status-pending';
      case 'ONGOING': return 'badge-ongoing';
      case 'FINISHED': return 'badge-finished';
      case 'CANCELLED': return 'badge-cancelled';
      case 'UPCOMING': return 'badge-upcoming';
      default: return '';
    }
  }
}
