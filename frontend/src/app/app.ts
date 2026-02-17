import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MyHttpClient } from './my-http-client';
import { Footer } from './footer/footer';
import { filter } from 'rxjs/operators';
import { IdleService } from './idle.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, Footer],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  isAdmin: boolean = false;
  showIdleWarning: boolean = false;
  idleCountdown: number = 0;

  constructor(
    private http: MyHttpClient,
    public router: Router,
    private cdr: ChangeDetectorRef,
    public idleService: IdleService
  ) {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.isAdmin = this.http.isAdmin();
      this.idleService.startWatching();
    });

    // Subscribe to idle status updates and force change detection
    this.idleService.showCountdownSource.subscribe(show => {
      this.showIdleWarning = show;
      this.cdr.detectChanges();
    });

    this.idleService.countdownValueSource.subscribe(val => {
      this.idleCountdown = val;
      this.cdr.detectChanges();
    });
  }

  ngOnInit(): void {
    this.isAdmin = this.http.isAdmin();
    this.idleService.startWatching();
  }

  logout(): void {
    this.idleService.stopWatching();
    this.http.logout();
    this.isAdmin = false;
    this.router.navigate(['/']).then(() => {
      window.location.reload();
    });
  }

  get isLoggedIn(): boolean {
    return this.http.isLoggedIn();
  }
}
