import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { MyHttpClient } from '../my-http-client';
import { Subscription, filter } from 'rxjs';

interface Announcement {
  id: number;
  title: string;
  content: string;
  createdAt: string;
  imageUrl?: string;
}

interface Program {
  id: number;
  name: string;
  description: string;
  startDate: string;
  endDate: string;
  imageUrl?: string;
}

interface Event {
  id: number;
  title: string;
  description: string;
  eventDate: string;
  location: string;
  imageUrl?: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home implements OnInit, OnDestroy {
  latestAnnouncements: Announcement[] = [];
  latestPrograms: Program[] = [];
  latestEvents: Event[] = [];
  isLoading: boolean = true;
  private completedRequests: number = 0;
  private totalRequests: number = 3;
  private routerSub?: Subscription;

  constructor(
    private http: MyHttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadLatestData();

    // Reload data when navigating to home route
    this.routerSub = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      if (event.url === '/' || event.urlAfterRedirects === '/') {
        this.loadLatestData();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.routerSub) {
      this.routerSub.unsubscribe();
    }
  }

  loadLatestData(): void {
    this.isLoading = true;
    this.completedRequests = 0;
    this.cdr.detectChanges();

    // Load latest announcements (limit to 5)
    this.http.get('/api/public/announcements').subscribe({
      next: (data: unknown) => {
        const list = Array.isArray(data) ? (data as Announcement[]) : [];
        this.latestAnnouncements = list.slice(0, 5);
        this.completedRequests++;
        this.checkLoadingComplete();
      },
      error: () => {
        this.latestAnnouncements = [];
        this.completedRequests++;
        this.checkLoadingComplete();
      }
    });

    // Load latest programs (limit to 5)
    this.http.get('/api/public/programs').subscribe({
      next: (data: unknown) => {
        const list = Array.isArray(data) ? (data as Program[]) : [];
        this.latestPrograms = list.slice(0, 5);
        this.completedRequests++;
        this.checkLoadingComplete();
      },
      error: () => {
        this.latestPrograms = [];
        this.completedRequests++;
        this.checkLoadingComplete();
      }
    });

    // Load latest events (limit to 5)
    this.http.get('/api/public/events').subscribe({
      next: (data: unknown) => {
        const list = Array.isArray(data) ? (data as Event[]) : [];
        this.latestEvents = list.slice(0, 5);
        this.completedRequests++;
        this.checkLoadingComplete();
      },
      error: () => {
        this.latestEvents = [];
        this.completedRequests++;
        this.checkLoadingComplete();
      }
    });
  }

  private checkLoadingComplete(): void {
    // Check if all three requests have completed
    if (this.completedRequests >= this.totalRequests) {
      this.isLoading = false;
      this.cdr.detectChanges();
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  truncateText(text: string, maxLength: number = 150): string {
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  }
}
