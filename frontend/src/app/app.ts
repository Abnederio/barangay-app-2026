import { Component, OnInit } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MyHttpClient } from './my-http-client';
import { Footer } from './footer/footer';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, Footer],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  isAdmin: boolean = false;

  constructor(
    private http: MyHttpClient,
    private router: Router
  ) {
    // Refresh admin status on route changes
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.isAdmin = this.http.isAdmin();
    });
  }

  ngOnInit(): void {
    this.isAdmin = this.http.isAdmin();
  }

  logout(): void {
    this.http.logout();
    this.isAdmin = false;
    this.router.navigate(['/']).then(() => {
      // Ensure all pages re-load state (likes/check/admin badge) after account switch
      window.location.reload();
    });
  }

  get isLoggedIn(): boolean {
    return this.http.isLoggedIn();
  }
}
