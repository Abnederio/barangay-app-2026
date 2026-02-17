import { Injectable, NgZone } from '@angular/core';
import { Router } from '@angular/router';
import { MyHttpClient } from './my-http-client';
import { BehaviorSubject, Subscription, timer, fromEvent, merge } from 'rxjs';
import { switchMap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class IdleService {
  private idleTimerSubscription?: Subscription;
  private countdownSubscription?: Subscription;

  public showCountdownSource = new BehaviorSubject<boolean>(false);
  public countdownValueSource = new BehaviorSubject<number>(10);

  constructor(
    private http: MyHttpClient,
    private router: Router,
    private ngZone: NgZone
  ) {}

  startWatching() {
    this.stopWatching();
    if (!this.http.isLoggedIn()) return;

    // 2 minutes for admin (120s), 5 minutes for normal users (300s)
    const isAdmin = this.http.isAdmin();
    const idleTime = isAdmin ? 20 : 20;
    const warningTime = 10;
    const idleSeconds = idleTime - warningTime;

    const activity$ = merge(
      fromEvent(window, 'mousemove'),
      fromEvent(window, 'keydown'),
      fromEvent(window, 'click'),
      fromEvent(window, 'scroll')
    );

    this.ngZone.runOutsideAngular(() => {
      this.idleTimerSubscription = activity$.pipe(
        switchMap(() => timer(idleSeconds * 1000))
      ).subscribe(() => {
        this.ngZone.run(() => this.startCountdown(warningTime));
      });
    });
  }

  private startCountdown(seconds: number) {
    this.showCountdownSource.next(true);
    this.countdownValueSource.next(seconds);

    this.countdownSubscription = timer(0, 1000).subscribe(val => {
      const remaining = seconds - val;
      if (remaining <= 0) {
        this.logoutUser();
      } else {
        this.countdownValueSource.next(remaining);
      }
    });
  }

  private logoutUser() {
    this.stopWatching();
    this.showCountdownSource.next(false);
    this.http.logout();
    this.router.navigate(['/login']);
    window.location.reload();
  }

  stopWatching() {
    this.idleTimerSubscription?.unsubscribe();
    this.countdownSubscription?.unsubscribe();
    this.showCountdownSource.next(false);
  }
}
