import { Routes } from '@angular/router';
import { Home } from './home/home';
import { Programs } from './programs/programs';
import { Events } from './events/events';
import { Feedback } from './feedback/feedback';
import { Officials } from './officials/officials';
import { Services } from './services/services';
import { LoginForm } from './login-form/login-form';
import { SignupForm } from './signup-form/signup-form';
import { ForgotPassword } from './forgot-password/forgot-password';
import { Announcements } from './announcements/announcements';

export const routes: Routes = [
  // This is the correct fix for the persistent "Home" highlight
  { path: '', component: Home, pathMatch: 'full' },
  { path: 'login', component: LoginForm },
  { path: 'signup', component: SignupForm },
  { path: 'forgot-password', component: ForgotPassword },
  { path: 'announcements', component: Announcements },
  { path: 'programs', component: Programs },
  { path: 'events', component: Events },
  { path: 'feedback', component: Feedback },
  { path: 'officials', component: Officials },
  { path: 'services', component: Services },
  { path: '**', redirectTo: '' }
];
