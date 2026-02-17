import { Component, OnInit } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MyHttpClient } from './my-http-client';
import { Footer } from './footer/footer';
import { filter } from 'rxjs/operators';
import { IdleService } from './idle.service'; // Import added

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, Footer],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  isAdmin: boolean = false;
  showIdleWarning: boolean = false; // New property
  idleCountdown: number = 0;        // New property

  constructor(
    private http: MyHttpClient,
    private router: Router,
    public idleService: IdleService // Injected IdleService
  ) {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.isAdmin = this.http.isAdmin();
      this.idleService.startWatching(); // Restart timer on navigation
    });

    // Listen for idle warnings
    this.idleService.showCountdownSource.subscribe(show => this.showIdleWarning = show);
    this.idleService.countdownValueSource.subscribe(val => this.idleCountdown = val);
  }

  ngOnInit(): void {
    this.isAdmin = this.http.isAdmin();
    this.idleService.startWatching(); // Start watching on load
  }

  logout(): void {
    this.idleService.stopWatching(); // Stop timers on logout
    this.http.logout(); //
    this.isAdmin = false; //
    this.router.navigate(['/']).then(() => { //
      window.location.reload(); //
    });
  }

  get isLoggedIn(): boolean {
    return this.http.isLoggedIn(); //
  }
}
